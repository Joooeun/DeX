package kr.Windmill.dto;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SQL 템플릿 실행을 위한 DTO 클래스
 */
public class SqlTemplateExecuteDto {
    
    private String templateId;        // 템플릿 ID
    private String templateName;      // 템플릿 이름
    private String connectionId;      // DB 연결 ID (선택적)
    private String sqlContent;        // 직접 입력된 SQL 내용 (선택적)
    private String parameters;        // 파라미터 JSON 문자열
    private String log;               // LOG 파라미터 JSON 문자열 (예전 소스와 동일)
    private List<Map<String, Object>> parameterList;  // 파싱된 파라미터 리스트
    private Integer limit;            // 실행 제한 (기본값: 1000)
    private Boolean audit;            // 감사 로그 저장 여부
    private Boolean skipMetadata;     // 메타데이터 조회 스킵 여부 (모니터링/PostgreSQL 최적화용)
    private String memberId;          // 사용자 ID (세션에서 설정)
    private String ip;                // 클라이언트 IP (세션에서 설정)
    
    // 로그 관련 필드
    private Instant startTime;        // 실행 시작 시간
    private Instant endTime;          // 실행 종료 시간
    private String result;            // 실행 결과 (Success/Failed)
    private Integer rows;             // 처리된 행 수
    private Integer logNo;            // 로그 번호
    private String logId;            // 로그 ID
    private String errorMessage;      // 에러 메시지
    private Duration executionTime;   // 실행 시간

    
    // 생성자
    public SqlTemplateExecuteDto() {
        this.limit = 1000; // 기본값 설정
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
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getSqlContent() {
        return sqlContent;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public String getParameters() {
        return parameters;
    }
    
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    
    public String getLog() {
        return log;
    }
    
    public void setLog(String log) {
        this.log = log;
    }
    
    public List<Map<String, Object>> getParameterList() {
        return parameterList;
    }
    
    public void setParameterList(List<Map<String, Object>> parameterList) {
        this.parameterList = parameterList;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Boolean getAudit() {
        return audit;
    }
    
    public void setAudit(Boolean audit) {
        this.audit = audit;
    }
    
    public Boolean getSkipMetadata() {
        return skipMetadata;
    }
    
    public void setSkipMetadata(Boolean skipMetadata) {
        this.skipMetadata = skipMetadata;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Integer getRows() {
        return rows;
    }
    
    public void setRows(Integer rows) {
        this.rows = rows;
    }
    
    public Integer getLogNo() {
        return logNo;
    }
    
    public void setLogNo(int logNo) {
		this.logNo = logNo;
		
		if (this.logId == null || this.logId.trim().isEmpty()) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddHHmmssSSS");
			String strNowDate = simpleDateFormat.format(Date.from(this.startTime));
			this.logId = this.memberId + "_" + this.templateName + "_" + strNowDate + "_" + logNo;
		}
	}


    public String getLogId() {
        return logId;
    }
    
    public void setLogId(String logId) {
        this.logId = logId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Duration getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Duration executionTime) {
        this.executionTime = executionTime;
    }

}
