package kr.Windmill.dto.sqltemplate;

import java.time.LocalDateTime;

/**
 * SQL 템플릿 파라미터 Dto 클래스
 */
public class SqlTemplateParameterDto {
    
    private String parameterId;
    private String templateId;
    private String parameterName;
    private String parameterType; // STRING, NUMBER, DATE, BOOLEAN
    private String defaultValue;
    private String description;
    private Integer parameterOrder;
    private Boolean required;
    private String status;
    private LocalDateTime createdTimestamp;
    private String createdBy;
    private LocalDateTime modifiedTimestamp;
    private String modifiedBy;
    
    // 생성자
    public SqlTemplateParameterDto() {}
    
    public SqlTemplateParameterDto(String templateId, String parameterName, String parameterType) {
        this.templateId = templateId;
        this.parameterName = parameterName;
        this.parameterType = parameterType;
    }
    
    // Getter/Setter
    public String getParameterId() {
        return parameterId;
    }
    
    public void setParameterId(String parameterId) {
        this.parameterId = parameterId;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
    
    public String getParameterType() {
        return parameterType;
    }
    
    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getParameterOrder() {
        return parameterOrder;
    }
    
    public void setParameterOrder(Integer parameterOrder) {
        this.parameterOrder = parameterOrder;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
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
        return "SqlTemplateParameterDto{" +
                "parameterId='" + parameterId + '\'' +
                ", templateId='" + templateId + '\'' +
                ", parameterName='" + parameterName + '\'' +
                ", parameterType='" + parameterType + '\'' +
                ", parameterOrder=" + parameterOrder +
                ", required=" + required +
                '}';
    }
}
