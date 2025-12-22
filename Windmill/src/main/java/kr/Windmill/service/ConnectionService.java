package kr.Windmill.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.Windmill.dto.connection.ConnectionStatusDto;
import kr.Windmill.util.Common;
import kr.Windmill.util.Crypto;
import kr.Windmill.util.DynamicJdbcManager;
import kr.Windmill.util.Log;

@Service
public class ConnectionService {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

	private final Common com;
	private final Log cLog;
	private final DynamicJdbcManager dynamicJdbcManager;

	private final Map<String, ConnectionStatusDto> connectionStatusMap = new ConcurrentHashMap<>();
	private final Map<String, Long> lastMonitoringCheckMap = new ConcurrentHashMap<>();
	private final Map<String, String> monitoringStatusMap = new ConcurrentHashMap<>();
	private Thread monitoringThread;
	private volatile boolean isRunning = false;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PermissionService permissionService;

	@Autowired
	public ConnectionService(Common common, Log log, DynamicJdbcManager dynamicJdbcManager) {
		this.com = common;
		this.cLog = log;
		this.dynamicJdbcManager = dynamicJdbcManager;
	}

	// ==================== 커넥션 풀 이벤트 처리 ====================

	/**
	 * 커넥션 생성 시 호출되는 이벤트 메서드
	 */
	public void onConnectionCreated(String connectionId) {
		try {
			dynamicJdbcManager.addConnectionPool(connectionId);
			logger.info("커넥션 생성 이벤트 처리 완료: {}", connectionId);
		} catch (Exception e) {
			logger.error("커넥션 생성 이벤트 처리 실패: {} - {}", connectionId, e.getMessage(), e);
		}
	}

	/**
	 * 커넥션 수정 시 호출되는 이벤트 메서드 (delete + create)
	 */
	public void onConnectionUpdated(String connectionId) {
		try {
			dynamicJdbcManager.recreateConnectionPool(connectionId);
			logger.info("커넥션 수정 이벤트 처리 완료: {}", connectionId);
		} catch (Exception e) {
			logger.error("커넥션 수정 이벤트 처리 실패: {} - {}", connectionId, e.getMessage(), e);
		}
	}

	/**
	 * 커넥션 삭제 시 호출되는 이벤트 메서드
	 */
	public void onConnectionDeleted(String connectionId) {
		try {
			dynamicJdbcManager.removeConnectionPool(connectionId);
			logger.info("커넥션 삭제 이벤트 처리 완료: {}", connectionId);
		} catch (Exception e) {
			logger.error("커넥션 삭제 이벤트 처리 실패: {} - {}", connectionId, e.getMessage(), e);
		}
	}

	/**
	 * 연결이 존재하는지 확인합니다.
	 */
	public boolean isConnectionExists(String connectionId) {
		try {
			String sql = "SELECT COUNT(*) FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			int count = jdbcTemplate.queryForObject(sql, Integer.class, connectionId);
			return count > 0;
		} catch (Exception e) {
			logger.debug("연결 존재 여부 확인 실패: {} - {}", connectionId, e.getMessage());
			return false;
		}
	}

	/**
	 * 기존 커넥션 풀을 사용하여 TEST_SQL로 연결을 테스트합니다.
	 */
	private boolean testConnectionWithPool(String connectionId) {
		// RootPath 유효성 검증
		if (!Common.isRootPathValid()) {
			logger.warn("RootPath가 유효하지 않아 연결 테스트를 건너뜁니다: {}", connectionId);
			return false;
		}
		
		try {
			
			// CompletableFuture를 사용하여 타임아웃 설정 (10초)
			CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
				Connection conn = null;
				try {
					// DynamicJdbcManager에서 커넥션 가져오기
					conn = dynamicJdbcManager.getConnection(connectionId);

					// TEST_SQL 조회
					String testSql = getTestSql(connectionId);
					if (testSql == null || testSql.trim().isEmpty()) {
						// TEST_SQL이 없으면 기본 테스트 쿼리 사용
						testSql = "SELECT 1";
					}

					// TEST_SQL 실행
					try (PreparedStatement stmt = conn.prepareStatement(testSql)) {
						stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
						stmt.executeQuery();
					}

					return true;
				} catch (Exception e) {
					logger.error("커넥션 풀 테스트 실패: {} - {}", connectionId, e.getMessage());
					
					// 절전 모드 복귀 후 연결 실패 시 풀 재생성 시도
					if (isLikelySleepModeRecovery(e)) {
						logger.info("절전 모드 복귀로 인한 연결 실패로 판단, 풀 재생성을 시도합니다: {}", connectionId);
						try {
							// 풀 재생성 시도
							dynamicJdbcManager.reinitializePool(connectionId);
							logger.info("풀 재생성 완료: {}", connectionId);
						} catch (Exception reinitEx) {
							logger.error("풀 재생성 실패: {} - {}", connectionId, reinitEx.getMessage());
						}
					} else {
						// 일반적인 연결 실패의 경우 상세 로그
						logger.debug("일반적인 연결 실패: {} - {}", connectionId, e.getClass().getSimpleName());
					}
					
					return false;
				} finally {
					if (conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {
							logger.debug("커넥션 닫기 실패: {}", e.getMessage());
						}
					}
				}
			});
			
