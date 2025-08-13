package kr.Windmill.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConnectionStatusDTO {
    private String connectionId;
    private String status; // "connected", "disconnected", "error"
    private String color; // "#28a745", "#dc3545"
    private String lastChecked;
    private String errorMessage;

    public ConnectionStatusDTO() {}

    public ConnectionStatusDTO(String connectionId, String status, String color) {
        this.connectionId = connectionId;
        this.status = status;
        this.color = color;
        this.lastChecked = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters and Setters
    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(String lastChecked) {
        this.lastChecked = lastChecked;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 