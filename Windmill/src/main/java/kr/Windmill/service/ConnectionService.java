package kr.Windmill.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Service
public class ConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    
    private final Common com;
    private final Log cLog;
    private final Map<String, ConnectionStatusDTO> connectionStatusMap = new ConcurrentHashMap<>();
    private Thread monitoringThread;
    private volatile boolean isRunning = false;
    
    @Autowired
    public ConnectionService(Common common, Log log) {
        this.com = common;
        this.cLog = log;
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
            Map<String, String> connConfig = com.ConnectionConf(connectionName);
            
            // 5초 타임아웃으로 연결 테스트
            boolean isConnected = testConnection(connConfig);
            
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
                cLog.monitoringLog("CONNECTION_STATUS", "DB 연결 상태 확인 완료: " + connectionName + " - 연결됨");
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
     * 연결 설정을 기반으로 데이터베이스 연결을 테스트합니다.
     * @param connConfig 연결 설정 정보
     * @return 연결 성공 여부
     */
    public boolean testConnection(Map<String, String> connConfig) {
        Connection conn = null;
        try {
            String driver = getDriverByDbType(connConfig.get("DBTYPE"));
            String url = buildJdbcUrl(connConfig);
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
            String testQuery = getTestQueryByDbType(connConfig.get("DBTYPE"));
            try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
                stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
                stmt.executeQuery();
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed for " + connConfig.get("IP") + ":" + connConfig.get("PORT"));
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
     * 연결 이름으로 ConnectionDTO를 생성합니다.
     * @param connectionName 연결 이름
     * @return ConnectionDTO 객체
     * @throws IOException 설정 파일 읽기 실패 시
     */
    public ConnectionDTO createConnectionDTO(String connectionName) throws IOException {
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
        String jdbcUrl = buildJdbcUrl(connConfig);
        connectionDTO.setJdbc(jdbcUrl);
        
        // 드라이버 클래스 설정
        String driverClass = getDriverByDbType(connConfig.get("DBTYPE"));
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
    
    private String getDriverByDbType(String dbType) {
        switch (dbType.toUpperCase()) {
            case "ORACLE":
                return "oracle.jdbc.driver.OracleDriver";
            case "POSTGRESQL":
                return "org.postgresql.Driver";
            case "TIBERO":
                return "com.tmax.tibero.jdbc.TbDriver";
            case "DB2":
                return "com.ibm.db2.jcc.DB2Driver";
            case "MYSQL":
                return "com.mysql.jdbc.Driver";
            default:
                return "oracle.jdbc.driver.OracleDriver";
        }
    }
    
    private String getTestQueryByDbType(String dbType) {
        switch (dbType.toUpperCase()) {
            case "ORACLE":
                return "SELECT 1 FROM DUAL";
            case "POSTGRESQL":
                return "SELECT 1";
            case "TIBERO":
                return "SELECT 1 FROM DUAL";
            case "DB2":
                return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
            case "MYSQL":
                return "SELECT 1";
            default:
                return "SELECT 1 FROM DUAL";
        }
    }
    
    private String buildJdbcUrl(Map<String, String> connConfig) {
        String dbType = connConfig.get("DBTYPE").toUpperCase();
        String ip = connConfig.get("IP");
        String port = connConfig.get("PORT");
        String db = connConfig.get("DB");

        switch (dbType) {
            case "ORACLE":
                return "jdbc:oracle:thin:@" + ip + ":" + port + ":" + db;
            case "POSTGRESQL":
                return "jdbc:postgresql://" + ip + ":" + port + "/" + db;
            case "TIBERO":
                return "jdbc:tibero:thin:@" + ip + ":" + port + ":" + db;
            case "DB2":
                return "jdbc:db2://" + ip + ":" + port + "/" + db;
            case "MYSQL":
                return "jdbc:mysql://" + ip + ":" + port + "/" + db;
            default:
                return "jdbc:oracle:thin:@" + ip + ":" + port + ":" + db;
        }
    }
    
    /**
     * JDBC URL을 생성합니다.
     * @param dbtype 데이터베이스 타입
     * @param ip IP 주소
     * @param port 포트 번호
     * @param db 데이터베이스명
     * @return JDBC URL
     */
    public String createJdbcUrl(String dbtype, String ip, String port, String db) {
        return com.createJdbcUrl(dbtype, ip, port, db);
    }
    
    /**
     * 드라이버 정보를 추출합니다.
     * @param driverFileName 드라이버 파일명
     * @return 드라이버 정보 맵
     */
    public Map<String, String> extractDriverInfo(String driverFileName) {
        return com.extractDriverInfo(driverFileName);
    }
}
