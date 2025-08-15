package kr.Windmill.dto.permission;

import java.time.LocalDateTime;

/**
 * SQL 템플릿 카테고리 권한 DTO 클래스
 */
public class SqlTemplatePermissionDto {
    
    private String groupId;
    private String categoryId;
    private String accessType; // READ, WRITE, DELETE, ADMIN
    private String grantedBy;
    private LocalDateTime grantedTimestamp;
    
    // JOIN 결과용 필드
    private String groupName;
    private String categoryName;
    private String categoryDescription;
    
    // 생성자
    public SqlTemplatePermissionDto() {}
    
    public SqlTemplatePermissionDto(String groupId, String categoryId, String accessType) {
        this.groupId = groupId;
        this.categoryId = categoryId;
        this.accessType = accessType;
    }
    
    // Getter/Setter
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getAccessType() {
        return accessType;
    }
    
    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }
    
    public String getGrantedBy() {
        return grantedBy;
    }
    
    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }
    
    public LocalDateTime getGrantedTimestamp() {
        return grantedTimestamp;
    }
    
    public void setGrantedTimestamp(LocalDateTime grantedTimestamp) {
        this.grantedTimestamp = grantedTimestamp;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getCategoryDescription() {
        return categoryDescription;
    }
    
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }
    
    @Override
    public String toString() {
        return "SqlTemplatePermissionDto{" +
                "groupId='" + groupId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", accessType='" + accessType + '\'' +
                ", groupName='" + groupName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
