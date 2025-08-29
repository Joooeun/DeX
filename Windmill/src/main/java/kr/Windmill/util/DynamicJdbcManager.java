package kr.Windmill.util;

import java.io.Closeable;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * DeX 프로젝트용 동적 JDBC 드라이버 관리 시스템
 * 
 * 기능: 1. 서비스 시작 시 DATABASE_CONNECTION 테이블에서 정보를 조회하여 커넥션 풀 초기화 2. SQL 실행 시 커넥션
 * 풀에서 재사용 3. 연결 테스트 시 일회성 커넥션 생성 4. 멀티스레드 안전한 커넥션 풀 관리
 * 
 * @author DeX Team
 * @version 1.0
 */
@Component
public class DynamicJdbcManager implements Closeable {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DynamicJdbcManager.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Common common;

	/* ----------------------------- DriverShim ----------------------------- */
	public static final class DriverShim implements Driver {
		private final Driver delegate;

		public DriverShim(Driver delegate) {
			this.delegate = delegate;
		}

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			return delegate.connect(url, info);
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return delegate.acceptsURL(url);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return delegate.getPropertyInfo(url, info);
		}

		@Override
		public int getMajorVersion() {
			return delegate.getMajorVersion();
		}

		@Override
		public int getMinorVersion() {
			return delegate.getMinorVersion();
		}

