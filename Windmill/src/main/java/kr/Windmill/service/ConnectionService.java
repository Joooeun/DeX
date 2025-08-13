package kr.Windmill.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;
import kr.Windmill.util.Log;

@Service
public class ConnectionService {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

	private final Common com;
	private final Log cLog;
	private final DynamicJdbcManager dynamicJdbcManager;

	private final Map<String, ConnectionStatusDTO> connectionStatusMap = new ConcurrentHashMap<>();
	private Thread monitoringThread;
	private volatile boolean isRunning = false;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
			logger.error("커넥션 풀 테스트 실패: {} - {}", connectionId, e.getStackTrace());
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("커넥션 닫기 실패", e);
				}
			}
		}
	}

	/**
	 * 연결 ID로 TEST_SQL을 조회합니다.
	 */
	private String getTestSql(String connectionId) {
		try {
			String sql = "SELECT TEST_SQL FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			return jdbcTemplate.queryForObject(sql, String.class, connectionId);
		} catch (Exception e) {
			logger.debug("TEST_SQL 조회 실패: {} - {}", connectionId, e.getMessage());
			return null;
		}
	}

	// ==================== 연결 상태 모니터링 ====================

	@PostConstruct
	public void startMonitoring() {
		cLog.monitoringLog("CONNECTION_STATUS", "Connection status monitoring started");
		isRunning = true;

		// 연결 목록을 미리 확인중 상태로 초기화
		try {
			List<String> connectionList = com.ConnectionnList();
			cLog.monitoringLog("CONNECTION_STATUS", "초기 연결 목록 로드: " + connectionList);

			for (String connectionId : connectionList) {
				ConnectionStatusDTO status = new ConnectionStatusDTO(connectionId, "checking", // 확인중 상태
						"#ffc107" // 노란색
				);
				status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
				connectionStatusMap.put(connectionId, status);
				cLog.monitoringLog("CONNECTION_STATUS", "연결 상태 초기화: " + connectionId + " - 확인중");
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
		cLog.monitoringLog("CONNECTION_STATUS", "Connection status monitoring stopping...");
		System.out.println("=== ConnectionService 정리 시작 ===");
		isRunning = false;

		if (monitoringThread != null && monitoringThread.isAlive()) {
			System.out.println("ConnectionStatusMonitor 스레드 종료 중...");
			monitoringThread.interrupt();
			try {
				monitoringThread.join(3000);
				if (monitoringThread.isAlive()) {
					cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드가 3초 내에 종료되지 않았습니다. 강제 종료합니다.");
					System.out.println("ConnectionStatusMonitor 스레드 강제 종료 시도...");
					monitoringThread.interrupt();
					monitoringThread.join(1000);
				} else {
					cLog.monitoringLog("CONNECTION_STATUS", "모니터링 스레드가 정상적으로 종료되었습니다.");
					System.out.println("ConnectionStatusMonitor 스레드 정상 종료 완료");
				}
			} catch (InterruptedException e) {
				cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드 종료 대기 중 인터럽트 발생");
				System.out.println("ConnectionStatusMonitor 스레드 종료 중 인터럽트 발생");
				Thread.currentThread().interrupt();
			}
		} else {
			System.out.println("ConnectionStatusMonitor 스레드가 이미 종료되었거나 존재하지 않습니다.");
		}

		connectionStatusMap.clear();
		System.out.println("연결 상태 맵 정리 완료");

		// 동적 드라이버 매니저 정리

		cLog.monitoringLog("CONNECTION_STATUS", "Connection status monitoring stopped");
		System.out.println("=== ConnectionService 정리 완료 ===");
	}

	private void monitorConnections() {
		while (isRunning) {
			try {
				updateAllConnectionStatuses();
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

	private void updateAllConnectionStatuses() {
		try {
			cLog.monitoringLog("CONNECTION_STATUS", "=== 연결 상태 확인 시작 ===");

			List<String> connectionList = com.ConnectionnList();
			cLog.monitoringLog("CONNECTION_STATUS", "발견된 연결 목록: " + connectionList);

			for (String connectionId : connectionList) {
				cLog.monitoringLog("CONNECTION_STATUS", "연결 상태 확인 중: " + connectionId);
				updateConnectionStatus(connectionId);
			}
		} catch (Exception e) {
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "연결 목록 조회 중 오류 발생: " + e.getMessage());
		}
	}

	private void updateConnectionStatus(String connectionId) {
		try {
			// 기존 커넥션 풀에서 연결 가져와서 TEST_SQL 실행
			boolean isConnected = testConnectionWithPool(connectionId);

			// 기존 상태가 있으면 업데이트, 없으면 새로 생성
			ConnectionStatusDTO status = connectionStatusMap.get(connectionId);
			if (status == null) {
				status = new ConnectionStatusDTO(connectionId, isConnected ? "connected" : "disconnected", isConnected ? "#28a745" : "#dc3545");
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
			ConnectionStatusDTO status = connectionStatusMap.get(connectionId);
			if (status == null) {
				status = new ConnectionStatusDTO(connectionId, "error", "#dc3545");
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

	public List<ConnectionStatusDTO> getAllConnectionStatuses() {
		return new ArrayList<>(connectionStatusMap.values());
	}

	public List<ConnectionStatusDTO> getConnectionStatusesForUser(String userId) {
		try {
			if ("admin".equals(userId)) {
				return getAllConnectionStatuses();
			}

			// 사용자가 권한을 가진 연결 ID 목록 조회
			List<String> authorizedConnectionIds = getUserDatabaseConnections(userId);

			return connectionStatusMap.values().stream().filter(status -> authorizedConnectionIds.contains(status.getConnectionId()))
					.collect(Collectors.toList());

		} catch (Exception e) {
			cLog.monitoringLog("CONNECTION_STATUS_ERROR", "Error getting user configuration for " + userId + ": " + e.getMessage());
			return new ArrayList<>();
		}
	}

	public ConnectionStatusDTO getConnectionStatus(String connectionId) {
		return connectionStatusMap.get(connectionId);
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
			String url = dynamicJdbcManager.createJdbcUrl(dbType, connConfig.get("IP"), connConfig.get("PORT"), connConfig.get("DB"));
			// 공통 연결 속성 생성
			Properties prop = dynamicJdbcManager.createConnectionProperties(dbType, connConfig.get("USER"), connConfig.get("PW"));

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
	private Map<String, Object> testSftpConnection(Map<String, String> connConfig) {
		Map<String, Object> result = new HashMap<>();

		try {
			// SFTP 연결 테스트 (JSch 라이브러리 사용)
			String host = connConfig.get("IP");
			int port = Integer.parseInt(connConfig.get("PORT"));
			String username = connConfig.get("USER");
			String password = connConfig.get("PW");
			String privateKeyPath = connConfig.get("PRIVATE_KEY_PATH");
			String remotePath = connConfig.get("REMOTE_PATH");
			int timeout = Integer.parseInt(connConfig.getOrDefault("CONNECTION_TIMEOUT", "30"));

			// JSch를 사용한 SFTP 연결 테스트
			com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
			com.jcraft.jsch.Session session = null;
			com.jcraft.jsch.ChannelSftp channelSftp = null;

			try {
				// 개인키가 있으면 추가
				if (privateKeyPath != null && !privateKeyPath.trim().isEmpty()) {
					jsch.addIdentity(privateKeyPath);
				}

				// 세션 생성
				session = jsch.getSession(username, host, port);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setTimeout(timeout * 1000); // 밀리초 단위

				// 비밀번호가 있으면 설정
				if (password != null && !password.trim().isEmpty()) {
					session.setPassword(password);
				}

				// 연결 시도
				session.connect();

				// SFTP 채널 열기
				channelSftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
				channelSftp.connect();

				// 원격 경로가 있으면 접근 가능한지 확인
				if (remotePath != null && !remotePath.trim().isEmpty()) {
					try {
						channelSftp.cd(remotePath);
						logger.info("SFTP 연결 테스트 성공 - Host: {}, User: {}, Path: {}", host, username, remotePath);
					} catch (Exception e) {
						result.put("success", false);
						result.put("error", "연결은 성공했지만 지정된 경로에 접근할 수 없습니다: " + remotePath);
						result.put("errorType", "PATH_ACCESS_ERROR");
						return result;
					}
				} else {
					logger.info("SFTP 연결 테스트 성공 - Host: {}, User: {}", host, username);
				}

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
	 * @param typeFilter    타입 필터
	 * @param page          현재 페이지
	 * @param pageSize      페이지 크기
	 * @return 연결 목록과 페이징 정보
	 */
	public Map<String, Object> getConnectionListWithPagination(String userId, String searchKeyword, String typeFilter, int page, int pageSize) {
		Map<String, Object> result = new HashMap<>();

		try {
			// 전체 연결 목록 가져오기
			List<Map<String, Object>> allConnections = getAllConnections(userId);

			// 검색 및 필터링 적용
			List<Map<String, Object>> filteredConnections = filterConnections(allConnections, searchKeyword, typeFilter);

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
	 * @param typeFilter    타입 필터
	 * @return 필터링된 연결 목록
	 */
	private List<Map<String, Object>> filterConnections(List<Map<String, Object>> connections, String searchKeyword, String typeFilter) {
		return connections.stream().filter(conn -> {
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
	 * 데이터베이스 연결 목록을 조회합니다
	 * 
	 * @param userId 사용자 ID
	 * @return DB 연결 목록
	 */
	public List<Map<String, Object>> getDatabaseConnections(String userId) {
		String sql = "SELECT * FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
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
		if ("admin".equals(userId)) {
			return allConnections;
		}

		// 일반 사용자는 권한이 있는 연결만 필터링
		List<String> authorizedConnections = new ArrayList<>();

		try {
			// 사용자 권한 조회
			String sql = "SELECT DISTINCT dc.CONNECTION_ID " + "FROM DATABASE_CONNECTION dc "
					+ "LEFT JOIN CONNECTION_PERMISSIONS cp ON dc.CONNECTION_ID = cp.CONNECTION_ID "
					+ "LEFT JOIN USER_GROUP_MAPPING ugm ON cp.GROUP_ID = ugm.GROUP_ID " + "WHERE dc.STATUS = 'ACTIVE' " + "AND ugm.USER_ID = ? "
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
	 * SFTP 연결 목록을 조회합니다
	 * 
	 * @param userId 사용자 ID
	 * @return SFTP 연결 목록
	 */
	public List<Map<String, Object>> getSftpConnections(String userId) {
		String sql = "SELECT * FROM SFTP_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY SFTP_CONNECTION_ID";
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
			return result.isEmpty() ? null : result.get(0);
		} else {
			String sql = "SELECT * FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ?";
			List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, connectionId);
			return result.isEmpty() ? null : result.get(0);
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

			if ("DB".equals(connectionType)) {
				return saveDatabaseConnection(connectionData, userId);
			} else {
				return saveSftpConnection(connectionData, userId);
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
		String connectionId = (String) connectionData.get("CONNECTION_ID");
		boolean isNew = connectionId == null || connectionId.trim().isEmpty();

		if (isNew) {
			// 새 연결 생성
			connectionId = "DB_" + System.currentTimeMillis();
			String sql = "INSERT INTO DATABASE_CONNECTION (CONNECTION_ID, DB_TYPE, HOST_IP, PORT, "
					+ "DATABASE_NAME, USERNAME, PASSWORD, JDBC_DRIVER_FILE, CONNECTION_POOL_SETTINGS, "
					+ "CONNECTION_TIMEOUT, QUERY_TIMEOUT, MAX_POOL_SIZE, MIN_POOL_SIZE, CREATED_BY) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			jdbcTemplate.update(sql, connectionId, connectionData.get("DB_TYPE"), connectionData.get("HOST_IP"), connectionData.get("PORT"),
					connectionData.get("DATABASE_NAME"), connectionData.get("USERNAME"), connectionData.get("PASSWORD"),
					connectionData.get("JDBC_DRIVER_FILE"), connectionData.get("CONNECTION_POOL_SETTINGS"), connectionData.get("CONNECTION_TIMEOUT"),
					connectionData.get("QUERY_TIMEOUT"), connectionData.get("MAX_POOL_SIZE"), connectionData.get("MIN_POOL_SIZE"), userId);
		} else {
			// 기존 연결 수정
			String sql = "UPDATE DATABASE_CONNECTION SET DB_TYPE = ?, HOST_IP = ?, PORT = ?, "
					+ "DATABASE_NAME = ?, USERNAME = ?, PASSWORD = ?, JDBC_DRIVER_FILE = ?, "
					+ "CONNECTION_POOL_SETTINGS = ?, CONNECTION_TIMEOUT = ?, QUERY_TIMEOUT = ?, "
					+ "MAX_POOL_SIZE = ?, MIN_POOL_SIZE = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " + "WHERE CONNECTION_ID = ?";

			jdbcTemplate.update(sql, connectionData.get("DB_TYPE"), connectionData.get("HOST_IP"), connectionData.get("PORT"),
					connectionData.get("DATABASE_NAME"), connectionData.get("USERNAME"), connectionData.get("PASSWORD"),
					connectionData.get("JDBC_DRIVER_FILE"), connectionData.get("CONNECTION_POOL_SETTINGS"), connectionData.get("CONNECTION_TIMEOUT"),
					connectionData.get("QUERY_TIMEOUT"), connectionData.get("MAX_POOL_SIZE"), connectionData.get("MIN_POOL_SIZE"), userId,
					connectionId);
		}

		return true;
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
		boolean isNew = connectionId == null || connectionId.trim().isEmpty();

		if (isNew) {
			// 새 연결 생성
			connectionId = "SFTP_" + System.currentTimeMillis();
			String sql = "INSERT INTO SFTP_CONNECTION (SFTP_CONNECTION_ID, HOST_IP, PORT, "
					+ "USERNAME, PASSWORD, PRIVATE_KEY_PATH, REMOTE_PATH, CONNECTION_TIMEOUT, CREATED_BY) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

			jdbcTemplate.update(sql, connectionId, connectionData.get("HOST_IP"), connectionData.get("PORT"), connectionData.get("USERNAME"),
					connectionData.get("PASSWORD"), connectionData.get("PRIVATE_KEY_PATH"), connectionData.get("REMOTE_PATH"),
					connectionData.get("CONNECTION_TIMEOUT"), userId);
		} else {
			// 기존 연결 수정
			String sql = "UPDATE SFTP_CONNECTION SET HOST_IP = ?, PORT = ?, " + "USERNAME = ?, PASSWORD = ?, PRIVATE_KEY_PATH = ?, REMOTE_PATH = ?, "
					+ "CONNECTION_TIMEOUT = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " + "WHERE SFTP_CONNECTION_ID = ?";

			jdbcTemplate.update(sql, connectionData.get("HOST_IP"), connectionData.get("PORT"), connectionData.get("USERNAME"),
					connectionData.get("PASSWORD"), connectionData.get("PRIVATE_KEY_PATH"), connectionData.get("REMOTE_PATH"),
					connectionData.get("CONNECTION_TIMEOUT"), userId, connectionId);
		}

		return true;
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
				String sql = "UPDATE DATABASE_CONNECTION SET STATUS = 'DELETED' WHERE CONNECTION_ID = ?";
				jdbcTemplate.update(sql, connectionId);
			} else {
				String sql = "UPDATE SFTP_CONNECTION SET STATUS = 'DELETED' WHERE SFTP_CONNECTION_ID = ?";
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
		if ("admin".equals(userId)) {
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
				connId = (String) conn.get("SFTP_CONNECTION_ID");
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
		String sql = "SELECT CONNECTION_ID FROM CONNECTION_PERMISSIONS WHERE GROUP_ID = ?";
		try {
			return jdbcTemplate.queryForList(sql, String.class, groupId);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