			// 10초 타임아웃으로 결과 대기
			boolean result = future.get(10, TimeUnit.SECONDS);
			return result;
			
		} catch (TimeoutException e) {
			logger.error("커넥션 풀 테스트 타임아웃: {} - 10초", connectionId);
			return false;
		} catch (Exception e) {
			System.err.println("ConnectionService: 연결 테스트 예외 [" + connectionId + "] - " + e.getMessage());
			logger.error("커넥션 풀 테스트 예외: {} - {}", connectionId, e.getMessage());
			return false;
		}
	}


	/**
	 * 절전 모드 복귀로 인한 연결 실패인지 판단합니다.
	 */
	private boolean isLikelySleepModeRecovery(Exception e) {
		String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
		String errorClass = e.getClass().getSimpleName();
		
		// 절전 모드 복귀 후 발생하는 일반적인 오류들
		return errorMessage.contains("connection") && 
			   (errorMessage.contains("closed") || 
				errorMessage.contains("timeout") || 
				errorMessage.contains("broken") ||
				errorMessage.contains("reset") ||
				errorClass.contains("SQLException") ||
				errorClass.contains("CommunicationsException"));
	}

	/**
	 * 연결 ID로 TEST_SQL을 조회합니다. TEST_SQL이 없으면 DB_TYPE에 맞는 기본 테스트 SQL을 반환합니다.
	 */
	private String getTestSql(String connectionId) {
		try {
			String sql = "SELECT TEST_SQL, DB_TYPE FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			Map<String, Object> result = jdbcTemplate.queryForMap(sql, connectionId);
			
			String testSql = (String) result.get("TEST_SQL");
			if (testSql != null && !testSql.trim().isEmpty()) {
				return testSql;
			}
			
			// TEST_SQL이 없으면 DB_TYPE에 맞는 기본 테스트 SQL 사용
			String dbType = (String) result.get("DB_TYPE");
			return getDefaultTestSql(dbType);
		} catch (Exception e) {
			logger.debug("TEST_SQL 조회 실패: {} - {}", connectionId, e.getMessage());
			return "SELECT 1"; // 기본값
		}
	}

	/**
	 * DB_TYPE에 맞는 기본 테스트 SQL을 반환합니다.
	 */
	private String getDefaultTestSql(String dbType) {
		if (dbType == null) {
			return "SELECT 1";
		}
		
		switch (dbType.toUpperCase()) {
			case "ORACLE":
				return "SELECT 1 FROM DUAL";
			case "DB2":
				return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
			case "TIBERO":
				return "SELECT 1 FROM DUAL";
			case "POSTGRESQL":
				return "SELECT 1";
			case "MYSQL":
				return "SELECT 1";
			default:
				return "SELECT 1";
		}
	}

	// ==================== 연결 상태 모니터링 ====================

	@PostConstruct
	public void startMonitoring() {
		isRunning = true;

		// 연결 목록을 미리 확인중 상태로 초기화
		try {
			List<String> connectionList = com.ConnectionnList();

			for (String connectionId : connectionList) {
				ConnectionStatusDto status = new ConnectionStatusDto(connectionId, "checking", // 확인중 상태
						"#ffc107" // 노란색
				);
				status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
				connectionStatusMap.put(connectionId, status);
				// 연결 상태 초기화 로그 제거 (단순 정보성)
			}
		} catch (Exception e) {
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "초기 연결 목록 로드 중 오류 발생: " + e.getMessage());
		}

		monitoringThread = new Thread(this::monitorConnections, "ConnectionStatusMonitor");
		monitoringThread.setDaemon(true);
		monitoringThread.start();
	}

	@PreDestroy
	public void stopMonitoring() {
		// 모니터링 중지 시작 로그 제거 (단순 정보성)
		logger.info("=== ConnectionService 정리 시작 ===");
		isRunning = false;

		if (monitoringThread != null && monitoringThread.isAlive()) {
			logger.info("ConnectionStatusMonitor 스레드 종료 중...");
			monitoringThread.interrupt();
			try {
				monitoringThread.join(3000);
				if (monitoringThread.isAlive()) {
					cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드가 3초 내에 종료되지 않았습니다. 강제 종료합니다.");
					logger.info("ConnectionStatusMonitor 스레드 종료 시도...");
					monitoringThread.interrupt();
					monitoringThread.join(1000);
				} else {
					// 모니터링 스레드 정상 종료 로그 제거 (단순 정보성)
					logger.info("ConnectionStatusMonitor 스레드 정상 종료 완료");
				}
			} catch (InterruptedException e) {
				cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드 종료 대기 중 인터럽트 발생");
				logger.info("ConnectionStatusMonitor 스레드 종료 중 인터럽트 발생");
				Thread.currentThread().interrupt();
			}
		} else {
			logger.info("ConnectionStatusMonitor 스레드가 이미 종료되었거나 존재하지 않습니다.");
		}

		connectionStatusMap.clear();
		logger.info("연결 상태 맵 정리 완료");

		// 동적 드라이버 매니저 정리

		// 모니터링 중지 완료 로그 제거 (단순 정보성)
		logger.info("=== ConnectionService 정리 완료 ===");
	}

	private void monitorConnections() {
		while (isRunning) {
			try {
				// RootPath 유효성 검증
				if (!Common.isRootPathValid()) {
					logger.warn("RootPath가 유효하지 않아 모니터링을 일시 중단합니다. 30초 후 다시 시도합니다.");
					Thread.sleep(30000); // 30초 대기
					continue;
				}
				
				updateAllConnectionStatusesWithInterval();
				Thread.sleep(10000); // 10초 대기
			} catch (InterruptedException e) {
				if (isRunning) {
					cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드 인터럽트 발생");
				}
				break;
			} catch (Exception e) {
				cLog.monitoringLog("CONNECTION_STATUS_ERROR", "모니터링 중 오류 발생: " + e.getMessage());
				try {
					Thread.sleep(5000); // 오류 발생 시 5초 대기
				} catch (InterruptedException ie) {
					break;
				}
			}
		}
	}

	private void updateAllConnectionStatusesWithInterval() {
		
		try {
			cLog.monitoringLog("CONNECTION_STATUS", "=== 연결 상태 확인 시작 ===");

			List<String> connectionList = com.ConnectionnList();
			cLog.monitoringLog("CONNECTION_STATUS", "발견된 연결 목록: " + connectionList);

			long currentTime = System.currentTimeMillis();
			
			for (String connectionId : connectionList) {
				// 모니터링이 활성화되지 않은 연결은 스킵
				if (!isMonitoringEnabled(connectionId)) {
					// 모니터링이 비활성화된 연결은 connectionStatusMap에서 제거
					if (connectionStatusMap.containsKey(connectionId)) {
						logger.debug("모니터링 비활성화로 인한 연결 상태 제거: {}", connectionId);
						connectionStatusMap.remove(connectionId);
						monitoringStatusMap.remove(connectionId);
						lastMonitoringCheckMap.remove(connectionId);
					}
					continue;
				}
				
				//여기확
				// 마지막 모니터링 체크 시간 확인
				Long lastCheck = getLastMonitoringCheck(connectionId);
				int interval = getMonitoringInterval(connectionId) * 1000; // 초를 밀리초로 변환
				
				
				// 모니터링 간격이 지났거나 처음 체크하는 경우에만 업데이트
				if (lastCheck == null || (currentTime - lastCheck) >= interval) {
					cLog.monitoringLog("CONNECTION_STATUS", "연결 상태 확인 중: " + connectionId + " (간격: " + (interval/1000) + "초)");
					updateConnectionStatus(connectionId);
				} else {
					logger.debug("모니터링 간격 미도달 스킵: {} (남은 시간: {}초)", connectionId, (interval - (currentTime - lastCheck))/1000);
				}
			}
		} catch (Exception e) {
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "연결 상태 확인 중 오류 발생: " + e.getMessage());
		}
	}

	private void updateConnectionStatus(String connectionId) {
		try {
			// RootPath 유효성 검증
			if (!Common.isRootPathValid()) {
				logger.warn("RootPath가 유효하지 않아 연결 상태 확인을 건너뜁니다: {}", connectionId);
				return;
			}
			
			// 모니터링이 활성화되지 않은 연결은 스킵
			if (!isMonitoringEnabled(connectionId)) {
				logger.debug("모니터링이 비활성화된 연결 스킵: {}", connectionId);
				return;
			}

			// 기존 커넥션 풀에서 연결 가져와서 TEST_SQL 실행
			boolean isConnected = testConnectionWithPool(connectionId);
			
			// 연결 테스트 완료 후 마지막 모니터링 체크 시간 업데이트
			setLastMonitoringCheck(connectionId);

			// 모니터링 상태 설정
			String monitoringStatus = isConnected ? "ONLINE" : "OFFLINE";
			setMonitoringStatus(connectionId, monitoringStatus);

			// 기존 상태가 있으면 업데이트, 없으면 새로 생성
			ConnectionStatusDto status = connectionStatusMap.get(connectionId);
			if (status == null) {
				status = new ConnectionStatusDto(connectionId, isConnected ? "connected" : "disconnected", isConnected ? "#28a745" : "#dc3545");
			} else {
				status.setStatus(isConnected ? "connected" : "disconnected");
				status.setColor(isConnected ? "#28a745" : "#dc3545");
				status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			}

			connectionStatusMap.put(connectionId, status);

			if (isConnected) {
				// 성공한 연결은 로그 레벨을 낮춤 (DEBUG)
				logger.debug("DB 연결 상태 확인 완료: {} - 연결됨", connectionId);
			} else {
				cLog.monitoringLog("CONNECTION_STATUS_WARN", "DB 연결 상태 확인 완료: " + connectionId + " - 연결실패");
			}

		} catch (Exception e) {
			// 모니터링 상태를 ERROR로 설정
			setMonitoringStatus(connectionId, "ERROR");

			ConnectionStatusDto status = connectionStatusMap.get(connectionId);
			if (status == null) {
				status = new ConnectionStatusDto(connectionId, "error", "#dc3545");
			} else {
				status.setStatus("error");
				status.setColor("#dc3545");
				status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			}
			status.setErrorMessage(e.getMessage());
			connectionStatusMap.put(connectionId, status);
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "DB 연결 상태 확인 완료: " + connectionId + " - 오류발생: " + e.getMessage());
		}
	}

	// ==================== 연결 상태 조회 ====================

	public List<ConnectionStatusDto> getAllConnectionStatuses() {
		return connectionStatusMap.values().stream()
				.filter(status -> isMonitoringEnabled(status.getConnectionId()))
				.collect(Collectors.toList());
	}

	/**
	 * 온라인 상태인 연결 ID 목록을 반환합니다.
	 * @return 온라인 연결 ID 목록
	 */
	public List<String> getOnlineConnectionIds() {
		return connectionStatusMap.values().stream()
				.filter(status -> isMonitoringEnabled(status.getConnectionId()))
				.filter(status -> "connected".equals(status.getStatus()))
				.map(ConnectionStatusDto::getConnectionId)
				.collect(Collectors.toList());
	}

	public List<ConnectionStatusDto> getConnectionStatusesForUser(String userId) {
		try {
					if (permissionService.isAdmin(userId)) {
			return getAllConnectionStatuses();
		}

			// 사용자가 권한을 가진 연결 ID 목록 조회
			List<String> authorizedConnectionIds = getUserDatabaseConnections(userId);

			return connectionStatusMap.values().stream()
					.filter(status -> authorizedConnectionIds.contains(status.getConnectionId()))
					.filter(status -> isMonitoringEnabled(status.getConnectionId()))
					.collect(Collectors.toList());

		} catch (Exception e) {
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "Error getting user configuration for " + userId + ": " + e.getMessage());
			return new ArrayList<>();
		}
	}

	public ConnectionStatusDto getConnectionStatus(String connectionId) {
		return connectionStatusMap.get(connectionId);
	}

	// 연결의 모니터링 활성화 여부 확인
	private boolean isMonitoringEnabled(String connectionId) {
		try {
			String sql = "SELECT MONITORING_ENABLED, STATUS FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			Map<String, Object> result = jdbcTemplate.queryForMap(sql, connectionId);
			
			// STATUS가 INACTIVE면 모니터링 비활성화로 간주
			String status = (String) result.get("STATUS");
			if (status != null && !"ACTIVE".equals(status)) {
				return false;
			}
			
			Object monitoringEnabledObj = result.get("MONITORING_ENABLED");
			Boolean monitoringEnabled = convertToBoolean(monitoringEnabledObj);
			return monitoringEnabled != null && monitoringEnabled;
		} catch (Exception e) {
			logger.warn("모니터링 설정 조회 실패: {}", connectionId, e);
			return false; // 기본값으로 모니터링 비활성화 (안전한 기본값)
		}
	}

	// 연결의 모니터링 간격 조회
	private int getMonitoringInterval(String connectionId) {
		try {
			String sql = "SELECT MONITORING_INTERVAL FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			Integer interval = jdbcTemplate.queryForObject(sql, Integer.class, connectionId);
			return interval != null ? interval : 300; // 기본값 5분
		} catch (Exception e) {
			logger.warn("모니터링 간격 조회 실패: {}", connectionId, e);
			return 300; // 기본값으로 5분
		}
	}

	// 마지막 모니터링 체크 시간 설정
	private void setLastMonitoringCheck(String connectionId) {
		lastMonitoringCheckMap.put(connectionId, System.currentTimeMillis());
	}

	// 마지막 모니터링 체크 시간 조회
	private Long getLastMonitoringCheck(String connectionId) {
		return lastMonitoringCheckMap.get(connectionId);
	}

	// 모니터링 상태 설정
	private void setMonitoringStatus(String connectionId, String status) {
		monitoringStatusMap.put(connectionId, status);
	}

	// 모니터링 상태 조회
	private String getMonitoringStatus(String connectionId) {
		return monitoringStatusMap.getOrDefault(connectionId, "UNKNOWN");
	}

	// 수동으로 특정 연결 상태 업데이트
	public void updateConnectionStatusManually(String connectionId) {
		cLog.monitoringLog("CONNECTION_STATUS_MANUAL", "수동 DB 연결 상태 확인 요청: " + connectionId);
		updateConnectionStatus(connectionId);
		cLog.monitoringLog("CONNECTION_STATUS_MANUAL", "수동 DB 연결 상태 확인 완료: " + connectionId);
	}

	// ==================== 연결 테스트 및 생성 ====================

	/**
	 * 데이터베이스 연결을 테스트합니다.
	 * 
	 * @param connConfig 연결 설정 정보
	 * @return 테스트 결과
	 */
	public Map<String, Object> testDatabaseConnection(Map<String, String> connConfig) {
		Map<String, Object> result = new HashMap<>();
		Connection conn = null;

		try {
			String dbType = connConfig.get("DBTYPE");
			if (dbType == null || dbType.trim().isEmpty()) {
				result.put("success", false);
				result.put("error", "DB 타입이 지정되지 않았습니다.");
				result.put("errorType", "INVALID_CONFIG");
				return result;
			}

			String driver = com.getDriverByDbType(dbType);

			// 연결 테스트는 항상 새로운 연결 정보로 생성 (캐시 사용 안함)
			// 패스워드 복호화 (평문 호환)
			String password = connConfig.get("PW");
			String decryptedPassword = decryptPassword(password);
			
			String url = dynamicJdbcManager.createJdbcUrl(dbType, connConfig.get("IP"), connConfig.get("PORT"), connConfig.get("DB"));
			// 공통 연결 속성 생성
			Properties prop = dynamicJdbcManager.createConnectionProperties(dbType, connConfig.get("USER"), decryptedPassword);

			// 공통 JDBC 연결 메서드 사용
			try {
				conn = dynamicJdbcManager.createOneTimeConnection(driver, url, prop, connConfig.get("JDBC_DRIVER_FILE"));
				logger.info("기본 JDBC 연결 성공: {}", driver);
			} catch (Exception e) {
				String errorMessage = e.getMessage();
				String errorType = "CONNECTION_FAILED";

				// 오류 유형별 메시지 개선
				if (errorMessage.contains("선택한 JDBC 드라이버 파일로 연결할 수 없습니다")) {
					errorMessage = "선택한 JDBC 드라이버 파일이 DB 타입과 일치하지 않거나 올바르지 않습니다. 올바른 드라이버를 선택해주세요.";
					errorType = "DRIVER_MISMATCH";
				} else if (errorMessage.contains("Connection refused") || errorMessage.contains("No route to host")) {
					errorMessage = "서버에 연결할 수 없습니다. IP 주소와 포트를 확인해주세요.";
					errorType = "NETWORK_ERROR";
				} else if (errorMessage.contains("authentication failed") || errorMessage.contains("password")) {
					errorMessage = "사용자명 또는 비밀번호가 올바르지 않습니다.";
					errorType = "AUTHENTICATION_ERROR";
				} else if (errorMessage.contains("database") && errorMessage.contains("does not exist")) {
					errorMessage = "지정된 데이터베이스가 존재하지 않습니다.";
					errorType = "DATABASE_NOT_FOUND";
				} else if (errorMessage.contains("timeout")) {
					errorMessage = "연결 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.";
					errorType = "TIMEOUT_ERROR";
				} else if (errorMessage.contains("permission denied")) {
					errorMessage = "데이터베이스 접근 권한이 없습니다.";
					errorType = "PERMISSION_ERROR";
				}

				result.put("success", false);
				result.put("error", errorMessage);
				result.put("errorType", errorType);
				result.put("originalError", e.getMessage());
				return result;
			}

			// 연결 테스트용 SQL 실행
			String testQuery = connConfig.get("TEST_SQL");
			if (testQuery == null || testQuery.trim().isEmpty()) {
				// TEST_SQL이 없으면 기본 테스트 쿼리 사용
				testQuery = com.getTestQueryByDbType(dbType);
			}

			try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
				stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						// 연결 정보 로깅
						logger.info("DB 연결 테스트 성공 - DB: {}, User: {}, Query: {}", connConfig.get("DB"), connConfig.get("USER"), testQuery);
					}
				}
			} catch (SQLException e) {
				result.put("success", false);
				result.put("error", "연결은 성공했지만 테스트 쿼리 실행에 실패했습니다: " + e.getMessage());
				result.put("errorType", "QUERY_ERROR");
				result.put("testQuery", testQuery);
				return result;
			}

			result.put("success", true);
			return result;

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "데이터베이스 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
			result.put("errorType", "UNKNOWN_ERROR");
			result.put("originalError", e.getMessage());

			String errorDetails = String.format("Database connection test failed for %s:%s (DB: %s, Type: %s, User: %s) - Error: %s",
					connConfig.get("IP"), connConfig.get("PORT"), connConfig.get("DB"), connConfig.get("DBTYPE"), connConfig.get("USER"),
					e.getMessage());

			logger.error(errorDetails, e);
			return result;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Error closing connection", e);
				}
			}
		}
	}

	/**
	 * SFTP 연결을 테스트합니다.
	 * 
	 * @param connConfig 연결 설정 정보
	 * @return 테스트 결과
	 */
	public Map<String, Object> testSftpConnection(Map<String, String> connConfig) {
		Map<String, Object> result = new HashMap<>();

		try {
			// SFTP 연결 테스트 (JSch 라이브러리 사용)
			String host = connConfig.get("IP");
			int port = Integer.parseInt(connConfig.get("PORT"));
			String username = connConfig.get("USER");
			// 패스워드 복호화 (평문 호환)
			String encryptedPassword = connConfig.get("PW");
			String password = decryptPassword(encryptedPassword);

			// JSch를 사용한 SFTP 연결 테스트
			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
			com.jcraft.jsch.Session session = null;
			com.jcraft.jsch.ChannelSftp channelSftp = null;

			try {
				// 세션 생성
				session = jsch.getSession(username, host, port);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setTimeout(30000); // 30초 타임아웃

				// 비밀번호가 있으면 설정
				if (password != null && !password.trim().isEmpty()) {
					session.setPassword(password);
				}

				// 연결 시도
				session.connect();

				// SFTP 채널 열기
				channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
				channelSftp.connect();

				// 기본 경로 접근 확인
				channelSftp.cd("/");
				logger.info("SFTP 연결 테스트 성공 - Host: {}, User: {}", host, username);

				result.put("success", true);
				return result;

			} catch (com.jcraft.jsch.JSchException e) {
				String errorMessage = e.getMessage();
				String errorType = "CONNECTION_FAILED";

				// 오류 유형별 메시지 개선
				if (errorMessage.contains("Connection refused") || errorMessage.contains("No route to host")) {
					errorMessage = "서버에 연결할 수 없습니다. IP 주소와 포트를 확인해주세요.";
					errorType = "NETWORK_ERROR";
				} else if (errorMessage.contains("Auth fail") || errorMessage.contains("authentication")) {
					errorMessage = "사용자명 또는 비밀번호가 올바르지 않습니다.";
					errorType = "AUTHENTICATION_ERROR";
				} else if (errorMessage.contains("timeout")) {
					errorMessage = "연결 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.";
					errorType = "TIMEOUT_ERROR";
				} else if (errorMessage.contains("Permission denied")) {
					errorMessage = "SFTP 접근 권한이 없습니다.";
					errorType = "PERMISSION_ERROR";
				}

				result.put("success", false);
				result.put("error", errorMessage);
				result.put("errorType", errorType);
				result.put("originalError", e.getMessage());
				return result;

			} finally {
				if (channelSftp != null && channelSftp.isConnected()) {
					channelSftp.disconnect();
				}
				if (session != null && session.isConnected()) {
					session.disconnect();
				}
			}

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "SFTP 연결 테스트 중 오류가 발생했습니다: " + e.getMessage());
			result.put("errorType", "UNKNOWN_ERROR");
			result.put("originalError", e.getMessage());

			String errorDetails = String.format("SFTP connection test failed for %s:%s (User: %s) - Error: %s", connConfig.get("IP"),
					connConfig.get("PORT"), connConfig.get("USER"), e.getMessage());

			logger.error(errorDetails, e);
			return result;
		}
	}

	// ==================== 유틸리티 메서드 ====================

	/**
	 * 드라이버 정보를 추출합니다.
	 * 
	 * @param driverFileName 드라이버 파일명
	 * @return 드라이버 정보 맵
	 */
	public Map<String, String> extractDriverInfo(String driverFileName) {
		return com.extractDriverInfo(driverFileName);
	}

	/**
	 * 패스워드를 복호화합니다. 평문인 경우 그대로 반환합니다 (기존 데이터 호환).
	 * 
	 * @param encryptedPassword 암호화된 패스워드 또는 평문 패스워드
	 * @return 복호화된 패스워드 또는 평문 패스워드
	 */
	private String decryptPassword(String encryptedPassword) {
		if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
			return encryptedPassword;
		}
		
		try {
			String decrypted = Crypto.deCrypt(encryptedPassword);
			// 복호화 실패 시 빈 문자열이 반환되므로, 원본이 평문인 것으로 간주
			if (decrypted == null || decrypted.isEmpty()) {
				return encryptedPassword; // 평문으로 간주
			}
			return decrypted;
		} catch (Exception e) {
			logger.debug("패스워드 복호화 실패 (평문으로 간주): {}", e.getMessage());
			return encryptedPassword; // 평문으로 간주
		}
	}

	/**
	 * 패스워드를 암호화합니다.
	 * 
	 * @param plainPassword 평문 패스워드
	 * @return 암호화된 패스워드
	 */
	private String encryptPassword(String plainPassword) {
		if (plainPassword == null || plainPassword.trim().isEmpty()) {
			return plainPassword;
		}
		return Crypto.crypt(plainPassword);
	}


	/**
	 * 연결 캐시에서 특정 연결을 제거합니다.
	 * 
	 * @param connectionId 연결 ID
	 */
	public void removeConnectionFromCache(String connectionId) {
		// ConnectionStatusMap에서 제거
		connectionStatusMap.remove(connectionId);

		logger.info("연결 캐시에서 제거됨: {}", connectionId);
	}

	// ==================== DB 기반 연결 관리 ====================

	/**
	 * 모든 연결 목록을 조회합니다 (DB + SFTP)
	 * 
	 * @param userId 사용자 ID
	 * @return 연결 목록
	 */
	public List<Map<String, Object>> getAllConnections(String userId) {
		List<Map<String, Object>> allConnections = new ArrayList<>();

		// DB 연결 조회
		List<Map<String, Object>> dbConnections = getDatabaseConnections(userId);
		for (Map<String, Object> conn : dbConnections) {
			conn.put("TYPE", "DB");
		}
		allConnections.addAll(dbConnections);

		// SFTP 연결 조회
		List<Map<String, Object>> sftpConnections = getSftpConnections(userId);
		for (Map<String, Object> conn : sftpConnections) {
			conn.put("TYPE", "HOST");

		}
		allConnections.addAll(sftpConnections);

		return allConnections;
	}

	/**
	 * 페이징을 포함한 연결 목록을 가져옵니다.
	 * 
	 * @param userId        사용자 ID
	 * @param searchKeyword 검색 키워드
	 * @param typeFilter    타입 필터 (DB/HOST)
	 * @param dbTypeFilter  DB 타입 필터 (ORACLE, POSTGRESQL, DB2, MYSQL, TIBERO)
	 * @param page          현재 페이지
	 * @param pageSize      페이지 크기
	 * @return 연결 목록과 페이징 정보
	 */
	public Map<String, Object> getConnectionListWithPagination(String userId, String searchKeyword, String typeFilter, String dbTypeFilter, int page, int pageSize) {
		Map<String, Object> result = new HashMap<>();

		try {
			// 전체 연결 목록 가져오기
			List<Map<String, Object>> allConnections = getAllConnections(userId);

			// 검색 및 필터링 적용
			List<Map<String, Object>> filteredConnections = filterConnections(allConnections, searchKeyword, typeFilter, dbTypeFilter);

			// 페이징 계산
			int totalCount = filteredConnections.size();
			int totalPages = (int) Math.ceil((double) totalCount / pageSize);
			int startIndex = (page - 1) * pageSize;
			int endIndex = Math.min(startIndex + pageSize, totalCount);

			// 현재 페이지 데이터 추출
			List<Map<String, Object>> pageData = new ArrayList<>();
			if (startIndex < totalCount) {
				pageData = filteredConnections.subList(startIndex, endIndex);
			}

			// DB 타입 이름 추가 (DB 연결에만)
			for (Map<String, Object> conn : pageData) {
				if ("DB".equals(conn.get("TYPE"))) {
					String dbType = (String) conn.get("DB_TYPE");
					conn.put("DB_TYPE_NAME", getDbTypeName(dbType));
				} else {
					conn.put("DB_TYPE_NAME", "-");
				}
			}

			// 페이징 정보 구성
			Map<String, Object> pagination = new HashMap<>();
			pagination.put("currentPage", page);
			pagination.put("totalPages", totalPages);
			pagination.put("totalCount", totalCount);
			pagination.put("pageSize", pageSize);
			pagination.put("startIndex", startIndex + 1);
			pagination.put("endIndex", endIndex);

			result.put("connections", pageData);
			result.put("pagination", pagination);

		} catch (Exception e) {
			logger.error("페이징 연결 목록 조회 실패", e);
			result.put("connections", new ArrayList<>());
			result.put("pagination", new HashMap<>());
		}

		return result;
	}

	/**
	 * 연결 목록을 검색 및 필터링합니다.
	 * 
	 * @param connections   전체 연결 목록
	 * @param searchKeyword 검색 키워드
	 * @param typeFilter    타입 필터 (DB/HOST)
	 * @param dbTypeFilter DB 타입 필터 (ORACLE, POSTGRESQL, DB2, MYSQL, TIBERO)
	 * @return 필터링된 연결 목록
	 */
	private List<Map<String, Object>> filterConnections(List<Map<String, Object>> connections, String searchKeyword, String typeFilter, String dbTypeFilter) {
		return connections.stream().filter(conn -> {
			// DB 타입 필터가 있으면 HOST 연결은 제외
			if (dbTypeFilter != null && !dbTypeFilter.isEmpty()) {
				String type = (String) conn.get("TYPE");
				if (!"DB".equals(type)) {
					return false; // DB 타입 필터가 있으면 HOST 연결은 제외
				}
				// DB 타입 필터 적용
				String dbType = (String) conn.get("DB_TYPE");
				if (dbType == null || !dbTypeFilter.equalsIgnoreCase(dbType)) {
					return false;
				}
			}

			// 타입 필터 적용
			if (typeFilter != null && !typeFilter.isEmpty()) {
				String type = (String) conn.get("TYPE");
				if (!typeFilter.equals(type)) {
					return false;
				}
			}

			// 검색 키워드 적용
			if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
				String connectionId = (String) conn.get("CONNECTION_ID");
				String hostIp = (String) conn.get("HOST_IP");

				boolean matchesId = connectionId != null && connectionId.toLowerCase().contains(searchKeyword.toLowerCase());
				boolean matchesIp = hostIp != null && hostIp.toLowerCase().contains(searchKeyword.toLowerCase());

				if (!matchesId && !matchesIp) {
					return false;
				}
			}

			return true;
		}).collect(Collectors.toList());
	}
	
	/**
	 * DB 타입 코드를 읽기 쉬운 이름으로 변환합니다.
	 * 
	 * @param dbType DB 타입 코드
	 * @return DB 타입 이름
	 */
	private String getDbTypeName(String dbType) {
		if (dbType == null || dbType.trim().isEmpty()) {
			return "-";
		}
		
		switch (dbType.toUpperCase()) {
			case "ORACLE":
				return "Oracle";
			case "POSTGRESQL":
				return "PostgreSQL";
			case "DB2":
				return "DB2";
			case "MYSQL":
				return "MySQL";
			case "TIBERO":
				return "Tibero";
			default:
				return dbType;
		}
	}

	/**
	 * 데이터베이스 연결 목록을 조회합니다
	 * 
	 * @param userId 사용자 ID
	 * @return DB 연결 목록
	 */
	public List<Map<String, Object>> getDatabaseConnections(String userId) {
		String sql = "SELECT * FROM DATABASE_CONNECTION ORDER BY CONNECTION_ID";
		List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);

		// 권한 필터링 적용
		return filterConnectionsByPermission(userId, connections, "DATABASE");
	}

	/**
	 * 사용자가 권한을 가진 데이터베이스 연결 목록을 조회합니다 (sql.jsp용)
	 * 
	 * @param userId 사용자 ID
	 * @return DB 연결 ID 목록
	 */
	public List<String> getUserDatabaseConnections(String userId) {
		// Common.ConnectionnList를 사용하여 DB 연결 목록 조회
		List<String> allConnections = com.ConnectionnList();

		// admin 사용자는 모든 연결에 접근 가능
		if (permissionService.isAdmin(userId)) {
			return allConnections;
		}

		// 일반 사용자는 권한이 있는 연결만 필터링
		List<String> authorizedConnections = new ArrayList<>();

		try {
			// 사용자 권한 조회
			String sql = "SELECT DISTINCT dc.CONNECTION_ID " 
					+ "FROM DATABASE_CONNECTION dc "
					+ "LEFT JOIN GROUP_CONNECTION_MAPPING gcm ON dc.CONNECTION_ID = gcm.CONNECTION_ID "
					+ "LEFT JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " + "WHERE dc.STATUS = 'ACTIVE' " 
					+ "AND ugm.USER_ID = ? "
					+ "ORDER BY dc.CONNECTION_ID";

			List<Map<String, Object>> authorizedConnectionsFromDB = jdbcTemplate.queryForList(sql, userId);
			Set<String> authorizedConnectionIds = new HashSet<>();

			for (Map<String, Object> connection : authorizedConnectionsFromDB) {
				String connectionId = (String) connection.get("CONNECTION_ID");
				if (connectionId != null) {
					authorizedConnectionIds.add(connectionId);
				}
			}

			// Common.ConnectionnList 결과와 권한 정보를 매칭
			for (String connection : allConnections) {
				if (authorizedConnectionIds.contains(connection)) {
					authorizedConnections.add(connection);
				}
			}

		} catch (Exception e) {
			logger.warn("사용자 권한 조회 실패, 빈 목록 반환: {} - {}", userId, e.getMessage());
		}

		return authorizedConnections;
	}

	/**
	 * 사용자가 권한을 가진 SFTP 연결 목록을 조회합니다
	 * 
	 * @param userId 사용자 ID
	 * @return SFTP 연결 ID 목록
	 */
	public List<Map<String, Object>> getSftpConnections(String userId) {
		String sql = "SELECT * FROM SFTP_CONNECTION ORDER BY SFTP_CONNECTION_ID";
		List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);

		// SFTP_CONNECTION_ID를 CONNECTION_ID로 매핑
		for (Map<String, Object> conn : connections) {
			conn.put("CONNECTION_ID", conn.get("SFTP_CONNECTION_ID"));
			}

		// 권한 필터링 적용
		return filterConnectionsByPermission(userId, connections, "SFTP");

	}

	public List<Map<String, Object>> getSftpConnectionsWithDetails(String userId) {
		String sql = "SELECT SFTP_CONNECTION_ID AS CONNECTION_ID, HOST_IP, PORT, USERNAME, STATUS, " +
					 "CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP " +
					 "FROM SFTP_CONNECTION ORDER BY SFTP_CONNECTION_ID";
		
		List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);
		
		// 권한 필터링 적용
		return filterConnectionsByPermission(userId, connections, "SFTP");
	}

	/**
	 * 연결 상세 정보를 조회합니다
	 * 
	 * @param connectionId   연결 ID
	 * @param connectionType 연결 타입 (DB/HOST)
	 * @return 연결 상세 정보
	 */
	public Map<String, Object> getConnectionDetail(String connectionId, String connectionType) {
		if ("DB".equals(connectionType)) {
			String sql = "SELECT * FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, connectionId);
			if (result.isEmpty()) {
				return null;
			}
			Map<String, Object> detail = result.get(0);
			
			// 패스워드 복호화 및 자동 마이그레이션
			String password = (String) detail.get("PASSWORD");
			if (password != null && !password.trim().isEmpty()) {
				String decrypted = decryptPassword(password);
				// 평문이면 암호화하여 저장
				if (decrypted.equals(password)) {
					logger.info("평문 패스워드 발견, 자동 암호화 저장: {} (DB)", connectionId);
					String encrypted = encryptPassword(password);
					String updateSql = "UPDATE DATABASE_CONNECTION SET PASSWORD = ? WHERE CONNECTION_ID = ?";
					jdbcTemplate.update(updateSql, encrypted, connectionId);
					// 복호화된 비밀번호를 반환 (수정 화면에서 표시하기 위해)
					detail.put("PASSWORD", decrypted);
				} else {
					// 복호화된 비밀번호를 반환 (수정 화면에서 표시하기 위해)
					detail.put("PASSWORD", decrypted);
				}
			}
			return detail;
		} else {
			String sql = "SELECT SFTP_CONNECTION_ID AS CONNECTION_ID, HOST_IP, PORT, USERNAME, PASSWORD, STATUS, CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ?";
			List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, connectionId);
			if (result.isEmpty()) {
				return null;
			}
			Map<String, Object> detail = result.get(0);
			
			// 패스워드 복호화 및 자동 마이그레이션
			String password = (String) detail.get("PASSWORD");
			if (password != null && !password.trim().isEmpty()) {
				String decrypted = decryptPassword(password);
				// 평문이면 암호화하여 저장
				if (decrypted.equals(password)) {
					logger.info("평문 패스워드 발견, 자동 암호화 저장: {} (SFTP)", connectionId);
					String encrypted = encryptPassword(password);
					String updateSql = "UPDATE SFTP_CONNECTION SET PASSWORD = ? WHERE SFTP_CONNECTION_ID = ?";
					jdbcTemplate.update(updateSql, encrypted, connectionId);
					// 복호화된 비밀번호를 반환 (수정 화면에서 표시하기 위해)
					detail.put("PASSWORD", decrypted);
				} else {
					// 복호화된 비밀번호를 반환 (수정 화면에서 표시하기 위해)
					detail.put("PASSWORD", decrypted);
				}
			}
			return detail;
		}
	}

	/**
	 * 연결을 저장합니다 (생성 또는 수정)
	 * 
	 * @param connectionData 연결 데이터
	 * @param userId         사용자 ID
	 * @return 저장 결과
	 */
	@Transactional
	public boolean saveConnection(Map<String, Object> connectionData, String userId) {
		try {
			String connectionType = (String) connectionData.get("TYPE");
			String editConnectionId = (String) connectionData.get("editConnectionId");
			boolean isNew = editConnectionId == null || editConnectionId.trim().isEmpty();

			if (isNew) {
				// 새 연결 생성
				if ("DB".equals(connectionType)) {
					return saveDatabaseConnection(connectionData, userId);
				} else {
					return saveSftpConnection(connectionData, userId);
				}
			} else {
				// 기존 연결 수정 - 타입 변경 확인
				// DB_TYPE이 있으면 원래 DB 연결, 없으면 SFTP 연결
				boolean wasDatabaseConnection = connectionData.get("DB_TYPE") != null;
				boolean isDatabaseConnection = "DB".equals(connectionType);
				
				if (wasDatabaseConnection != isDatabaseConnection) {
					// 타입이 변경된 경우: 기존 연결 삭제 후 새 타입으로 추가
					String originalType = wasDatabaseConnection ? "DB" : "HOST";
					logger.info("연결 타입 변경: {} -> {} (ID: {})", originalType, connectionType, editConnectionId);
					
					// 기존 연결 삭제
					if (wasDatabaseConnection) {
						deleteDatabaseConnection(editConnectionId);
					} else {
						deleteSftpConnection(editConnectionId);
					}
					
					// 새 타입으로 연결 추가
					if (isDatabaseConnection) {
						return saveDatabaseConnection(connectionData, userId);
					} else {
						return saveSftpConnection(connectionData, userId);
					}
				} else {
					// 타입이 동일한 경우: 기존 방식으로 수정
					if (isDatabaseConnection) {
						return saveDatabaseConnection(connectionData, userId);
					} else {
						return saveSftpConnection(connectionData, userId);
					}
				}
			}
		} catch (Exception e) {
			logger.error("연결 저장 실패", e);
			return false;
		}
	}

	/**
	 * 데이터베이스 연결을 저장합니다
	 * 
	 * @param connectionData 연결 데이터
	 * @param userId         사용자 ID
	 * @return 저장 결과
	 */
	private boolean saveDatabaseConnection(Map<String, Object> connectionData, String userId) {
		String connectionId = (String) connectionData.get("editConnectionId");
		boolean isNew = connectionId == null || connectionId.trim().isEmpty();

		if (isNew) {
			// 새 연결 생성
			// 패스워드 암호화
			String password = (String) connectionData.get("PASSWORD");
			String encryptedPassword = encryptPassword(password);
			
			String sql = "INSERT INTO DATABASE_CONNECTION (CONNECTION_ID, DB_TYPE, HOST_IP, PORT, "
					+ "DATABASE_NAME, USERNAME, PASSWORD, JDBC_DRIVER_FILE, CONNECTION_POOL_SETTINGS, "
					+ "CONNECTION_TIMEOUT, QUERY_TIMEOUT, MAX_POOL_SIZE, MIN_POOL_SIZE, STATUS, "
					+ "MONITORING_ENABLED, MONITORING_INTERVAL, TEST_SQL, CREATED_BY) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			jdbcTemplate.update(sql, connectionData.get("CONNECTION_ID"), connectionData.get("DB_TYPE"), connectionData.get("HOST_IP"), connectionData.get("PORT"),
					connectionData.get("DATABASE_NAME"), connectionData.get("USERNAME"), encryptedPassword,
					connectionData.get("JDBC_DRIVER_FILE"), connectionData.get("CONNECTION_POOL_SETTINGS"), connectionData.get("CONNECTION_TIMEOUT"),
					connectionData.get("QUERY_TIMEOUT"), connectionData.get("MAX_POOL_SIZE"), connectionData.get("MIN_POOL_SIZE"), 
					connectionData.get("STATUS"), connectionData.get("MONITORING_ENABLED"), connectionData.get("MONITORING_INTERVAL"), 
					connectionData.get("TEST_SQL"), userId);
		} else {
			// 기존 연결 수정 - 모니터링 설정 변경 확인
			boolean monitoringEnabledChanged = checkMonitoringSettingChanged(connectionId, connectionData);
			
			// STATUS 변경 확인
			Map<String, Object> statusChangeInfo = checkStatusChanged(connectionId, connectionData);
			boolean statusChanged = (Boolean) statusChangeInfo.get("changed");
			String newStatus = (String) statusChangeInfo.get("newStatus");
			
			// 연결 ID 변경 여부 확인
			String newConnectionId = (String) connectionData.get("CONNECTION_ID");
			boolean connectionIdChanged = !connectionId.equals(newConnectionId);
			
			// 실제 사용할 연결 ID 결정 (연결 ID가 변경된 경우 새 ID 사용)
			String targetConnectionId = connectionIdChanged ? newConnectionId : connectionId;
			
			// 기존 연결 수정
			// 패스워드 암호화 (패스워드가 제공된 경우에만)
			String password = (String) connectionData.get("PASSWORD");
			String encryptedPassword = password != null && !password.trim().isEmpty() ? encryptPassword(password) : null;
			
			String sql = "UPDATE DATABASE_CONNECTION SET CONNECTION_ID = ?, DB_TYPE = ?, HOST_IP = ?, PORT = ?, "
					+ "DATABASE_NAME = ?, USERNAME = ?, PASSWORD = ?, JDBC_DRIVER_FILE = ?, "
					+ "CONNECTION_POOL_SETTINGS = ?, CONNECTION_TIMEOUT = ?, QUERY_TIMEOUT = ?, "
					+ "MAX_POOL_SIZE = ?, MIN_POOL_SIZE = ?, STATUS = ?, MONITORING_ENABLED = ?, "
					+ "MONITORING_INTERVAL = ?, TEST_SQL = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " + "WHERE CONNECTION_ID = ?";

			jdbcTemplate.update(sql, connectionData.get("CONNECTION_ID"), connectionData.get("DB_TYPE"), connectionData.get("HOST_IP"), connectionData.get("PORT"),
					connectionData.get("DATABASE_NAME"), connectionData.get("USERNAME"), encryptedPassword != null ? encryptedPassword : connectionData.get("PASSWORD"),
					connectionData.get("JDBC_DRIVER_FILE"), connectionData.get("CONNECTION_POOL_SETTINGS"), connectionData.get("CONNECTION_TIMEOUT"),
					connectionData.get("QUERY_TIMEOUT"), connectionData.get("MAX_POOL_SIZE"), connectionData.get("MIN_POOL_SIZE"), 
					connectionData.get("STATUS"), connectionData.get("MONITORING_ENABLED"), connectionData.get("MONITORING_INTERVAL"), 
					connectionData.get("TEST_SQL"), userId, connectionId);
			
			// 연결 ID가 변경된 경우 연결 상태 캐시 정리
			if (connectionIdChanged) {
				// 이전 ID의 상태 제거
				connectionStatusMap.remove(connectionId);
				monitoringStatusMap.remove(connectionId);
				lastMonitoringCheckMap.remove(connectionId);
				
				// 새 ID의 상태 초기화 (STATUS가 ACTIVE인 경우에만)
				if ("ACTIVE".equals(newStatus)) {
					ConnectionStatusDto newStatusDto = new ConnectionStatusDto();
					newStatusDto.setConnectionId(newConnectionId);
					newStatusDto.setStatus("checking");
					newStatusDto.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					newStatusDto.setColor("#ffc107");
					connectionStatusMap.put(newConnectionId, newStatusDto);
				}
				
				logger.info("연결 ID 변경으로 인한 상태 캐시 정리: {} -> {}", connectionId, newConnectionId);
			}
			
			// STATUS가 INACTIVE로 변경된 경우 connectionStatusMap에서 제거
			if (statusChanged && "INACTIVE".equals(newStatus)) {
				logger.info("STATUS가 INACTIVE로 변경되어 연결 상태 캐시에서 제거: {}", targetConnectionId);
				connectionStatusMap.remove(targetConnectionId);
				monitoringStatusMap.remove(targetConnectionId);
				lastMonitoringCheckMap.remove(targetConnectionId);
				cLog.monitoringLog("CONNECTION_STATUS", "STATUS가 INACTIVE로 변경됨: " + targetConnectionId);
			}
			
			// 모니터링 설정이 변경된 경우 관련 상태 초기화
			if (monitoringEnabledChanged) {
				updateMonitoringStatusAfterSettingChange(targetConnectionId, connectionData);
			}
			
			// 연결 정보 업데이트 후 풀 재생성 이벤트 호출 (STATUS가 ACTIVE인 경우에만)
			if ("ACTIVE".equals(newStatus)) {
				try {
					onConnectionUpdated(targetConnectionId);
				} catch (Exception e) {
					logger.warn("연결 업데이트 이벤트 처리 실패: {} - {}", targetConnectionId, e.getMessage());
				}
			}
		}

		return true;
	}

	/**
	 * 모니터링 설정 변경 여부를 확인합니다
	 * 
	 * @param connectionId 연결 ID
	 * @param connectionData 연결 데이터
	 * @return 모니터링 설정 변경 여부
	 */
	private boolean checkMonitoringSettingChanged(String connectionId, Map<String, Object> connectionData) {
		try {
			String sql = "SELECT MONITORING_ENABLED, MONITORING_INTERVAL FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			Map<String, Object> currentSettings = jdbcTemplate.queryForMap(sql, connectionId);
			
			// DB에서 가져온 값 타입 변환 (Boolean 또는 숫자 0/1로 올 수 있음)
			Object currentEnabledObj = currentSettings.get("MONITORING_ENABLED");
			Boolean currentEnabled = convertToBoolean(currentEnabledObj);
			Integer currentInterval = (Integer) currentSettings.get("MONITORING_INTERVAL");
			
			// 요청 파라미터는 String으로 올 수 있으므로 변환 필요
			Object newEnabledObj = connectionData.get("MONITORING_ENABLED");
			Boolean newEnabled = convertToBoolean(newEnabledObj);
			
			Object newIntervalObj = connectionData.get("MONITORING_INTERVAL");
			Integer newInterval = convertToInteger(newIntervalObj);
			
			// null 체크 및 비교
			boolean enabledChanged = (currentEnabled == null && newEnabled != null) || 
									(currentEnabled != null && !currentEnabled.equals(newEnabled));
			boolean intervalChanged = (currentInterval == null && newInterval != null) || 
									 (currentInterval != null && !currentInterval.equals(newInterval));
			
			return enabledChanged || intervalChanged;
		} catch (Exception e) {
			logger.warn("모니터링 설정 변경 확인 실패: {}", connectionId, e);
			return false;
		}
	}

	/**
	 * 다양한 타입을 Boolean으로 변환합니다.
	 * 
	 * @param value 변환할 값
	 * @return Boolean 값 (null 가능)
	 */
	private Boolean convertToBoolean(Object value) {
		if (value == null) {
			return null;
		}
		
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		
		if (value instanceof String) {
			String str = ((String) value).trim().toLowerCase();
			if ("true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str)) {
				return true;
			} else if ("false".equals(str) || "0".equals(str) || "no".equals(str) || "off".equals(str) || str.isEmpty()) {
				return false;
			}
			return null;
		}
		
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		
		return null;
	}

	/**
	 * 다양한 타입을 Integer로 변환합니다.
	 * 
	 * @param value 변환할 값
	 * @return Integer 값 (null 가능)
	 */
	private Integer convertToInteger(Object value) {
		if (value == null) {
			return null;
		}
		
		if (value instanceof Integer) {
			return (Integer) value;
		}
		
		if (value instanceof String) {
			try {
				String str = ((String) value).trim();
				if (str.isEmpty()) {
					return null;
				}
				return Integer.parseInt(str);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		
		return null;
	}

	/**
	 * STATUS 변경 여부를 확인합니다
	 * 
	 * @param connectionId 연결 ID
	 * @param connectionData 연결 데이터
	 * @return STATUS 변경 여부 및 변경 전 STATUS
	 */
	private Map<String, Object> checkStatusChanged(String connectionId, Map<String, Object> connectionData) {
		Map<String, Object> result = new HashMap<>();
		try {
			String sql = "SELECT STATUS FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			String currentStatus = jdbcTemplate.queryForObject(sql, String.class, connectionId);
			String newStatus = (String) connectionData.get("STATUS");
			
			boolean statusChanged = (currentStatus == null && newStatus != null) || 
								   (currentStatus != null && !currentStatus.equals(newStatus));
			
			result.put("changed", statusChanged);
			result.put("currentStatus", currentStatus);
			result.put("newStatus", newStatus);
			
			return result;
		} catch (Exception e) {
			logger.warn("STATUS 변경 확인 실패: {}", connectionId, e);
			result.put("changed", false);
			return result;
		}
	}

	/**
	 * 모니터링 설정 변경 후 상태를 업데이트합니다
	 * 
	 * @param connectionId 연결 ID (실제 사용할 연결 ID - 변경된 경우 새 ID)
	 * @param connectionData 연결 데이터
	 */
	private void updateMonitoringStatusAfterSettingChange(String connectionId, Map<String, Object> connectionData) {
		try {
			Object monitoringEnabledObj = connectionData.get("MONITORING_ENABLED");
			Boolean monitoringEnabled = convertToBoolean(monitoringEnabledObj);
			String status = (String) connectionData.get("STATUS");
			
			// STATUS가 INACTIVE면 모니터링 상태 맵에서 제거만 수행
			if ("INACTIVE".equals(status)) {
				logger.info("STATUS가 INACTIVE이므로 모니터링 상태 초기화: {}", connectionId);
				lastMonitoringCheckMap.remove(connectionId);
				monitoringStatusMap.remove(connectionId);
				connectionStatusMap.remove(connectionId);
				return;
			}
			
			if (monitoringEnabled != null && !monitoringEnabled) {
				// 모니터링이 비활성화된 경우
				logger.info("모니터링 비활성화로 인한 상태 초기화: {}", connectionId);
				
				// 모니터링 상태 맵에서 제거
				lastMonitoringCheckMap.remove(connectionId);
				monitoringStatusMap.remove(connectionId);
				connectionStatusMap.remove(connectionId);
				
				cLog.monitoringLog("CONNECTION_STATUS", "모니터링 비활성화: " + connectionId);
			} else {
				// 모니터링이 활성화된 경우 - 즉시 상태 확인
				logger.info("모니터링 활성화로 인한 즉시 상태 확인: {}", connectionId);
				
				// 마지막 체크 시간 초기화하여 즉시 확인하도록 함
				lastMonitoringCheckMap.remove(connectionId);
				
				// 즉시 상태 확인하여 connectionStatusMap에 추가
				// 별도 스레드에서 실행하지 않고 현재 스레드에서 즉시 실행
				try {
					updateConnectionStatus(connectionId);
				} catch (Exception e) {
					logger.error("모니터링 활성화 시 즉시 상태 확인 실패: {}", connectionId, e);
					// 실패 시에도 checking 상태로 추가하여 화면에 표시되도록 함
					ConnectionStatusDto initialStatus = new ConnectionStatusDto(connectionId, "checking", "#ffc107");
					initialStatus.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					connectionStatusMap.put(connectionId, initialStatus);
				}
				
				cLog.monitoringLog("CONNECTION_STATUS", "모니터링 활성화 및 즉시 확인: " + connectionId);
			}
		} catch (Exception e) {
			logger.error("모니터링 상태 업데이트 실패: {}", connectionId, e);
		}
	}

	/**
	 * SFTP 연결을 저장합니다
	 * 
	 * @param connectionData 연결 데이터
	 * @param userId         사용자 ID
	 * @return 저장 결과
	 */
	private boolean saveSftpConnection(Map<String, Object> connectionData, String userId) {
		String connectionId = (String) connectionData.get("CONNECTION_ID");
		
		// 기존 연결인지 확인 (editConnectionId가 있으면 수정, 없으면 신규)
		String editConnectionId = (String) connectionData.get("editConnectionId");
		boolean isNew = editConnectionId == null || editConnectionId.trim().isEmpty();

		if (isNew) {
			// 새 연결 생성 - 화면에서 입력받은 ID 사용
			if (connectionId == null || connectionId.trim().isEmpty()) {
				// ID가 없으면 자동 생성
				connectionId = "SFTP_" + System.currentTimeMillis();
			}
			connectionData.put("CONNECTION_ID", connectionId); // ID를 connectionData에 설정
			
			// 패스워드 암호화
			String password = (String) connectionData.get("PASSWORD");
			String encryptedPassword = encryptPassword(password);
			
			String sql = "INSERT INTO SFTP_CONNECTION (SFTP_CONNECTION_ID, HOST_IP, PORT, "
					+ "USERNAME, PASSWORD, STATUS, CREATED_BY) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";

			jdbcTemplate.update(sql, connectionId, connectionData.get("HOST_IP"), connectionData.get("PORT"), connectionData.get("USERNAME"),
					encryptedPassword, connectionData.get("STATUS"), userId);
		} else {
			// 기존 연결 수정 - editConnectionId 사용
			// 패스워드 암호화 (패스워드가 제공된 경우에만)
			String password = (String) connectionData.get("PASSWORD");
			String encryptedPassword = password != null && !password.trim().isEmpty() ? encryptPassword(password) : null;
			
			String sql = "UPDATE SFTP_CONNECTION SET SFTP_CONNECTION_ID = ?, HOST_IP = ?, PORT = ?, " + "USERNAME = ?, PASSWORD = ?, STATUS = ?, "
					+ "MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " + "WHERE SFTP_CONNECTION_ID = ?";

			jdbcTemplate.update(sql, connectionId, connectionData.get("HOST_IP"), connectionData.get("PORT"), connectionData.get("USERNAME"),
					encryptedPassword != null ? encryptedPassword : connectionData.get("PASSWORD"), connectionData.get("STATUS"), userId, editConnectionId);
		}

		return true;
	}



	/**
	 * 데이터베이스 연결을 삭제합니다
	 * 
	 * @param connectionId 연결 ID
	 * @return 삭제 결과
	 */
	private boolean deleteDatabaseConnection(String connectionId) {
		try {
			String sql = "DELETE FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
			jdbcTemplate.update(sql, connectionId);
			return true;
		} catch (Exception e) {
			logger.error("데이터베이스 연결 삭제 실패: {}", connectionId, e);
			return false;
		}
	}

	/**
	 * SFTP 연결을 삭제합니다
	 * 
	 * @param connectionId 연결 ID
	 * @return 삭제 결과
	 */
	private boolean deleteSftpConnection(String connectionId) {
		try {
			// 소프트 삭제: STATUS를 INACTIVE로 변경
			String sql = "UPDATE SFTP_CONNECTION SET STATUS = 'INACTIVE', MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE SFTP_CONNECTION_ID = ?";
			jdbcTemplate.update(sql, connectionId);
			return true;
		} catch (Exception e) {
			logger.error("SFTP 연결 삭제 실패: {}", connectionId, e);
			return false;
		}
	}

	/**
	 * 연결을 삭제합니다
	 * 
	 * @param connectionId   연결 ID
	 * @param connectionType 연결 타입 (DB/HOST)
	 * @return 삭제 결과
	 */
	@Transactional
	public boolean deleteConnection(String connectionId, String connectionType) {
		try {
			if ("DB".equals(connectionType)) {
				String sql = "DELETE FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ?";
				jdbcTemplate.update(sql, connectionId);
			} else {
				String sql = "DELETE FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ?";
				jdbcTemplate.update(sql, connectionId);
			}
			return true;
		} catch (Exception e) {
			logger.error("연결 삭제 실패: {}", connectionId, e);
			return false;
		}
	}

	/**
	 * 사용자 권한에 따라 연결을 필터링합니다
	 * 
	 * @param userId         사용자 ID
	 * @param connections    연결 목록
	 * @param connectionType 연결 타입
	 * @return 필터링된 연결 목록
	 */
	private List<Map<String, Object>> filterConnectionsByPermission(String userId, List<Map<String, Object>> connections, String connectionType) {
		// 관리자는 모든 연결에 접근 가능
		if (permissionService.isAdmin(userId)) {
			return connections;
		}

		// 사용자의 그룹 조회
		String groupId = getUserGroup(userId);
		if (groupId == null) {
			return new ArrayList<>();
		}

		// 그룹의 연결 권한 조회
		List<String> allowedConnections = getGroupConnectionPermissions(groupId, connectionType);

		// 권한이 있는 연결만 반환
		return connections.stream().filter(conn -> {
			String connId;
			if ("SFTP".equals(connectionType)) {
				// SFTP 연결의 경우 CONNECTION_ID를 사용 (getSftpConnections에서 매핑됨)
				connId = (String) conn.get("CONNECTION_ID");
			} else {
				connId = (String) conn.get("CONNECTION_ID");
			}
			return allowedConnections.contains(connId);
		}).collect(Collectors.toList());
	}

	/**
	 * 사용자의 그룹을 조회합니다
	 * 
	 * @param userId 사용자 ID
	 * @return 그룹 ID
	 */
	private String getUserGroup(String userId) {
		String sql = "SELECT GROUP_ID FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
		try {
			return jdbcTemplate.queryForObject(sql, String.class, userId);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 그룹의 연결 권한을 조회합니다
	 * 
	 * @param groupId        그룹 ID
	 * @param connectionType 연결 타입
	 * @return 권한이 있는 연결 ID 목록
	 */
	private List<String> getGroupConnectionPermissions(String groupId, String connectionType) {
		String sql = "SELECT CONNECTION_ID FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ?";
		try {
			return jdbcTemplate.queryForList(sql, String.class, groupId);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

    public List<String> getUserSftpConnections(String userId) {
		// admin 사용자는 모든 SFTP 연결에 접근 가능
		if (permissionService.isAdmin(userId)) {
			String sql = "SELECT SFTP_CONNECTION_ID FROM SFTP_CONNECTION ORDER BY SFTP_CONNECTION_ID";
			return jdbcTemplate.queryForList(sql, String.class);
		}

		// 일반 사용자는 권한이 있는 SFTP 연결만 필터링
		List<String> authorizedConnections = new ArrayList<>();

		try {
			// 사용자 권한 조회
			String sql = "SELECT DISTINCT sc.SFTP_CONNECTION_ID " 
					+ "FROM SFTP_CONNECTION sc "
					+ "LEFT JOIN GROUP_CONNECTION_MAPPING gcm ON sc.SFTP_CONNECTION_ID = gcm.CONNECTION_ID "
					+ "LEFT JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " 
					+ "WHERE sc.STATUS = 'ACTIVE' " 
					+ "AND ugm.USER_ID = ? "
					+ "ORDER BY sc.SFTP_CONNECTION_ID";

			List<Map<String, Object>> authorizedConnectionsFromDB = jdbcTemplate.queryForList(sql, userId);
			
			for (Map<String, Object> connection : authorizedConnectionsFromDB) {
				String connectionId = (String) connection.get("SFTP_CONNECTION_ID");
				if (connectionId != null) {
					authorizedConnections.add(connectionId);
				}
			}

		} catch (Exception e) {
			logger.warn("SFTP 연결 권한 조회 실패, 빈 목록 반환: {} - {}", userId, e.getMessage());
		}

		return authorizedConnections;
	}
}
