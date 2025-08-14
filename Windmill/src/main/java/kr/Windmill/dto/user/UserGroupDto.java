package kr.Windmill.dto.user;

import java.time.LocalDateTime;

/**
 * 사용자 그룹 Dto 클래스
 */
public class UserGroupDto {
    
    private String groupId;
    private String groupName;
    private String groupDescription;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    private Integer memberCount; // JOIN 결과용
    
    // 생성자
    public UserGroupDto() {}
    
    public UserGroupDto(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }
    
    // Getter/Setter
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupDescription() {
        return groupDescription;
    }
    
    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
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
    
    public Integer getMemberCount() {
        return memberCount;
    }
    
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
    
    @Override
    public String toString() {
        return "UserGroupDto{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", status='" + status + '\'' +
                ", memberCount=" + memberCount +
                '}';
    }
}
