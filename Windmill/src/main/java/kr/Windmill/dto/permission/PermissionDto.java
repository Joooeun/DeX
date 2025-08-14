package kr.Windmill.dto.permission;

import java.time.LocalDateTime;

/**
 * 권한 Dto 클래스
 */
public class PermissionDto {
    
    private String permissionId;
    private String groupId;
    private String connectionId;
    private String connectionType; // DB, SFTP
    private String permissionType; // READ, WRITE, ADMIN
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    
    // JOIN 결과용 필드
    private String groupName;
    private String connectionName;
    
    // 생성자
    public PermissionDto() {}
    
    public PermissionDto(String groupId, String connectionId, String connectionType) {
        this.groupId = groupId;
        this.connectionId = connectionId;
        this.connectionType = connectionType;
    }
    
    // Getter/Setter
    public String getPermissionId() {
        return permissionId;
    }
    
    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getConnectionType() {
        return connectionType;
    }
    
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    
    public String getPermissionType() {
        return permissionType;
    }
    
    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
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
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getConnectionName() {
        return connectionName;
    }
    
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }
    
    @Override
    public String toString() {
        return "PermissionDto{" +
                "permissionId='" + permissionId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", connectionType='" + connectionType + '\'' +
                ", permissionType='" + permissionType + '\'' +
                ", groupName='" + groupName + '\'' +
                ", connectionName='" + connectionName + '\'' +
                '}';
    }
}
