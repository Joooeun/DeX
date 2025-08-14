package kr.Windmill.dto.sqltemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL 템플릿 Dto 클래스
 */
public class SqlTemplateDto {
    
    private String templateId;
    private String templateName;
    private String categoryPath;
    private String sqlContent;
    private String description;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    
    // 연관된 파라미터 목록
    private List<SqlTemplateParameterDto> parameters;
    
    // 생성자
    public SqlTemplateDto() {}
    
    public SqlTemplateDto(String templateId, String templateName, String sqlContent) {
        this.templateId = templateId;
        this.templateName = templateName;
        this.sqlContent = sqlContent;
    }
    
    // Getter/Setter
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public String getCategoryPath() {
        return categoryPath;
    }
    
    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }
    
    public String getSqlContent() {
        return sqlContent;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public List<SqlTemplateParameterDto> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<SqlTemplateParameterDto> parameters) {
        this.parameters = parameters;
    }
    
    @Override
    public String toString() {
        return "SqlTemplateDto{" +
                "templateId='" + templateId + '\'' +
                ", templateName='" + templateName + '\'' +
                ", categoryPath='" + categoryPath + '\'' +
                ", status='" + status + '\'' +
                ", parametersCount=" + (parameters != null ? parameters.size() : 0) +
                '}';
    }
}
