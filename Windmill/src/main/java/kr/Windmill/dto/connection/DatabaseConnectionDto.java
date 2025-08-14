package kr.Windmill.dto.connection;

import java.time.LocalDateTime;

/**
 * 데이터베이스 연결 Dto 클래스
 */
public class DatabaseConnectionDto {
    
    private String connectionId;
    private String dbType;
    private String hostIp;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String jdbcDriverFile;
    private String testSql;
    private String connectionPoolSettings;
    private Integer connectionTimeout;
    private Integer queryTimeout;
    private Integer maxPoolSize;
    private Integer minPoolSize;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    
    // 생성자
    public DatabaseConnectionDto() {}
    
    public DatabaseConnectionDto(String connectionId, String dbType, String hostIp) {
        this.connectionId = connectionId;
        this.dbType = dbType;
        this.hostIp = hostIp;
    }
    
    // Getter/Setter
    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getDbType() {
        return dbType;
    }
    
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
    
    public String getHostIp() {
        return hostIp;
    }
    
    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getJdbcDriverFile() {
        return jdbcDriverFile;
    }
    
    public void setJdbcDriverFile(String jdbcDriverFile) {
        this.jdbcDriverFile = jdbcDriverFile;
    }
    
    public String getTestSql() {
        return testSql;
    }
    
    public void setTestSql(String testSql) {
        this.testSql = testSql;
    }
    
    public String getConnectionPoolSettings() {
        return connectionPoolSettings;
    }
    
    public void setConnectionPoolSettings(String connectionPoolSettings) {
        this.connectionPoolSettings = connectionPoolSettings;
    }
    
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Integer getQueryTimeout() {
        return queryTimeout;
    }
    
    public void setQueryTimeout(Integer queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
    
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public Integer getMinPoolSize() {
        return minPoolSize;
    }
    
    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getModifiedTimestamp() {
        return modifiedTimestamp;
    }
    
    public void setModifiedTimestamp(LocalDateTime modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }
    
    public String getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    @Override
    public String toString() {
        return "DatabaseConnectionDto{" +
                "connectionId='" + connectionId + '\'' +
                ", dbType='" + dbType + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
