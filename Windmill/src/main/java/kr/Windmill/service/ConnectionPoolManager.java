package kr.Windmill.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kr.Windmill.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;

/**
 * 연결별 독립적인 DataSource 관리 시스템
 * 각 데이터베이스 연결에 대해 별도의 연결 풀을 생성하고 관리
 */
@Service
public class ConnectionPoolManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);
    
    private final Common common;
    
    @Autowired
    public ConnectionPoolManager(Common common) {
        this.common = common;
    }
    
    // 연결별 DataSource 관리
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    
    // 연결별 설정 정보 관리
    private final Map<String, ConnectionDTO> connectionConfigMap = new ConcurrentHashMap<>();
    
    /**
     * 특정 연결에 대한 DataSource 가져오기
     * @param connectionId 연결 ID
     * @return DataSource 객체
     */
    public DataSource getDataSource(String connectionId) {
        return dataSourceMap.computeIfAbsent(connectionId, this::createDataSource);
    }
    
    /**
     * 연결별 독립적인 DataSource 생성
     * @param connectionId 연결 ID
     * @return 새로 생성된 DataSource
     */
    private DataSource createDataSource(String connectionId) {
        try {
            logger.info("DataSource 생성 시작: {}", connectionId);
            
            // 연결 설정 가져오기
            ConnectionDTO connection = common.getConnection(connectionId);
            connectionConfigMap.put(connectionId, connection);
            
            // HikariCP 설정
            HikariConfig config = new HikariConfig();
            
            // 기본 연결 정보 설정
            config.setDriverClassName(connection.getDriver());
            config.setJdbcUrl(connection.getJdbc());
            config.setUsername(connection.getProp().getProperty("user"));
            config.setPassword(connection.getProp().getProperty("password"));
            
            // 연결별 고유 설정
            config.setPoolName("HikariCP-" + connectionId);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30초
            config.setIdleTimeout(600000); // 10분
            config.setMaxLifetime(1800000); // 30분
            
            // 데이터베이스별 특화 설정
            configureDatabaseSpecificSettings(config, connection);
            
            // 추가 속성 설정
            Properties additionalProps = connection.getProp();
            for (String key : additionalProps.stringPropertyNames()) {
                if (!key.equals("user") && !key.equals("password")) {
                    config.addDataSourceProperty(key, additionalProps.getProperty(key));
                }
            }
            
            // DataSource 생성
            HikariDataSource dataSource = new HikariDataSource(config);
            
            logger.info("DataSource 생성 완료: {} (최대 연결: {})", connectionId, config.getMaximumPoolSize());
            
            return dataSource;
            
        } catch (Exception e) {
            logger.error("DataSource 생성 실패: {}", connectionId, e);
            throw new RuntimeException("DataSource 생성 실패: " + connectionId, e);
        }
    }
    
    /**
     * 데이터베이스별 특화 설정
     */
    private void configureDatabaseSpecificSettings(HikariConfig config, ConnectionDTO connection) {
        String dbType = connection.getDbtype().toUpperCase();
        
        switch (dbType) {
            case "DB2":
                // DB2 특화 설정
                config.addDataSourceProperty("currentSchema", "DEX");
                config.addDataSourceProperty("retrieveMessagesFromServerOnGetMessage", "false");
                config.addDataSourceProperty("blockingReadConnectionTimeout", "5");
                config.addDataSourceProperty("blockingReadConnectionTimeoutUnit", "SECONDS");
                config.addDataSourceProperty("loginTimeout", "5");
                break;
                
            case "ORACLE":
                // Oracle 특화 설정
                config.addDataSourceProperty("oracle.jdbc.timezoneAsRegion", "false");
                config.addDataSourceProperty("oracle.jdbc.fanEnabled", "false");
                break;
                
            case "POSTGRESQL":
                // PostgreSQL 특화 설정
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                break;
                
            case "TIBERO":
                // Tibero 특화 설정
                config.addDataSourceProperty("tibero.defaultRowPrefetch", "50");
                break;
                
            default:
                logger.warn("알 수 없는 데이터베이스 타입: {}", dbType);
                break;
        }
    }
    
    /**
     * 특정 연결의 DataSource 제거
     * @param connectionId 연결 ID
     */
    public void removeDataSource(String connectionId) {
        DataSource dataSource = dataSourceMap.remove(connectionId);
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            hikariDataSource.close();
            logger.info("DataSource 제거 완료: {}", connectionId);
        }
        
        connectionConfigMap.remove(connectionId);
    }
    
    /**
     * 모든 DataSource 제거
     */
    @PreDestroy
    public void cleanup() {
        logger.info("모든 DataSource 정리 시작");
        
        for (String connectionId : dataSourceMap.keySet()) {
            removeDataSource(connectionId);
        }
        
        dataSourceMap.clear();
        connectionConfigMap.clear();
        
        logger.info("모든 DataSource 정리 완료");
    }
    
    /**
     * 연결 풀 상태 정보 조회
     */
    public Map<String, Object> getPoolStatus(String connectionId) {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        DataSource dataSource = dataSourceMap.get(connectionId);
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            status.put("connectionId", connectionId);
            status.put("poolName", hikariDataSource.getPoolName());
            status.put("maximumPoolSize", hikariDataSource.getHikariConfigMXBean().getMaximumPoolSize());
            status.put("minimumIdle", hikariDataSource.getHikariConfigMXBean().getMinimumIdle());
            status.put("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections());
            status.put("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections());
            status.put("totalConnections", hikariDataSource.getHikariPoolMXBean().getTotalConnections());
        }
        
        return status;
    }
    
    /**
     * 모든 연결 풀 상태 정보 조회
     */
    public Map<String, Map<String, Object>> getAllPoolStatus() {
        Map<String, Map<String, Object>> allStatus = new ConcurrentHashMap<>();
        
        for (String connectionId : dataSourceMap.keySet()) {
            allStatus.put(connectionId, getPoolStatus(connectionId));
        }
        
        return allStatus;
    }
    
    /**
     * 연결 설정 정보 조회
     */
    public ConnectionDTO getConnectionConfig(String connectionId) {
        return connectionConfigMap.get(connectionId);
    }
    
    /**
     * 연결 풀 존재 여부 확인
     */
    public boolean hasDataSource(String connectionId) {
        return dataSourceMap.containsKey(connectionId);
    }
    
    /**
     * 연결 풀 개수 조회
     */
    public int getDataSourceCount() {
        return dataSourceMap.size();
    }
}
