package kr.Windmill.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        logger.info("Connection status monitoring started");
        isRunning = true;
        
        // 연결 목록을 미리 확인중 상태로 초기화
        try {
            List<String> connectionList = com.ConnectionnList("DB");
            logger.info("초기 연결 목록 로드: {}", connectionList);
            
            for (String connection : connectionList) {
                String connectionName = connection.split("\\.")[0];
                ConnectionStatusDTO status = new ConnectionStatusDTO(
                    connectionName,
                    "checking",  // 확인중 상태
                    "#ffc107"    // 노란색
                );
                status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                connectionStatusMap.put(connectionName, status);
                logger.info("연결 상태 초기화: {} - 확인중", connectionName);
            }
        } catch (Exception e) {
            logger.error("초기 연결 목록 로드 중 오류 발생", e);
        }
        
        monitoringThread = new Thread(this::monitorConnections, "ConnectionStatusMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
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
        try {
            // Common 클래스의 기존 로그 활용 (Setproperties에서 이미 출력됨)
            logger.info("=== 연결 상태 확인 시작 ===");
            
            List<String> connectionList = com.ConnectionnList("DB");
            logger.info("발견된 연결 목록: {}", connectionList);
            
            for (String connection : connectionList) {
                String connectionName = connection.split("\\.")[0];
                logger.info("연결 상태 확인 중: {}", connectionName);
                updateConnectionStatus(connectionName);
            }
        } catch (Exception e) {
            logger.error("연결 목록 조회 중 오류 발생", e);
        }
    }
    
    private void updateConnectionStatus(String connectionName) {
        try {
            Map<String, String> connConfig = com.ConnectionConf(connectionName);
            
            // 5초 타임아웃으로 연결 테스트
            boolean isConnected = com.testConnection(connConfig);
            
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