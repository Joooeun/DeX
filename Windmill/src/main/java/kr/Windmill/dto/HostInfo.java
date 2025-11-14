package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 호스트 정보 DTO
 */
public class HostInfo {
    
    @JsonProperty("hostId")
    private String hostId;
    
    @JsonProperty("hostName")
    private String hostName;
    
    @JsonProperty("hostIp")
    private String hostIp;
    
    @JsonProperty("port")
    private Integer port;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("status")
    private String status;
    
    // 기본 생성자
    public HostInfo() {}
    
    // 생성자
    public HostInfo(String hostId, String hostName, String hostIp, Integer port, String username, String status) {
        this.hostId = hostId;
        this.hostName = hostName;
        this.hostIp = hostIp;
        this.port = port;
        this.username = username;
        this.status = status;
    }
    
    // Getter/Setter
    public String getHostId() {
        return hostId;
    }
    
    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "HostInfo{" +
                "hostId='" + hostId + '\'' +
                ", hostName='" + hostName + '\'' +
                ", hostIp='" + hostIp + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
