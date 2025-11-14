package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * SQL 템플릿 기본 정보 DTO
 */
public class SqlTemplateInfo {
    
    @JsonProperty("templateId")
    private String templateId;
    
    @JsonProperty("templateName")
    private String templateName;
    
    @JsonProperty("templateDesc")
    private String templateDesc;
    
    @JsonProperty("sqlContent")
    private String sqlContent;
    
    @JsonProperty("accessibleConnectionIds")
    private List<String> accessibleConnectionIds = new ArrayList<>();
    
    @JsonProperty("templateType")
    private String templateType = "SQL";
    
    @JsonProperty("version")
    private Integer version = 1;
    
    @JsonProperty("status")
    private String status = "ACTIVE";
    
    @JsonProperty("executionLimit")
    private Integer executionLimit = 0;
    
    @JsonProperty("refreshTimeout")
    private Integer refreshTimeout = 0;
    
    @JsonProperty("newline")
    private Boolean newline = true;
    
    @JsonProperty("audit")
    private Boolean audit = false;
    
    // 기본 생성자
    public SqlTemplateInfo() {}
    
    // Getters and Setters
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
    
    public String getTemplateDesc() {
        return templateDesc;
    }
    
    public void setTemplateDesc(String templateDesc) {
        this.templateDesc = templateDesc;
    }
    
    public String getSqlContent() {
        return sqlContent;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public List<String> getAccessibleConnectionIds() {
        return accessibleConnectionIds;
    }
    
    public void setAccessibleConnectionIds(List<String> accessibleConnectionIds) {
        this.accessibleConnectionIds = accessibleConnectionIds != null ? accessibleConnectionIds : new ArrayList<>();
    }
    
    public String getTemplateType() {
        return templateType;
    }
    
    public void setTemplateType(String templateType) {
        this.templateType = templateType != null ? templateType : "SQL";
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getExecutionLimit() {
        return executionLimit;
    }
    
    public void setExecutionLimit(Integer executionLimit) {
        this.executionLimit = executionLimit;
    }
    
    public Integer getRefreshTimeout() {
        return refreshTimeout;
    }
    
    public void setRefreshTimeout(Integer refreshTimeout) {
        this.refreshTimeout = refreshTimeout;
    }
    
    public Boolean getNewline() {
        return newline;
    }
    
    public void setNewline(Boolean newline) {
        this.newline = newline;
    }
    
    public Boolean getAudit() {
        return audit;
    }
    
    public void setAudit(Boolean audit) {
        this.audit = audit;
    }
    
    @Override
    public String toString() {
        return "SqlTemplateInfo{" +
                "templateId='" + templateId + '\'' +
                ", templateName='" + templateName + '\'' +
                ", templateDesc='" + templateDesc + '\'' +
                ", sqlContent='" + sqlContent + '\'' +
                ", accessibleConnectionIds=" + accessibleConnectionIds +
                ", templateType='" + templateType + '\'' +
                ", version=" + version +
                ", status='" + status + '\'' +
                ", executionLimit=" + executionLimit +
                ", refreshTimeout=" + refreshTimeout +
                ", newline=" + newline +
                ", audit=" + audit +
                '}';
    }
}
