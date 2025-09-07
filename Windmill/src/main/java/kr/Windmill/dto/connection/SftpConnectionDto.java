package kr.Windmill.dto.connection;

import java.time.LocalDateTime;

/**
 * SFTP 연결 Dto 클래스
 */
public class SftpConnectionDto {
    
    private String sftpConnectionId;
    private String hostIp;
    private Integer port;
    private String username;
    private String password;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    
    // 생성자
    public SftpConnectionDto() {}
    
    public SftpConnectionDto(String sftpConnectionId, String hostIp, String username) {
        this.sftpConnectionId = sftpConnectionId;
        this.hostIp = hostIp;
        this.username = username;
    }
    
    // Getter/Setter
    public String getSftpConnectionId() {
        return sftpConnectionId;
    }
    
    public void setSftpConnectionId(String sftpConnectionId) {
        this.sftpConnectionId = sftpConnectionId;
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
        return "SftpConnectionDto{" +
                "sftpConnectionId='" + sftpConnectionId + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
