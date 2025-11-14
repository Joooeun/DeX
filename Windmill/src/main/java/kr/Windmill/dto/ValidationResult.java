package kr.Windmill.dto;

/**
 * 데이터 검증 결과 DTO
 */
public class ValidationResult {
    
    private boolean valid = true;
    private String errorMessage;
    private String errorCode;
    
    // 기본 생성자
    public ValidationResult() {}
    
    // 에러 추가
    public ValidationResult addError(String message, String errorCode) {
        this.valid = false;
        this.errorMessage = message;
        this.errorCode = errorCode;
        return this;
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
