package kr.Windmill.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.Windmill.util.Common;
import kr.Windmill.util.Log;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    
    private final Common com;
    private final Log cLog;
    private final ConnectionPoolManager connectionPoolManager;
    private final Map<String, ConnectionStatusDTO> connectionStatusMap = new ConcurrentHashMap<>();
    private Thread monitoringThread;
    private volatile boolean isRunning = false;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public ConnectionService(Common common, Log log, ConnectionPoolManager connectionPoolManager) {
        this.com = common;
        this.cLog = log;
        this.connectionPoolManager = connectionPoolManager;
    }
    
    // ==================== 연결 상태 모니터링 ====================
    
    @PostConstruct
    public void startMonitoring() {
        cLog.monitoringLog("CONNECTION_STATUS", "Connection status monitoring started");
        isRunning = true;
        
        // 연결 목록을 미리 확인중 상태로 초기화
        try {
            List<String> connectionList = com.ConnectionnList("DB");
            cLog.monitoringLog("CONNECTION_STATUS", "초기 연결 목록 로드: " + connectionList);
            
            for (String connection : connectionList) {
                String connectionName = connection.split("\\.")[0];
                ConnectionStatusDTO status = new ConnectionStatusDTO(
                    connectionName,
                    "checking",  // 확인중 상태
                    "#ffc107"    // 노란색
                );
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                connectionStatusMap.put(connectionName, status);
                cLog.monitoringLog("CONNECTION_STATUS", "연결 상태 초기화: " + connectionName + " - 확인중");
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
        isRunning = false;
        
        if (monitoringThread != null && monitoringThread.isAlive()) {
            monitoringThread.interrupt();
            try {
                monitoringThread.join(3000);
                if (monitoringThread.isAlive()) {
                    cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드가 3초 내에 종료되지 않았습니다. 강제 종료합니다.");
                    monitoringThread.interrupt();
                    monitoringThread.join(1000);
                } else {
                    cLog.monitoringLog("CONNECTION_STATUS", "모니터링 스레드가 정상적으로 종료되었습니다.");
                }
            } catch (InterruptedException e) {
                cLog.monitoringLog("CONNECTION_STATUS_WARN", "모니터링 스레드 종료 대기 중 인터럽트 발생");
                Thread.currentThread().interrupt();
            }
        }
        
        connectionStatusMap.clear();
        cLog.monitoringLog("CONNECTION_STATUS", "Connection status monitoring stopped");
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
            
            List<String> connectionList = com.ConnectionnList("DB");
            cLog.monitoringLog("CONNECTION_STATUS", "발견된 연결 목록: " + connectionList);
            
            for (String connection : connectionList) {
                String connectionName = connection.split("\\.")[0];
                cLog.monitoringLog("CONNECTION_STATUS", "연결 상태 확인 중: " + connectionName);
                updateConnectionStatus(connectionName);
            }
        } catch (Exception e) {
            cLog.monitoringLog("CONNECTION_STATUS_ERROR", "연결 목록 조회 중 오류 발생: " + e.getMessage());
        }
    }
    
    private void updateConnectionStatus(String connectionName) {
        try {
            // ConnectionPoolManager를 사용하여 연결 설정 가져오기 (캐싱됨)
            ConnectionDTO connection = connectionPoolManager.getConnectionConfig(connectionName);
            
            if (connection == null) {
                // 캐시에 없으면 새로 생성하고 캐시에 저장
                connection = createConnectionDTO(connectionName);
                connectionPoolManager.getDataSource(connectionName); // DataSource 생성 및 캐싱
            }
            
            // 캐시된 연결 정보를 직접 사용하여 테스트
            boolean isConnected = testConnectionWithConnectionDTO(connection);
            
            // 기존 상태가 있으면 업데이트, 없으면 새로 생성
            ConnectionStatusDTO status = connectionStatusMap.get(connectionName);
            if (status == null) {
                status = new ConnectionStatusDTO(
                    connectionName,
                    isConnected ? "connected" : "disconnected",
                    isConnected ? "#28a745" : "#dc3545"
                );
            } else {
                status.setStatus(isConnected ? "connected" : "disconnected");
                status.setColor(isConnected ? "#28a745" : "#dc3545");
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            connectionStatusMap.put(connectionName, status);
            
            if (isConnected) {
                // 성공한 연결은 로그 레벨을 낮춤 (DEBUG)
                logger.debug("DB 연결 상태 확인 완료: {} - 연결됨", connectionName);
            } else {
                cLog.monitoringLog("CONNECTION_STATUS_WARN", "DB 연결 상태 확인 완료: " + connectionName + " - 연결실패");
            }
            
        } catch (Exception e) {
            ConnectionStatusDTO status = connectionStatusMap.get(connectionName);
            if (status == null) {
                status = new ConnectionStatusDTO(
                    connectionName,
                    "error",
                    "#dc3545"
                );
            } else {
                status.setStatus("error");
                status.setColor("#dc3545");
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            status.setErrorMessage(e.getMessage());
            connectionStatusMap.put(connectionName, status);
            cLog.monitoringLog("CONNECTION_STATUS_ERROR", "DB 연결 상태 확인 완료: " + connectionName + " - 오류발생: " + e.getMessage());
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
            
            Map<String, String> userConfig = com.UserConf(userId);
            List<String> allowedConnections = Arrays.asList(userConfig.get("CONNECTION").split(","));
            
            return connectionStatusMap.values().stream()
                .filter(status -> allowedConnections.contains(status.getConnectionName()))
                .collect(Collectors.toList());
                
        } catch (IOException e) {
            cLog.monitoringLog("CONNECTION_STATUS_ERROR", "Error getting user configuration for " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public ConnectionStatusDTO getConnectionStatus(String connectionName) {
        return connectionStatusMap.get(connectionName);
    }
    
    // 수동으로 특정 연결 상태 업데이트
    public void updateConnectionStatusManually(String connectionName) {
        cLog.monitoringLog("CONNECTION_STATUS_MANUAL", "수동 DB 연결 상태 확인 요청: " + connectionName);
        updateConnectionStatus(connectionName);
        cLog.monitoringLog("CONNECTION_STATUS_MANUAL", "수동 DB 연결 상태 확인 완료: " + connectionName);
    }
    
    // ==================== 연결 테스트 및 생성 ====================
    
    /**
     * ConnectionDTO를 기반으로 데이터베이스 연결을 테스트합니다. (캐시 우선)
     * @param connection 캐시된 연결 정보
     * @return 연결 성공 여부
     */
    public boolean testConnectionWithConnectionDTO(ConnectionDTO connection) {
        Connection conn = null;
        try {
            // 캐시된 DataSource 사용 (이미 동적 드라이버 로딩이 적용됨)
            String connectionName = connection.getDbName(); // DB 이름을 연결 이름으로 사용
            if (connectionPoolManager.hasDataSource(connectionName)) {
                // 캐시된 DataSource에서 연결 가져오기
                DataSource dataSource = connectionPoolManager.getDataSource(connectionName);
                conn = dataSource.getConnection();
            } else {
                // DataSource가 없으면 기존 방식 사용 (최초 1회만)
                String driver = connection.getDriver();
                String url = connection.getJdbc();
                Properties prop = new Properties(connection.getProp());
                
                // 타임아웃 설정 추가
                prop.put("connectTimeout", "5000");  // 5초 연결 타임아웃
                prop.put("socketTimeout", "5000");   // 5초 소켓 타임아웃
                
                // DB2 전용 타임아웃 속성 추가
                if ("DB2".equalsIgnoreCase(connection.getDbtype())) {
                    prop.put("loginTimeout", "5");           // 로그인 타임아웃 5초
                    prop.put("blockingReadConnectionTimeout", "5");  // 읽기 타임아웃 5초
                    prop.put("blockingReadConnectionTimeoutUnit", "SECONDS");
                    prop.put("currentSchema", "DEX");        // 스키마 설정
                    prop.put("retrieveMessagesFromServerOnGetMessage", "false"); // 메시지 검색 비활성화
                }

                // 동적 드라이버 로딩 사용 (최초 1회만)
                if (connection.getJdbcDriverFile() != null && !connection.getJdbcDriverFile().trim().isEmpty()) {
                    conn = com.createConnectionWithDynamicDriver(url, prop, connection.getJdbcDriverFile(), driver);
                } else {
                    // 기본 드라이버 사용 (클래스패스에서)
                    try {
                        Class.forName(driver);
                        conn = DriverManager.getConnection(url, prop);
                    } catch (ClassNotFoundException e) {
                        logger.error("드라이버를 찾을 수 없습니다: {}", driver);
                        throw e;
                    }
                }
            }
            
            // 데이터베이스 타입에 맞는 연결 테스트 쿼리 실행
            String testQuery = com.getTestQueryByDbType(connection.getDbtype());
            try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
                stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
                stmt.executeQuery();
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed for " + connection.getJdbc());
            return false;
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
     * 연결 설정을 기반으로 데이터베이스 연결을 테스트합니다.
     * @param connConfig 연결 설정 정보
     * @return 연결 성공 여부
     */
    public boolean testConnection(Map<String, String> connConfig) {
        Connection conn = null;
        try {
            String driver = com.getDriverByDbType(connConfig.get("DBTYPE"));
            
            // 연결 테스트는 항상 새로운 연결 정보로 생성 (캐시 사용 안함)
            String url = com.createJdbcUrl(
                connConfig.get("DBTYPE"), 
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("DB")
            );
            Properties prop = new Properties();
            prop.put("user", connConfig.get("USER"));
            prop.put("password", connConfig.get("PW"));
            prop.put("clientProgramName", "DeX");
            
            // 타임아웃 설정 추가
            prop.put("connectTimeout", "5000");  // 5초 연결 타임아웃
            prop.put("socketTimeout", "5000");   // 5초 소켓 타임아웃
            
            // DB2 전용 타임아웃 속성 추가
            if ("DB2".equalsIgnoreCase(connConfig.get("DBTYPE"))) {
                prop.put("loginTimeout", "5");           // 로그인 타임아웃 5초
                prop.put("blockingReadConnectionTimeout", "5");  // 읽기 타임아웃 5초
                prop.put("blockingReadConnectionTimeoutUnit", "SECONDS");
                prop.put("currentSchema", "DEX");        // 스키마 설정
                prop.put("retrieveMessagesFromServerOnGetMessage", "false"); // 메시지 검색 비활성화
            }

            Class.forName(driver);
            conn = DriverManager.getConnection(url, prop);
            
            // 데이터베이스 타입에 맞는 연결 테스트 쿼리 실행
            String testQuery = com.getTestQueryByDbType(connConfig.get("DBTYPE"));
            try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
                stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
                stmt.executeQuery();
            }
            
            return true;
        } catch (Exception e) {
            String errorDetails = String.format("Connection test failed for %s:%s (DB: %s, Type: %s, User: %s) - Error: %s", 
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("DB"), 
                connConfig.get("DBTYPE"), 
                connConfig.get("USER"), 
                e.getMessage());
            
            logger.error(errorDetails, e);
            return false;
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
     * 연결 테스트를 수행하고 상세한 오류 정보를 반환합니다.
     * @param connConfig 연결 설정 정보
     * @return 성공/실패 여부와 상세 오류 정보를 포함한 Map
     */
    public Map<String, Object> testConnectionWithDetails(Map<String, String> connConfig) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 연결 타입 확인
            String connectionType = connConfig.get("TYPE");
            
            if ("HOST".equals(connectionType)) {
                // SFTP 연결 테스트
                return testSftpConnection(connConfig);
            } else {
                // DB 연결 테스트
                return testDatabaseConnection(connConfig);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "예상치 못한 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "UNKNOWN_ERROR");
            result.put("originalError", e.getMessage());
            
            String errorDetails = String.format("Connection test failed for %s:%s (Type: %s, User: %s) - Error: %s", 
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("TYPE"), 
                connConfig.get("USER"), 
                e.getMessage());
            
            logger.error(errorDetails, e);
            return result;
        }
    }
    
    /**
     * 데이터베이스 연결을 테스트합니다.
     * @param connConfig 연결 설정 정보
     * @return 테스트 결과
     */
    private Map<String, Object> testDatabaseConnection(Map<String, String> connConfig) {
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
            String url = com.createJdbcUrl(
                dbType, 
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("DB")
            );
            Properties prop = new Properties();
            prop.put("user", connConfig.get("USER"));
            prop.put("password", connConfig.get("PW"));
            prop.put("clientProgramName", "DeX");
            
            // 타임아웃 설정 추가
            prop.put("connectTimeout", "5000");  // 5초 연결 타임아웃
            prop.put("socketTimeout", "5000");   // 5초 소켓 타임아웃
            
            // DB2 전용 타임아웃 속성 추가
            if ("DB2".equalsIgnoreCase(dbType)) {
                prop.put("loginTimeout", "5");           // 로그인 타임아웃 5초
                prop.put("blockingReadConnectionTimeout", "5");  // 읽기 타임아웃 5초
                prop.put("blockingReadConnectionTimeoutUnit", "SECONDS");
                prop.put("currentSchema", "DEX");        // 스키마 설정
                prop.put("retrieveMessagesFromServerOnGetMessage", "false"); // 메시지 검색 비활성화
            }

            // JDBC 드라이버 파일 검증 및 연결 시도
            try {
                String jdbcDriverFile = connConfig.get("JDBC_DRIVER_FILE");
                
                if (jdbcDriverFile != null && !jdbcDriverFile.trim().isEmpty()) {
                    // JDBC 드라이버 파일 존재 여부 확인
                    String jdbcFilePath = com.JdbcPath + jdbcDriverFile.trim();
                    java.io.File jdbcFile = new java.io.File(jdbcFilePath);
                    if (!jdbcFile.exists()) {
                        result.put("success", false);
                        result.put("error", "선택한 JDBC 드라이버 파일을 찾을 수 없습니다: " + jdbcDriverFile);
                        result.put("errorType", "DRIVER_FILE_NOT_FOUND");
                        return result;
                    }
                    
                    // 동적 드라이버 로딩 사용 (실패 시 폴백 없이 오류 발생)
                    conn = com.createConnectionWithDynamicDriver(url, prop, jdbcDriverFile, driver);
                } else {
                    // 기본 드라이버 사용 (클래스패스에서)
                    try {
                        Class.forName(driver);
                        conn = DriverManager.getConnection(url, prop);
                    } catch (ClassNotFoundException e) {
                        result.put("success", false);
                        result.put("error", "JDBC 드라이버를 찾을 수 없습니다: " + driver);
                        result.put("errorType", "DRIVER_NOT_FOUND");
                        return result;
                    }
                }
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
                        logger.info("DB 연결 테스트 성공 - DB: {}, User: {}, Query: {}", 
                            connConfig.get("DB"), connConfig.get("USER"), testQuery);
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
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("DB"), 
                connConfig.get("DBTYPE"), 
                connConfig.get("USER"), 
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
            
            String errorDetails = String.format("SFTP connection test failed for %s:%s (User: %s) - Error: %s", 
                connConfig.get("IP"), 
                connConfig.get("PORT"), 
                connConfig.get("USER"), 
                e.getMessage());
            
            logger.error(errorDetails, e);
            return result;
        }
    }
    
    /**
     * 연결 이름으로 ConnectionDTO를 생성합니다. (캐시 우선)
     * @param connectionName 연결 이름
     * @return ConnectionDTO 객체
     * @throws IOException 설정 파일 읽기 실패 시
     */
    public ConnectionDTO createConnectionDTO(String connectionName) throws IOException {
        // 먼저 캐시에서 확인
        ConnectionDTO cachedConnection = connectionPoolManager.getConnectionConfig(connectionName);
        if (cachedConnection != null) {
            return cachedConnection;
        }
        
        // 캐시에 없으면 새로 생성
        Map<String, String> connConfig = com.ConnectionConf(connectionName);
        
        ConnectionDTO connectionDTO = new ConnectionDTO();
        connectionDTO.setDbtype(connConfig.get("DBTYPE"));
        connectionDTO.setDbName(connConfig.get("DB"));
        
        // Properties 설정
        Properties prop = new Properties();
        prop.put("user", connConfig.get("USER"));
        prop.put("password", connConfig.get("PW"));
        prop.put("clientProgramName", "DeX");
        connectionDTO.setProp(prop);
        
        // JDBC URL 생성
        String jdbcUrl = com.createJdbcUrl(
            connConfig.get("DBTYPE"), 
            connConfig.get("IP"), 
            connConfig.get("PORT"), 
            connConfig.get("DB")
        );
        connectionDTO.setJdbc(jdbcUrl);
        
        // 드라이버 클래스 설정
        String driverClass = com.getDriverByDbType(connConfig.get("DBTYPE"));
        connectionDTO.setDriver(driverClass);
        
        return connectionDTO;
    }
    
    /**
     * 동적 드라이버를 사용하여 연결을 생성합니다.
     * @param jdbcUrl JDBC URL
     * @param prop 연결 속성
     * @param jdbcDriverFile JDBC 드라이버 파일명
     * @param driverClass 드라이버 클래스명
     * @return 데이터베이스 연결
     * @throws Exception 연결 생성 실패 시
     */
    public Connection createConnectionWithDynamicDriver(String jdbcUrl, Properties prop, String jdbcDriverFile, String driverClass) throws Exception {
        return com.createConnectionWithDynamicDriver(jdbcUrl, prop, jdbcDriverFile, driverClass);
    }
    
    // ==================== 유틸리티 메서드 ====================
    

    
    /**
     * 드라이버 정보를 추출합니다.
     * @param driverFileName 드라이버 파일명
     * @return 드라이버 정보 맵
     */
    public Map<String, String> extractDriverInfo(String driverFileName) {
        return com.extractDriverInfo(driverFileName);
    }
    
    /**
     * 연결 캐시에서 특정 연결을 제거합니다.
     * @param connectionName 연결 이름
     */
    public void removeConnectionFromCache(String connectionName) {
        // ConnectionPoolManager의 캐시에서 제거
        connectionPoolManager.removeDataSource(connectionName);
        
        // ConnectionStatusMap에서도 제거
        connectionStatusMap.remove(connectionName);
        
        logger.info("연결 캐시에서 제거됨: {}", connectionName);
    }
    
    /**
     * ConnectionPoolManager 인스턴스를 반환합니다.
     * @return ConnectionPoolManager 인스턴스
     */
    public ConnectionPoolManager getConnectionPoolManager() {
        return connectionPoolManager;
    }
    
    // ==================== DB 기반 연결 관리 ====================
    
    /**
     * 모든 연결 목록을 조회합니다 (DB + SFTP)
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
     * @param userId 사용자 ID
     * @param searchKeyword 검색 키워드
     * @param typeFilter 타입 필터
     * @param page 현재 페이지
     * @param pageSize 페이지 크기
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
     * @param connections 전체 연결 목록
     * @param searchKeyword 검색 키워드
     * @param typeFilter 타입 필터
     * @return 필터링된 연결 목록
     */
    private List<Map<String, Object>> filterConnections(List<Map<String, Object>> connections, String searchKeyword, String typeFilter) {
        return connections.stream()
            .filter(conn -> {
                // 타입 필터 적용
                if (typeFilter != null && !typeFilter.isEmpty()) {
                    String type = (String) conn.get("TYPE");
                    if (!typeFilter.equals(type)) {
                        return false;
                    }
                }
                
                // 검색 키워드 적용
                if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                    String connectionName = (String) conn.get("CONNECTION_NAME");
                    String hostIp = (String) conn.get("HOST_IP");
                    
                    boolean matchesName = connectionName != null && connectionName.toLowerCase().contains(searchKeyword.toLowerCase());
                    boolean matchesIp = hostIp != null && hostIp.toLowerCase().contains(searchKeyword.toLowerCase());
                    
                    if (!matchesName && !matchesIp) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 데이터베이스 연결 목록을 조회합니다
     * @param userId 사용자 ID
     * @return DB 연결 목록
     */
    public List<Map<String, Object>> getDatabaseConnections(String userId) {
        String sql = "SELECT * FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_NAME";
        List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);
        
        // 권한 필터링 적용
        return filterConnectionsByPermission(userId, connections, "DATABASE");
    }
    
    /**
     * SFTP 연결 목록을 조회합니다
     * @param userId 사용자 ID
     * @return SFTP 연결 목록
     */
    public List<Map<String, Object>> getSftpConnections(String userId) {
        String sql = "SELECT * FROM SFTP_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_NAME";
        List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);
        
        // 권한 필터링 적용
        return filterConnectionsByPermission(userId, connections, "SFTP");
    }
    
    /**
     * 연결 상세 정보를 조회합니다
     * @param connectionId 연결 ID
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
     * @param connectionData 연결 데이터
     * @param userId 사용자 ID
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
     * @param connectionData 연결 데이터
     * @param userId 사용자 ID
     * @return 저장 결과
     */
    private boolean saveDatabaseConnection(Map<String, Object> connectionData, String userId) {
        String connectionId = (String) connectionData.get("CONNECTION_ID");
        boolean isNew = connectionId == null || connectionId.trim().isEmpty();
        
        if (isNew) {
            // 새 연결 생성
            connectionId = "DB_" + System.currentTimeMillis();
            String sql = "INSERT INTO DATABASE_CONNECTION (CONNECTION_ID, CONNECTION_NAME, DB_TYPE, HOST_IP, PORT, " +
                        "DATABASE_NAME, USERNAME, PASSWORD, JDBC_DRIVER_FILE, CONNECTION_POOL_SETTINGS, " +
                        "CONNECTION_TIMEOUT, QUERY_TIMEOUT, MAX_POOL_SIZE, MIN_POOL_SIZE, CREATED_BY) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql,
                connectionId,
                connectionData.get("CONNECTION_NAME"),
                connectionData.get("DB_TYPE"),
                connectionData.get("HOST_IP"),
                connectionData.get("PORT"),
                connectionData.get("DATABASE_NAME"),
                connectionData.get("USERNAME"),
                connectionData.get("PASSWORD"),
                connectionData.get("JDBC_DRIVER_FILE"),
                connectionData.get("CONNECTION_POOL_SETTINGS"),
                connectionData.get("CONNECTION_TIMEOUT"),
                connectionData.get("QUERY_TIMEOUT"),
                connectionData.get("MAX_POOL_SIZE"),
                connectionData.get("MIN_POOL_SIZE"),
                userId
            );
        } else {
            // 기존 연결 수정
            String sql = "UPDATE DATABASE_CONNECTION SET CONNECTION_NAME = ?, DB_TYPE = ?, HOST_IP = ?, PORT = ?, " +
                        "DATABASE_NAME = ?, USERNAME = ?, PASSWORD = ?, JDBC_DRIVER_FILE = ?, " +
                        "CONNECTION_POOL_SETTINGS = ?, CONNECTION_TIMEOUT = ?, QUERY_TIMEOUT = ?, " +
                        "MAX_POOL_SIZE = ?, MIN_POOL_SIZE = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                        "WHERE CONNECTION_ID = ?";
            
            jdbcTemplate.update(sql,
                connectionData.get("CONNECTION_NAME"),
                connectionData.get("DB_TYPE"),
                connectionData.get("HOST_IP"),
                connectionData.get("PORT"),
                connectionData.get("DATABASE_NAME"),
                connectionData.get("USERNAME"),
                connectionData.get("PASSWORD"),
                connectionData.get("JDBC_DRIVER_FILE"),
                connectionData.get("CONNECTION_POOL_SETTINGS"),
                connectionData.get("CONNECTION_TIMEOUT"),
                connectionData.get("QUERY_TIMEOUT"),
                connectionData.get("MAX_POOL_SIZE"),
                connectionData.get("MIN_POOL_SIZE"),
                userId,
                connectionId
            );
        }
        
        return true;
    }
    
    /**
     * SFTP 연결을 저장합니다
     * @param connectionData 연결 데이터
     * @param userId 사용자 ID
     * @return 저장 결과
     */
    private boolean saveSftpConnection(Map<String, Object> connectionData, String userId) {
        String connectionId = (String) connectionData.get("CONNECTION_ID");
        boolean isNew = connectionId == null || connectionId.trim().isEmpty();
        
        if (isNew) {
            // 새 연결 생성
            connectionId = "SFTP_" + System.currentTimeMillis();
            String sql = "INSERT INTO SFTP_CONNECTION (SFTP_CONNECTION_ID, CONNECTION_NAME, HOST_IP, PORT, " +
                        "USERNAME, PASSWORD, PRIVATE_KEY_PATH, REMOTE_PATH, CONNECTION_TIMEOUT, CREATED_BY) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql,
                connectionId,
                connectionData.get("CONNECTION_NAME"),
                connectionData.get("HOST_IP"),
                connectionData.get("PORT"),
                connectionData.get("USERNAME"),
                connectionData.get("PASSWORD"),
                connectionData.get("PRIVATE_KEY_PATH"),
                connectionData.get("REMOTE_PATH"),
                connectionData.get("CONNECTION_TIMEOUT"),
                userId
            );
        } else {
            // 기존 연결 수정
            String sql = "UPDATE SFTP_CONNECTION SET CONNECTION_NAME = ?, HOST_IP = ?, PORT = ?, " +
                        "USERNAME = ?, PASSWORD = ?, PRIVATE_KEY_PATH = ?, REMOTE_PATH = ?, " +
                        "CONNECTION_TIMEOUT = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                        "WHERE SFTP_CONNECTION_ID = ?";
            
            jdbcTemplate.update(sql,
                connectionData.get("CONNECTION_NAME"),
                connectionData.get("HOST_IP"),
                connectionData.get("PORT"),
                connectionData.get("USERNAME"),
                connectionData.get("PASSWORD"),
                connectionData.get("PRIVATE_KEY_PATH"),
                connectionData.get("REMOTE_PATH"),
                connectionData.get("CONNECTION_TIMEOUT"),
                userId,
                connectionId
            );
        }
        
        return true;
    }
    
    /**
     * 연결을 삭제합니다
     * @param connectionId 연결 ID
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
     * @param userId 사용자 ID
     * @param connections 연결 목록
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
        return connections.stream()
            .filter(conn -> {
                String connId = (String) conn.get("CONNECTION_ID");
                return allowedConnections.contains(connId);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 그룹을 조회합니다
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
     * @param groupId 그룹 ID
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