		@Override
		public boolean jdbcCompliant() {
			return delegate.jdbcCompliant();
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			try {
				return delegate.getParentLogger();
			} catch (SQLFeatureNotSupportedException e) {
				throw e;
			}
		}
	}

	/* ----------------------- Isolated Driver Registry ---------------------- */
	public static final class IsolatedDriver implements Closeable {
		final URLClassLoader cl;
		final DriverShim shim;

		private IsolatedDriver(URLClassLoader cl, DriverShim shim) {
			this.cl = cl;
			this.shim = shim;
		}

		public static IsolatedDriver load(Path driverJar, String driverClassName) throws Exception {
			URL jarUrl = driverJar.toUri().toURL();
			// Java 17에서는 system classloader를 parent로 사용해야 java.sql.Driver에 접근 가능
			URLClassLoader cl = new URLClassLoader(new URL[] { jarUrl }, ClassLoader.getSystemClassLoader());
			Class<?> drvClazz = Class.forName(driverClassName, true, cl);
			Driver realDriver = (Driver) drvClazz.getDeclaredConstructor().newInstance();
			DriverShim shim = new DriverShim(realDriver);
			DriverManager.registerDriver(shim);
			logger.info("드라이버 로드 및 등록 완료: {} from {}", driverClassName, driverJar);
			return new IsolatedDriver(cl, shim);
		}

		@Override
		public void close() {
			try {
				DriverManager.deregisterDriver(shim);
				logger.info("드라이버 등록 해제 완료");
			} catch (SQLException ignore) {
			}
			try {
				cl.close();
				logger.info("ClassLoader 정리 완료");
			} catch (Exception ignore) {
			}
		}

		Connection connect(String url, Properties props) throws SQLException {
			// 일부 드라이버는 Thread Context ClassLoader에 의존하므로 잠시 교체
			Thread t = Thread.currentThread();
			ClassLoader prev = t.getContextClassLoader();
			t.setContextClassLoader(cl);
			try {
				return DriverManager.getConnection(url, props);
			} finally {
				t.setContextClassLoader(prev);
			}
		}
	}

	/* ------------------------------ Pool Key ------------------------------ */
	public static final class PoolKey {
		final String connectionId;
		final String jarPath;
		final String driverClass;
		final String url;
		final String propsFingerprint;

		PoolKey(String connectionId, Path jar, String driverClass, String url, Properties props) {
			this.connectionId = connectionId;
			this.jarPath = jar.toAbsolutePath().toString();
			this.driverClass = driverClass;
			this.url = url;
			this.propsFingerprint = fingerprint(props);
		}

		private static String fingerprint(Properties p) {
			List<String> keys = new ArrayList<>();
			for (Object k : p.keySet())
				keys.add(String.valueOf(k));
			Collections.sort(keys);
			StringBuilder sb = new StringBuilder();
			for (String k : keys) {
				sb.append(k).append('=').append(p.getProperty(k)).append(';');
			}
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof PoolKey))
				return false;
			PoolKey that = (PoolKey) o;
			return connectionId.equals(that.connectionId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(connectionId);
		}

		@Override
		public String toString() {
			return "PoolKey{" + connectionId + " | " + driverClass + "@" + jarPath + "}";
		}
	}

	/* --------------------------- Pooled Connection ------------------------- */
	public interface RealConnectionCloser {
		void reallyClose(Connection physical) throws SQLException;
	}

	public static Connection wrapPooled(Connection physical, BlockingQueue<Connection> poolQueue, RealConnectionCloser closer, long maxLifetimeMillis, long createdAtMillis) {
		InvocationHandler h = (proxy, method, args) -> {
			String name = method.getName();
			if ("close".equals(name)) {
				// 수명 만료되었으면 진짜 닫고 새로 쓰게 함
				if (maxLifetimeMillis > 0 && (System.currentTimeMillis() - createdAtMillis) > maxLifetimeMillis) {
					closer.reallyClose(physical);
					return null;
				}
				// 트랜잭션/오토커밋 초기화 (안전)
				try {
					if (!physical.getAutoCommit()) {
						physical.rollback();
						physical.setAutoCommit(true);
					}
				} catch (SQLException ignore) {
				}
				// 풀 반납
				poolQueue.offer(wrapPooled(physical, poolQueue, closer, maxLifetimeMillis, createdAtMillis));
				return null;
			}
			if ("unwrap".equals(name))
				return physical.unwrap((Class<?>) args[0]);
			if ("isWrapperFor".equals(name))
				return physical.isWrapperFor((Class<?>) args[0]);
			if ("toString".equals(name))
				return "PooledConnection(" + physical + ")";
			if ("hashCode".equals(name))
				return System.identityHashCode(proxy);
			if ("equals".equals(name))
				return proxy == args[0];
			return method.invoke(physical, args);
		};
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, h);
	}

	/* ------------------------------ Pool Impl ------------------------------ */
	public static final class IsolatedPool implements Closeable {
		private final PoolKey key;
		private final IsolatedDriver isoDriver;
		private final BlockingQueue<Connection> idle;
		private final AtomicInteger created = new AtomicInteger(0);

		private final int maxPoolSize;
		private final long connectionTimeoutMs;
		private final long maxLifetimeMs;
		private final long idleTimeoutMs;

		private final ScheduledExecutorService evictor;

		public IsolatedPool(PoolKey key, IsolatedDriver isoDriver, int maxPoolSize, long connectionTimeoutMs, long maxLifetimeMs, long idleTimeoutMs) {
			this.key = key;
			this.isoDriver = isoDriver;
			this.maxPoolSize = Math.max(1, maxPoolSize);
			this.connectionTimeoutMs = Math.max(1_000, connectionTimeoutMs);
			this.maxLifetimeMs = maxLifetimeMs;
			this.idleTimeoutMs = idleTimeoutMs;
			this.idle = new LinkedBlockingQueue<>();

			// 간단한 idle evictor
			this.evictor = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "PoolEvictor-" + key.connectionId);
				t.setDaemon(true);
				return t;
			});
			if (idleTimeoutMs > 0) {
				this.evictor.scheduleAtFixedRate(this::evictIdle, idleTimeoutMs, idleTimeoutMs, TimeUnit.MILLISECONDS);
			}

			logger.info("커넥션 풀 생성 완료: {} (maxSize={}, timeout={}ms, maxLifetime={}ms, idleTimeout={}ms)", key, maxPoolSize, connectionTimeoutMs, maxLifetimeMs, idleTimeoutMs);
		}

		private void evictIdle() {
			List<Connection> drained = new ArrayList<>();
			idle.drainTo(drained);
			for (Connection c : drained) {
				try {
					unwrapPhysical(c).close();
					created.decrementAndGet();
				} catch (Exception ignore) {
				}
			}
			if (!drained.isEmpty()) {
				logger.debug("Idle 커넥션 {}개 정리 완료", drained.size());
			}
		}

		private static Connection unwrapPhysical(Connection maybeProxy) {
			try {
				if (maybeProxy.isWrapperFor(Connection.class)) {
					return maybeProxy.unwrap(Connection.class);
				}
			} catch (SQLException ignore) {
			}
			return maybeProxy;
		}

		public Connection borrow(String url, Properties props) throws Exception {
			// 1) idle에서 즉시 얻기
			Connection c = idle.poll();
			if (c != null) {
				logger.debug("Idle 풀에서 커넥션 재사용: {}", key.connectionId);
				return c;
			}

			// 2) 필요시 새로 생성
			int cur = created.get();
			if (cur < maxPoolSize && created.compareAndSet(cur, cur + 1)) {
				Connection physical = isoDriver.connect(url, props);
				long createdAt = System.currentTimeMillis();
				RealConnectionCloser closer = phys -> {
					try {
						phys.close();
					} finally {
						created.decrementAndGet();
					}
				};
				logger.debug("새 커넥션 생성: {} (현재 {}개)", key.connectionId, created.get());
				return wrapPooled(physical, idle, closer, maxLifetimeMs, createdAt);
			}

			// 3) 누군가 반납하기를 대기
			c = idle.poll(connectionTimeoutMs, TimeUnit.MILLISECONDS);
			if (c != null) {
				logger.debug("타임아웃 대기 후 커넥션 획득: {}", key.connectionId);
				return c;
			}
			throw new SQLTimeoutException("Timeout waiting for connection from pool: " + key);
		}

		@Override
		public void close() {
			logger.info("커넥션 풀 정리 시작: {}", key);

			// evictor 종료
			evictor.shutdown();
			try {
				if (!evictor.awaitTermination(1, TimeUnit.SECONDS)) {
					evictor.shutdownNow();
				}
			} catch (InterruptedException e) {
				evictor.shutdownNow();
				Thread.currentThread().interrupt();
			}

			// 모든 idle 커넥션 정리
			List<Connection> list = new ArrayList<>();
			idle.drainTo(list);
			for (Connection c : list) {
				try {
					unwrapPhysical(c).close();
				} catch (Exception e) {
					logger.debug("커넥션 정리 중 오류 (무시): {}", e.getMessage());
				}
				created.decrementAndGet();
			}

			// 드라이버 정리
			try {
				isoDriver.close();
			} catch (Exception e) {
				logger.warn("드라이버 정리 중 오류: {}", e.getMessage());
			}

			logger.info("커넥션 풀 정리 완료: {} (총 {}개 커넥션 정리)", key, list.size());
		}
	}

	/* --------------------------- Pool Manager --------------------------- */
	private final ConcurrentMap<String, IsolatedPool> pools = new ConcurrentHashMap<>();
	private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

	/**
	 * 서비스 시작 시 DATABASE_CONNECTION 테이블에서 정보를 조회하여 커넥션 풀 초기화
	 */
	@PostConstruct
	public void initializeConnectionPools() {
		logger.info("=== 커넥션 풀 초기화 시작 ===");

		// RootPath 유효성 검증
		if (!Common.isRootPathValid()) {
			logger.warn("RootPath가 유효하지 않습니다. 5초 후 다시 시도합니다.");
			// 5초 후 다시 시도
			cleanupExecutor.schedule(this::initializeConnectionPools, 5, TimeUnit.SECONDS);
			return;
		}

		try {
			// DATABASE_CONNECTION 테이블 존재 여부 확인
			if (!isTableExists("DATABASE_CONNECTION")) {
				logger.warn("DATABASE_CONNECTION 테이블이 존재하지 않습니다. 커넥션 풀 초기화를 건너뜁니다.");
				return;
			}

			// 활성 상태의 데이터베이스 연결 정보 조회
			String sql = "SELECT * FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
			List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);

			logger.info("총 {}개의 활성 연결 정보를 발견했습니다.", connections.size());

			for (Map<String, Object> connInfo : connections) {
				try {
					initializePoolForConnection(connInfo);
				} catch (Exception e) {
					logger.error("연결 풀 초기화 실패: {} - {}", connInfo.get("CONNECTION_ID"), e.getMessage(), e);
				}
			}

			// 주기적으로 사용하지 않는 풀 정리 (1시간마다)
			cleanupExecutor.scheduleAtFixedRate(this::cleanupUnusedPools, 1, 1, TimeUnit.HOURS);

			logger.info("=== 커넥션 풀 초기화 완료 (총 {}개 풀) ===", pools.size());

		} catch (Exception e) {
			logger.error("커넥션 풀 초기화 중 오류 발생", e);
		}
	}

	/**
	 * 특정 연결 정보로 커넥션 풀을 초기화합니다.
	 */
	private void initializePoolForConnection(Map<String, Object> connInfo) throws Exception {
		String connectionId = (String) connInfo.get("CONNECTION_ID");
		String dbType = (String) connInfo.get("DB_TYPE");
		String hostIp = (String) connInfo.get("HOST_IP");
		Integer port = (Integer) connInfo.get("PORT");
		String databaseName = (String) connInfo.get("DATABASE_NAME");
		String username = (String) connInfo.get("USERNAME");
		String password = (String) connInfo.get("PASSWORD");
		String jdbcDriverFile = (String) connInfo.get("JDBC_DRIVER_FILE");

		// JDBC URL 생성
		String jdbcUrl = createJdbcUrl(dbType, hostIp, String.valueOf(port), databaseName);

		// 드라이버 클래스명 가져오기
		String driverClassName = common.getDriverByDbType(dbType);

		// 연결 속성 생성
		Properties props = createConnectionProperties(dbType, username, password);

		// JAR 파일 경로 찾기
		String jarPath = findDriverJarPath(jdbcDriverFile);
		if (jarPath == null) {
			throw new Exception("드라이버 JAR 파일을 찾을 수 없습니다: " + jdbcDriverFile);
		}

		// 풀 설정 (데이터베이스에서 가져오거나 기본값 사용)
		int maxPoolSize = connInfo.get("MAX_POOL_SIZE") != null ? (Integer) connInfo.get("MAX_POOL_SIZE") : 10;
		long connectionTimeoutMs = connInfo.get("CONNECTION_TIMEOUT") != null ? (Integer) connInfo.get("CONNECTION_TIMEOUT") * 1000L : 5000L;
		long maxLifetimeMs = 30 * 60 * 1000L; // 30분
		long idleTimeoutMs = 60 * 1000L; // 1분

		// 풀 키 생성
		PoolKey poolKey = new PoolKey(connectionId, Paths.get(jarPath), driverClassName, jdbcUrl, props);

		// 드라이버 로드 및 풀 생성
		IsolatedDriver isoDriver = IsolatedDriver.load(Paths.get(jarPath), driverClassName);
		IsolatedPool pool = new IsolatedPool(poolKey, isoDriver, maxPoolSize, connectionTimeoutMs, maxLifetimeMs, idleTimeoutMs);

		// 풀 등록
		pools.put(connectionId, pool);

		logger.info("커넥션 풀 초기화 완료: {}", connectionId);
	}

	/**
	 * 연결 ID로 커넥션을 가져옵니다 (SQL 실행용).
	 */
	public Connection getConnection(String connectionId) throws Exception {
		// RootPath 유효성 검증
		if (!Common.isRootPathValid()) {
			throw new Exception("RootPath가 유효하지 않아 연결을 가져올 수 없습니다: " + connectionId);
		}
		
		IsolatedPool pool = pools.get(connectionId);
		if (pool == null) {
			throw new Exception("연결 ID에 해당하는 풀이 없습니다: " + connectionId);
		}

		// 연결 정보 조회
		String sql = "SELECT * FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
		Map<String, Object> connInfo = jdbcTemplate.queryForMap(sql, connectionId);

		String jdbcUrl = createJdbcUrl((String) connInfo.get("DB_TYPE"), (String) connInfo.get("HOST_IP"), String.valueOf(connInfo.get("PORT")), (String) connInfo.get("DATABASE_NAME"));

		Properties props = createConnectionProperties((String) connInfo.get("DB_TYPE"), (String) connInfo.get("USERNAME"), (String) connInfo.get("PASSWORD"));

		return pool.borrow(jdbcUrl, props);
	}

	/**
	 * 일회성 커넥션을 생성합니다 (연결 테스트용).
	 */
	public Connection createOneTimeConnection(String driverClassName, String jdbcUrl, Properties props, String jdbcDriverFile) throws Exception {
		// RootPath 유효성 검증
		if (!Common.isRootPathValid()) {
			throw new Exception("RootPath가 유효하지 않아 일회성 연결을 생성할 수 없습니다");
		}
		
		// JAR 파일 경로 찾기
		String jarPath = findDriverJarPath(jdbcDriverFile);
		if (jarPath == null) {
			throw new Exception("드라이버 JAR 파일을 찾을 수 없습니다: " + driverClassName);
		}

		// 일회성 드라이버 로드
		try (IsolatedDriver isoDriver = IsolatedDriver.load(Paths.get(jarPath), driverClassName)) {
			return isoDriver.connect(jdbcUrl, props);
		}
	}

	/**
	 * 드라이버 클래스명과 JAR 파일명으로 JAR 파일 경로를 찾습니다.
	 */
	private String findDriverJarPath(String jdbcDriverFile) {
		// RootPath 유효성 검증
		if (!Common.isRootPathValid()) {
			logger.warn("RootPath가 유효하지 않아 JDBC 드라이버 경로를 찾을 수 없습니다");
			return null;
		}
		
		try {
			File jdbcDir = new File(common.JdbcPath);
			if (!jdbcDir.exists()) {
				logger.warn("JDBC 드라이버 디렉토리가 존재하지 않습니다: {}", common.JdbcPath);
				return null;
			}

			// 1. 지정된 JAR 파일명이 있으면 우선 사용
			if (jdbcDriverFile != null && !jdbcDriverFile.trim().isEmpty()) {
				File specifiedJar = new File(jdbcDir, jdbcDriverFile);
				logger.info("지정된 JAR 파일 사용: {} -> {}", specifiedJar.getAbsolutePath());
				return specifiedJar.getAbsolutePath();
			}

			return null;

		} catch (Exception e) {
			logger.error("드라이버 JAR 파일 검색 중 오류 발생: {}", jdbcDriverFile, e);
			return null;
		}
	}

	/**
	 * 테이블 존재 여부를 확인합니다.
	 */
	private boolean isTableExists(String tableName) {
		try {
			String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE 1=0";
			jdbcTemplate.queryForObject(sql, Integer.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 특정 연결의 풀을 재초기화합니다.
	 */
	public void reinitializePool(String connectionId) throws Exception {
		logger.info("풀 재초기화 시작: {}", connectionId);
		
		// 기존 풀 제거
		IsolatedPool oldPool = pools.remove(connectionId);
		if (oldPool != null) {
			try {
				oldPool.close();
				logger.debug("기존 풀 정리 완료: {}", connectionId);
			} catch (Exception e) {
				logger.warn("기존 풀 정리 중 오류: {} - {}", connectionId, e.getMessage());
			}
		}
		
		// 연결 정보 조회
		String sql = "SELECT * FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
		Map<String, Object> connInfo = jdbcTemplate.queryForMap(sql, connectionId);
		
		// 새 풀 초기화
		initializePoolForConnection(connInfo);
		logger.info("풀 재초기화 완료: {}", connectionId);
	}

	/**
	 * 사용하지 않는 풀들을 정리합니다.
	 */
	private void cleanupUnusedPools() {
		logger.debug("사용하지 않는 풀 정리 시작");

		// 현재 사용 중인 풀 키들을 수집 (실제로는 더 정교한 로직 필요)
		// 여기서는 간단히 모든 풀을 유지
	}

	/**
	 * 모든 리소스를 정리합니다.
	 */
	@PreDestroy
	@Override
	public void close() {
		logger.info("DynamicJdbcManager 정리 시작 ({}개 풀)", pools.size());
		
		long startTime = System.currentTimeMillis();

		// 스케줄러 종료
		cleanupExecutor.shutdown();
		try {
			if (!cleanupExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				cleanupExecutor.shutdownNow();
				if (!cleanupExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
					logger.warn("스케줄러 강제 종료 실패");
				}
			}
		} catch (InterruptedException e) {
			cleanupExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		// 모든 풀 정리
		int poolCount = 0;
		for (IsolatedPool pool : pools.values()) {
			try {
				poolCount++;
				pool.close();
			} catch (Exception e) {
				logger.warn("풀 정리 중 오류: {}", e.getMessage());
			}
		}
		pools.clear();

		// DB2 드라이버 관련 타이머 스레드 정리 시도
		try {
			Class.forName("com.ibm.db2.jcc.am.GlobalProperties");
			DriverManager.deregisterDriver(DriverManager.getDriver("jdbc:db2://"));
		} catch (Exception e) {
			logger.debug("DB2 드라이버 정리 중 오류 (무시): {}", e.getMessage());
		}

		// 모든 등록된 드라이버 정리
		int driverCount = 0;
		try {
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				Driver driver = drivers.nextElement();
				try {
					DriverManager.deregisterDriver(driver);
					driverCount++;
				} catch (Exception e) {
					logger.debug("드라이버 등록 해제 실패: {} - {}", driver.getClass().getName(), e.getMessage());
				}
			}
		} catch (Exception e) {
		}

		// 추가: 시스템 타이머 스레드 정리 시도
		int timerThreadCount = 0;
		try {
			// 모든 타이머 스레드 찾기 및 정리
			ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
			while (rootGroup.getParent() != null) {
				rootGroup = rootGroup.getParent();
			}

			Thread[] threads = new Thread[rootGroup.activeCount()];
			rootGroup.enumerate(threads);

			for (Thread thread : threads) {
				if (thread != null && thread.getName().startsWith("Timer-")) {
					timerThreadCount++;
					try {
						thread.interrupt();
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			logger.warn("드라이버 정리 중 오류: {}", e.getMessage());
		}

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		
		logger.info("DynamicJdbcManager 정리 완료 ({}ms, {}개 풀, {}개 드라이버)", duration, poolCount, driverCount);
	}

	/**
	 * 현재 풀 상태를 반환합니다.
	 */
	public int getPoolCount() {
		return pools.size();
	}

	public Set<String> getPoolKeys() {
		return new HashSet<>(pools.keySet());
	}

	/**
	 * 커넥션 풀을 추가합니다.
	 */
	public void addConnectionPool(String connectionId) {
		try {
			// DATABASE_CONNECTION에서 정보 조회
			Map<String, Object> connInfo = getConnectionInfo(connectionId);

			// 기존 풀이 있으면 먼저 제거
			removeConnectionPool(connectionId);

			// 새 커넥션 풀 생성
			initializePoolForConnection(connInfo);

			logger.info("커넥션 풀 추가 완료: {}", connectionId);
		} catch (Exception e) {
			logger.error("커넥션 풀 추가 실패: {} - {}", connectionId, e.getMessage(), e);
		}
	}

	/**
	 * 커넥션 풀을 삭제합니다.
	 */
	public void removeConnectionPool(String connectionId) {
		IsolatedPool pool = pools.remove(connectionId);
		if (pool != null) {
			try {
				pool.close();
				logger.info("커넥션 풀 삭제 완료: {}", connectionId);
			} catch (Exception e) {
				logger.warn("커넥션 풀 삭제 중 오류: {} - {}", connectionId, e.getMessage());
			}
		}
	}

	/**
	 * 커넥션 풀을 재생성합니다 (delete + create).
	 */
	public void recreateConnectionPool(String connectionId) {
		try {
			logger.info("커넥션 풀 재생성 시작: {}", connectionId);

			// 1. 기존 풀 삭제
			removeConnectionPool(connectionId);

			// 2. 잠시 대기 (진행 중인 커넥션 정리 시간)
			try {
				Thread.sleep(1000); // 1초 대기
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// 3. 새 풀 생성
			addConnectionPool(connectionId);

			logger.info("커넥션 풀 재생성 완료: {}", connectionId);
		} catch (Exception e) {
			logger.error("커넥션 풀 재생성 실패: {} - {}", connectionId, e.getMessage(), e);
		}
	}

	/**
	 * 연결 ID로 연결 정보를 조회합니다.
	 */
	private Map<String, Object> getConnectionInfo(String connectionId) {
		String sql = "SELECT * FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
		return jdbcTemplate.queryForMap(sql, connectionId);
	}

	// JDBC URL 생성 메서드
	public String createJdbcUrl(String dbtype, String ip, String port, String db) {
		String jdbcUrl = "";

		switch (dbtype.toUpperCase()) {
		case "DB2":
			jdbcUrl = "jdbc:db2://" + ip + ":" + port + "/" + db;
			break;
		case "ORACLE":
			jdbcUrl = "jdbc:oracle:thin:@" + ip + ":" + port + "/" + db;
			break;
		case "POSTGRESQL":
			jdbcUrl = "jdbc:postgresql://" + ip + ":" + port + "/" + db;
			break;
		case "TIBERO":
			jdbcUrl = "jdbc:tibero:thin:@" + ip + ":" + port + ":" + db;
			break;
		case "MYSQL":
			jdbcUrl = "jdbc:mysql://" + ip + ":" + port + "/" + db;
			break;
		default:
			jdbcUrl = "jdbc:db2://" + ip + ":" + port + "/" + db;
			break;
		}

		logger.debug("JDBC URL 생성: {} -> {}", dbtype, jdbcUrl);
		return jdbcUrl;
	}

	/**
	 * 데이터베이스 타입별 기본 연결 속성 생성
	 * 
	 * @param dbtype   데이터베이스 타입
	 * @param username 사용자명
	 * @param password 비밀번호
	 * @return 연결 속성
	 */
	public Properties createConnectionProperties(String dbtype, String username, String password) {
		Properties properties = new Properties();

		// 기본 속성
		properties.put("user", username);
		properties.put("password", password);
		properties.put("clientProgramName", "DeX");

		// 타임아웃 설정
		properties.put("connectTimeout", "5000"); // 5초 연결 타임아웃
		properties.put("socketTimeout", "5000"); // 5초 소켓 타임아웃

		// DB2 전용 속성
		if ("DB2".equalsIgnoreCase(dbtype)) {
			properties.put("loginTimeout", "5"); // 로그인 타임아웃 5초
			properties.put("blockingReadConnectionTimeout", "5"); // 읽기 타임아웃 5초
			properties.put("blockingReadConnectionTimeoutUnit", "SECONDS");
			// currentSchema 설정 제거 - 각 연결별로 다른 스키마를 사용할 수 있도록
			properties.put("retrieveMessagesFromServerOnGetMessage", "false"); // 메시지 검색 비활성화
		}

		return properties;
	}
}
