package kr.Windmill.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kr.Windmill.util.Common;

@Service
public class ConnectionStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionStatusService.class);
    
    private final Common com = new Common();
    private final Map<String, ConnectionStatusDTO> connectionStatusMap = new ConcurrentHashMap<>();
    private Thread monitoringThread;
    private volatile boolean isRunning = false;
    
    @PostConstruct
    public void startMonitoring() {
        isRunning = true;
        monitoringThread = new Thread(this::monitorConnections);
        monitoringThread.setDaemon(true);
        monitoringThread.setName("ConnectionStatusMonitor");
        monitoringThread.start();
        logger.info("Connection status monitoring started");
    }
    
    @PreDestroy
    public void stopMonitoring() {
        logger.info("Connection status monitoring stopping...");
        isRunning = false;
        
        if (monitoringThread != null && monitoringThread.isAlive()) {
            monitoringThread.interrupt();
            try {
                // 모니터링 스레드가 종료될 때까지 최대 10초 대기
                monitoringThread.join(10000);
                if (monitoringThread.isAlive()) {
                    logger.warn("모니터링 스레드가 10초 내에 종료되지 않았습니다.");
                } else {
                    logger.info("모니터링 스레드가 정상적으로 종료되었습니다.");
                }
            } catch (InterruptedException e) {
                logger.warn("모니터링 스레드 종료 대기 중 인터럽트 발생");
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Connection status monitoring stopped");
    }
    
    private void monitorConnections() {
        while (isRunning) {
            try {
                updateAllConnectionStatuses();
                Thread.sleep(10000); // 10초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error during connection monitoring", e);
            }
        }
    }
    
    private void updateAllConnectionStatuses() {
        List<String> connectionList = com.ConnectionnList("DB");
		
		for (String connection : connectionList) {
		    String connectionName = connection.split("\\.")[0];
		    updateConnectionStatus(connectionName);
		}
    }
    
    private void updateConnectionStatus(String connectionName) {
        try {
            Map<String, String> connConfig = com.ConnectionConf(connectionName);
            
            // 5초 타임아웃으로 연결 테스트
            boolean isConnected = testConnectionWithTimeout(connConfig, 5000);
            
            // 기존 상태가 있으면 업데이트, 없으면 새로 생성
            ConnectionStatusDTO status = connectionStatusMap.get(connectionName);
            if (status == null) {
                status = new ConnectionStatusDTO(
                    connectionName,
                    isConnected ? "connected" : "disconnected",
                    isConnected ? "#28a745" : "#dc3545"
                );
            } else {
                status.setStatus(isConnected ? "connected" : "disconnected");
                status.setColor(isConnected ? "#28a745" : "#dc3545");
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            connectionStatusMap.put(connectionName, status);
            
            // 한 줄로 통합된 로그
            if (isConnected) {
                logger.info("DB 연결 상태 확인 완료: {} - 연결됨", connectionName);
            } else {
                logger.warn("DB 연결 상태 확인 완료: {} - 연결실패", connectionName);
            }
            
        } catch (Exception e) {
            // 기존 상태가 있으면 업데이트, 없으면 새로 생성
            ConnectionStatusDTO status = connectionStatusMap.get(connectionName);
            if (status == null) {
                status = new ConnectionStatusDTO(
                    connectionName,
                    "error",
                    "#dc3545"
                );
            } else {
                status.setStatus("error");
                status.setColor("#dc3545");
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            status.setErrorMessage(e.getMessage());
            connectionStatusMap.put(connectionName, status);
            logger.error("DB 연결 상태 확인 완료: {} - 오류발생: {}", connectionName, e.getMessage());
        }
    }
    
    // 타임아웃이 있는 연결 테스트 메서드
    private boolean testConnectionWithTimeout(Map<String, String> connConfig, int timeoutMs) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    return com.testConnection(connConfig);
                } catch (Exception e) {
                    return false;
                }
            });
            
            try {
                // 타임아웃 설정
                boolean result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                return result;
            } catch (TimeoutException e) {
                future.cancel(true);
                return false;
            } catch (Exception e) {
                return false;
            }
        } finally {
            // ExecutorService 정리
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warn("ExecutorService가 정상적으로 종료되지 않았습니다.");
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public List<ConnectionStatusDTO> getAllConnectionStatuses() {
        return new ArrayList<>(connectionStatusMap.values());
    }
    
    public List<ConnectionStatusDTO> getConnectionStatusesForUser(String userId) {
        try {
            if ("admin".equals(userId)) {
                return getAllConnectionStatuses();
            }
            
            Map<String, String> userConfig = com.UserConf(userId);
            List<String> allowedConnections = Arrays.asList(userConfig.get("CONNECTION").split(","));
            
            return connectionStatusMap.values().stream()
                .filter(status -> allowedConnections.contains(status.getConnectionName()))
                .collect(Collectors.toList());
                
        } catch (IOException e) {
            logger.error("Error getting user configuration for {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    public ConnectionStatusDTO getConnectionStatus(String connectionName) {
        return connectionStatusMap.get(connectionName);
    }
    
    // 수동으로 특정 연결 상태 업데이트
    public void updateConnectionStatusManually(String connectionName) {
        logger.info("수동 DB 연결 상태 확인 요청: {}", connectionName);
        updateConnectionStatus(connectionName);
        logger.info("수동 DB 연결 상태 확인 완료: {}", connectionName);
    }
} 