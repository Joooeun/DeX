package kr.Windmill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * SQL 템플릿 저장 요청 DTO
 * 새로운 JSON 형식에 맞는 깔끔한 데이터 구조
 */
public class SqlTemplateSaveRequest {
    
    @JsonProperty("template")
    private SqlTemplateInfo template;
    
    @JsonProperty("categories")
    private List<String> categories = new ArrayList<>();
    
    @JsonProperty("parameters")
    private List<SqlTemplateParameter> parameters = new ArrayList<>();
    
    @JsonProperty("shortcuts")
    private List<SqlTemplateShortcut> shortcuts = new ArrayList<>();
    
    @JsonProperty("sqlContents")
    private List<SqlContent> sqlContents = new ArrayList<>();
    
    // 기본 생성자
    public SqlTemplateSaveRequest() {}
    
    // Getters and Setters
    public SqlTemplateInfo getTemplate() {
        return template;
    }
    
    public void setTemplate(SqlTemplateInfo template) {
        this.template = template;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }
    
    public List<SqlTemplateParameter> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<SqlTemplateParameter> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }
    
    public List<SqlTemplateShortcut> getShortcuts() {
        return shortcuts;
    }
    
    public void setShortcuts(List<SqlTemplateShortcut> shortcuts) {
        this.shortcuts = shortcuts != null ? shortcuts : new ArrayList<>();
    }
    
    public List<SqlContent> getSqlContents() {
        return sqlContents;
    }
    
    public void setSqlContents(List<SqlContent> sqlContents) {
        this.sqlContents = sqlContents != null ? sqlContents : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return "SqlTemplateSaveRequest{" +
                "template=" + template +
                ", categories=" + categories +
                ", parameters=" + parameters +
                ", shortcuts=" + shortcuts +
                ", sqlContents=" + sqlContents +
                '}';
    }
}
