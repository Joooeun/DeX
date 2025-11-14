package kr.Windmill.dto.system;

/**
 * DeX 시스템 상태 Dto 클래스
 */
public class DexStatusDto {
    
    private String statusName;
    private String displayName;
    private String status;
    private String color;
    private String message;
    private String lastChecked;
    private String detailInfo;  // 상세 정보 추가
    private double cpuUsage;    // CPU 사용률 (0-100)
    private double memoryUsage; // 메모리 사용률 (0-100)
    private String pid;         // 프로세스 ID
    
    // 생성자
    public DexStatusDto() {}
    
    public DexStatusDto(String statusName, String displayName, String status, String color, String message) {
        this.statusName = statusName;
        this.displayName = displayName;
        this.status = status;
        this.color = color;
        this.message = message;
        this.detailInfo = "";
        this.cpuUsage = 0.0;
        this.memoryUsage = 0.0;
        this.pid = "";
    }
    
    // Getter/Setter
    public String getStatusName() {
        return statusName;
    }
    
    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(String lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public String getDetailInfo() {
        return detailInfo;
    }
    
    public void setDetailInfo(String detailInfo) {
        this.detailInfo = detailInfo;
    }
    
    public double getCpuUsage() {
        return cpuUsage;
    }
    
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
    
    public double getMemoryUsage() {
        return memoryUsage;
    }
    
    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    
    public String getPid() {
        return pid;
    }
    
    public void setPid(String pid) {
        this.pid = pid;
    }
    
    @Override
    public String toString() {
        return "DexStatusDto{" +
                "statusName='" + statusName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", color='" + color + '\'' +
                ", message='" + message + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", memoryUsage=" + memoryUsage +
                '}';
    }
}
