package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SQL 내용 DTO (연결별 SQL 내용)
 */
public class SqlContent {
    
    @JsonProperty("connectionId")
    private String connectionId;
    
    @JsonProperty("sqlContent")
    private String sqlContent;
    
    @JsonProperty("version")
    private Integer version = 1;
    
    // 기본 생성자
    public SqlContent() {}
    
    // Getters and Setters
    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getSqlContent() {
        return sqlContent;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "SqlContent{" +
                "connectionId='" + connectionId + '\'' +
                ", sqlContent='" + sqlContent + '\'' +
                ", version=" + version +
                '}';
    }
}
