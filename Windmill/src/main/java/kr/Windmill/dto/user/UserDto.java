package kr.Windmill.dto.user;

import java.time.LocalDateTime;

/**
 * 사용자 Dto 클래스
 */
public class UserDto {
    
    private String userId;
    private String userName;
    private String password;
    private String status;
    private String ipRestriction;
    private LocalDateTime lastLoginTimestamp;
    private Integer loginFailCount;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    private LocalDateTime passwordChangeDate;
    private String groupName; // JOIN 결과용
    
    // 생성자
    public UserDto() {}
    
    public UserDto(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
    
    // Getter/Setter
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
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
    
    public String getIpRestriction() {
        return ipRestriction;
    }
    
    public void setIpRestriction(String ipRestriction) {
        this.ipRestriction = ipRestriction;
    }
    
    public LocalDateTime getLastLoginTimestamp() {
        return lastLoginTimestamp;
    }
    
    public void setLastLoginTimestamp(LocalDateTime lastLoginTimestamp) {
        this.lastLoginTimestamp = lastLoginTimestamp;
    }
    
    public Integer getLoginFailCount() {
        return loginFailCount;
    }
    
    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
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
    
    public LocalDateTime getPasswordChangeDate() {
        return passwordChangeDate;
    }
    
    public void setPasswordChangeDate(LocalDateTime passwordChangeDate) {
        this.passwordChangeDate = passwordChangeDate;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    @Override
    public String toString() {
        return "UserDto{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", status='" + status + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}
