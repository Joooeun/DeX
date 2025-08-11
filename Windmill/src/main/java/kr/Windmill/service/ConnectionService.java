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
            
            // 캐시된 연결 정보가 있으면 사용, 없으면 새로 생성
            String url;
            Properties prop;
            
            // 연결 이름으로 캐시된 ConnectionDTO 확인
            String connectionName = connConfig.get("DB"); // DB 이름을 연결 이름으로 사용
            ConnectionDTO cachedConnection = connectionPoolManager.getConnectionConfig(connectionName);
            
            if (cachedConnection != null) {
                // 캐시된 연결 정보 사용
                url = cachedConnection.getJdbc();
                prop = new Properties(cachedConnection.getProp());
            } else {
                // 새로 생성
                url = com.createJdbcUrl(
                    connConfig.get("DBTYPE"), 
                    connConfig.get("IP"), 
                    connConfig.get("PORT"), 
                    connConfig.get("DB")
                );
                prop = new Properties();
                prop.put("user", connConfig.get("USER"));
                prop.put("password", connConfig.get("PW"));
                prop.put("clientProgramName", "DeX");
            }
            
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
        Connection conn = null;
        
        try {
            String driver = com.getDriverByDbType(connConfig.get("DBTYPE"));
            
            // 캐시된 연결 정보가 있으면 사용, 없으면 새로 생성
            String url;
            Properties prop;
            
            // 연결 이름으로 캐시된 ConnectionDTO 확인
            String connectionName = connConfig.get("DB"); // DB 이름을 연결 이름으로 사용
            ConnectionDTO cachedConnection = connectionPoolManager.getConnectionConfig(connectionName);
            
            if (cachedConnection != null) {
                // 캐시된 연결 정보 사용
                url = cachedConnection.getJdbc();
                prop = new Properties(cachedConnection.getProp());
            } else {
                // 새로 생성
                url = com.createJdbcUrl(
                    connConfig.get("DBTYPE"), 
                    connConfig.get("IP"), 
                    connConfig.get("PORT"), 
                    connConfig.get("DB")
                );
                prop = new Properties();
                prop.put("user", connConfig.get("USER"));
                prop.put("password", connConfig.get("PW"));
                prop.put("clientProgramName", "DeX");
            }
            
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

            // 동적 드라이버 로딩을 사용한 연결 시도
            try {
                // JDBC 드라이버 파일 결정 (캐시된 정보 우선, 없으면 요청에서)
                String jdbcDriverFile = null;
                if (cachedConnection != null && cachedConnection.getJdbcDriverFile() != null) {
                    jdbcDriverFile = cachedConnection.getJdbcDriverFile();
                } else {
                    jdbcDriverFile = connConfig.get("JDBC_DRIVER_FILE");
                }
                
                if (jdbcDriverFile != null && !jdbcDriverFile.trim().isEmpty()) {
                    // 동적 드라이버 로딩 사용
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
                if (errorMessage.contains("Connection refused") || errorMessage.contains("No route to host")) {
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
            
            // 데이터베이스 타입에 맞는 연결 테스트 쿼리 실행
            String testQuery = com.getTestQueryByDbType(connConfig.get("DBTYPE"));
            try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
                stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // 연결 정보 로깅
                        logger.info("연결 테스트 성공 - DB: {}, User: {}, Query: {}", 
                            connConfig.get("DB"), connConfig.get("USER"), testQuery);
                    }
                }
            } catch (SQLException e) {
                result.put("success", false);
                result.put("error", "연결은 성공했지만 쿼리 실행에 실패했습니다: " + e.getMessage());
                result.put("errorType", "QUERY_ERROR");
                return result;
            }
            
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "예상치 못한 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "UNKNOWN_ERROR");
            result.put("originalError", e.getMessage());
            
            String errorDetails = String.format("Connection test failed for %s:%s (DB: %s, Type: %s, User: %s) - Error: %s", 
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
}
