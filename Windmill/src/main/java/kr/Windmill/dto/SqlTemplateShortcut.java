package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SQL 템플릿 단축키 DTO
 */
public class SqlTemplateShortcut {
    
    @JsonProperty("shortcutKey")
    private String shortcutKey;
    
    @JsonProperty("shortcutName")
    private String shortcutName;
    
    @JsonProperty("targetTemplateId")
    private String targetTemplateId;
    
    @JsonProperty("shortcutDescription")
    private String shortcutDescription;
    
    @JsonProperty("sourceColumnIndexes")
    private String sourceColumnIndexes;
    
    @JsonProperty("autoExecute")
    private Boolean autoExecute = true;
    
    @JsonProperty("isActive")
    private Boolean isActive = true;
    
    // 기본 생성자
    public SqlTemplateShortcut() {}
    
    // Getters and Setters
    public String getShortcutKey() {
        return shortcutKey;
    }
    
    public void setShortcutKey(String shortcutKey) {
        this.shortcutKey = shortcutKey;
    }
    
    public String getShortcutName() {
        return shortcutName;
    }
    
    public void setShortcutName(String shortcutName) {
        this.shortcutName = shortcutName;
    }
    
    public String getTargetTemplateId() {
        return targetTemplateId;
    }
    
    public void setTargetTemplateId(String targetTemplateId) {
        this.targetTemplateId = targetTemplateId;
    }
    
    public String getShortcutDescription() {
        return shortcutDescription;
    }
    
    public void setShortcutDescription(String shortcutDescription) {
        this.shortcutDescription = shortcutDescription;
    }
    
    public String getSourceColumnIndexes() {
        return sourceColumnIndexes;
    }
    
    public void setSourceColumnIndexes(String sourceColumnIndexes) {
        this.sourceColumnIndexes = sourceColumnIndexes;
    }
    
    public Boolean getAutoExecute() {
        return autoExecute;
    }
    
    public void setAutoExecute(Boolean autoExecute) {
        this.autoExecute = autoExecute;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "SqlTemplateShortcut{" +
                "shortcutKey='" + shortcutKey + '\'' +
                ", shortcutName='" + shortcutName + '\'' +
                ", targetTemplateId='" + targetTemplateId + '\'' +
                ", shortcutDescription='" + shortcutDescription + '\'' +
                ", sourceColumnIndexes='" + sourceColumnIndexes + '\'' +
                ", autoExecute=" + autoExecute +
                ", isActive=" + isActive +
                '}';
    }
}
