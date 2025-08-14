package kr.Windmill.dto.common;

import java.time.LocalDateTime;

/**
 * API 응답용 공통 Dto 클래스
 */
public class ApiResponseDto<T> {
    
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String errorCode;
    
    // 생성자
    public ApiResponseDto() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApiResponseDto(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
    
    public ApiResponseDto(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }
    
    // 정적 팩토리 메서드
    public static <T> ApiResponseDto<T> success(String message) {
        return new ApiResponseDto<>(true, message);
    }
    
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }
    
    public static <T> ApiResponseDto<T> error(String message) {
        return new ApiResponseDto<>(false, message);
    }
    
    public static <T> ApiResponseDto<T> error(String message, String errorCode) {
        ApiResponseDto<T> response = new ApiResponseDto<>(false, message);
        response.setErrorCode(errorCode);
        return response;
    }
    
    // Getter/Setter
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    @Override
    public String toString() {
        return "ApiResponseDto{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
