package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SQL 템플릿 파라미터 DTO
 */
public class SqlTemplateParameter {
    
    @JsonProperty("parameterName")
    private String parameterName;
    
    @JsonProperty("parameterType")
    private String parameterType;
    
    @JsonProperty("parameterOrder")
    private Integer parameterOrder;
    
    @JsonProperty("isRequired")
    private Boolean isRequired = false;
    
    @JsonProperty("defaultValue")
    private String defaultValue;
    
    @JsonProperty("isReadonly")
    private Boolean isReadonly = false;
    
    @JsonProperty("isHidden")
    private Boolean isHidden = false;
    
    @JsonProperty("isDisabled")
    private Boolean isDisabled = false;
    
    @JsonProperty("description")
    private String description;
    
    // 기본 생성자
    public SqlTemplateParameter() {}
    
    // Getters and Setters
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
    
    public Integer getParameterOrder() {
        return parameterOrder;
    }
    
    public void setParameterOrder(Integer parameterOrder) {
        this.parameterOrder = parameterOrder;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public Boolean getIsReadonly() {
        return isReadonly;
    }
    
    public void setIsReadonly(Boolean isReadonly) {
        this.isReadonly = isReadonly;
    }
    
    public Boolean getIsHidden() {
        return isHidden;
    }
    
    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }
    
    public Boolean getIsDisabled() {
        return isDisabled;
    }
    
    public void setIsDisabled(Boolean isDisabled) {
        this.isDisabled = isDisabled;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "SqlTemplateParameter{" +
                "parameterName='" + parameterName + '\'' +
                ", parameterType='" + parameterType + '\'' +
                ", parameterOrder=" + parameterOrder +
                ", isRequired=" + isRequired +
                ", defaultValue='" + defaultValue + '\'' +
                ", isReadonly=" + isReadonly +
                ", isHidden=" + isHidden +
                ", isDisabled=" + isDisabled +
                ", description='" + description + '\'' +
                '}';
    }
}
