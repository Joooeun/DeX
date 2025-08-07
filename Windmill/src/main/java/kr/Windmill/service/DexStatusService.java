package kr.Windmill.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kr.Windmill.util.Common;

@Service
public class DexStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(DexStatusService.class);
    
    private final Common com = new Common();
    private final Map<String, DexStatusDTO> dexStatusMap = new ConcurrentHashMap<>();
    private Thread monitoringThread;
    private volatile boolean isRunning = false;
    
    @PostConstruct
    public void startMonitoring() {
        logger.info("DEX 상태 모니터링 시작");
        isRunning = true;
        
        // 초기 상태 설정
        initializeDexStatus();
        
        monitoringThread = new Thread(this::monitorDexStatus, "DexStatusMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }
    
    @PreDestroy
    public void stopMonitoring() {
        logger.info("DEX 상태 모니터링 중지");
        isRunning = false;
        
        if (monitoringThread != null && monitoringThread.isAlive()) {
            monitoringThread.interrupt();
            try {
                // 모니터링 스레드가 종료될 때까지 최대 3초 대기 (10초에서 단축)
                monitoringThread.join(3000);
                if (monitoringThread.isAlive()) {
                    logger.warn("DEX 모니터링 스레드가 3초 내에 종료되지 않았습니다. 강제 종료합니다.");
                    // 강제 종료를 위해 추가 인터럽트
                    monitoringThread.interrupt();
                    monitoringThread.join(1000); // 추가 1초 대기
                } else {
                    logger.info("DEX 모니터링 스레드가 정상적으로 종료되었습니다.");
                }
            } catch (InterruptedException e) {
                logger.warn("DEX 모니터링 스레드 종료 대기 중 인터럽트 발생");
                Thread.currentThread().interrupt();
            }
        }
        
        // 상태 맵 정리
        dexStatusMap.clear();
        logger.info("DEX 상태 모니터링 중지 완료");
    }
    
    private void initializeDexStatus() {
        // DEX 프로세스 상태 초기화
        DexStatusDTO processStatus = new DexStatusDTO(
            "dex_process",
            "DEX 프로세스",
            "checking",
            "#ffc107",
            "프로세스 상태 확인 중"
        );
        dexStatusMap.put("dex_process", processStatus);
        
        // DEX 서비스 상태 초기화
        DexStatusDTO serviceStatus = new DexStatusDTO(
            "dex_service", 
            "DEX 서비스",
            "checking",
            "#ffc107",
            "서비스 상태 확인 중"
        );
        dexStatusMap.put("dex_service", serviceStatus);
        
        logger.info("DEX 상태 초기화 완료");
    }
    
    private void monitorDexStatus() {
        logger.info("DEX 상태 모니터링 스레드 시작");
        while (isRunning) {
            try {
                logger.info("=== DEX 상태 업데이트 시작 ===");
                updateAllDexStatuses();
                logger.info("=== DEX 상태 업데이트 완료 ===");
                Thread.sleep(30000); // 30초마다 확인
            } catch (InterruptedException e) {
                logger.info("DEX 상태 모니터링 스레드 인터럽트됨");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("DEX 상태 모니터링 중 오류 발생", e);
            }
        }
        logger.info("DEX 상태 모니터링 스레드 종료");
    }
    
    private void updateAllDexStatuses() {
        try {
            // DEX 프로세스 상태 확인
            updateProcessStatus();
            
            // DEX 서비스 상태 확인
            updateServiceStatus();
            
        } catch (Exception e) {
            logger.error("DEX 상태 업데이트 중 오류 발생", e);
        }
    }
    
    private void updateProcessStatus() {
        try {
            logger.info("DEX 프로세스 상태 확인 중...");
            
            ProcessInfo processInfo = checkDexProcessDetailed();
            
            DexStatusDTO status = dexStatusMap.get("dex_process");
            if (processInfo.isRunning()) {
                status.setStatus("running");
                status.setColor("#28a745");
                status.setMessage("프로세스 정상 실행 중");
                status.setDetailInfo(processInfo.getDetailInfo());
                status.setCpuUsage(processInfo.getCpuUsage());
                status.setMemoryUsage(processInfo.getMemoryUsage());
                status.setPid(processInfo.getPid());
                logger.info("DEX 프로세스 정상 실행 중 - PID: {}, CPU: {}%, MEM: {}%", 
                    processInfo.getPid(), processInfo.getCpuUsage(), processInfo.getMemoryUsage());
            } else {
                status.setStatus("stopped");
                status.setColor("#dc3545");
                status.setMessage("프로세스 중지됨");
                status.setDetailInfo("Tomcat 프로세스를 찾을 수 없습니다.");
                status.setCpuUsage(0.0);
                status.setMemoryUsage(0.0);
                status.setPid("");
                logger.warn("DEX 프로세스 중지됨");
            }
            status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            logger.error("프로세스 상태 확인 중 오류", e);
            DexStatusDTO status = dexStatusMap.get("dex_process");
            status.setStatus("error");
            status.setColor("#dc3545");
            status.setMessage("확인 중 오류 발생: " + e.getMessage());
            status.setDetailInfo("오류: " + e.getMessage());
            status.setCpuUsage(0.0);
            status.setMemoryUsage(0.0);
            status.setPid("");
        }
    }
    
    private ProcessInfo checkDexProcessDetailed() {
        ProcessInfo processInfo = new ProcessInfo();
        
        try {
            logger.info("DEX 프로세스 상세 확인 시작...");
            
            // ps aux 명령어로 상세 프로세스 정보 확인
            ProcessBuilder pb = new ProcessBuilder("ps", "aux");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder detailInfo = new StringBuilder();
            int processCount = 0;
            
            while ((line = reader.readLine()) != null) {
                // Tomcat 관련 프로세스 검색
                if (line.toLowerCase().contains("tomcat") || 
                    (line.toLowerCase().contains("java") && line.toLowerCase().contains("catalina"))) {
                    processCount++;
                    processInfo.setRunning(true);
                    
                    // 프로세스 정보 파싱
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 11) {
                        String pid = parts[1];
                        String cpu = parts[2];
                        String mem = parts[3];
                        String vsz = parts[4];
                        String rss = parts[5];
                        String tty = parts[6];
                        String stat = parts[7];
                        String start = parts[8];
                        String time = parts[9];
                        String command = parts[10];
                        
                        // CPU와 메모리 사용률을 숫자로 변환
                        try {
                            double cpuValue = Double.parseDouble(cpu);
                            double memValue = Double.parseDouble(mem);
                            processInfo.setCpuUsage(cpuValue);
                            processInfo.setMemoryUsage(memValue);
                            processInfo.setPid(pid);
                        } catch (NumberFormatException e) {
                            logger.warn("CPU/메모리 값 파싱 실패: CPU={}, MEM={}", cpu, mem);
                        }
                        
                        detailInfo.append(String.format("PID: %s, CPU: %s%%, MEM: %s%%, VSZ: %sKB, RSS: %sKB, TIME: %s\n", 
                            pid, cpu, mem, vsz, rss, time));
                    } else {
                        detailInfo.append("프로세스: ").append(line.trim()).append("\n");
                    }
                    
                    logger.info("발견된 Tomcat 프로세스: {}", line.trim());
                }
            }
            
            process.waitFor();
            
            if (processCount == 0) {
                processInfo.setRunning(false);
                detailInfo.append("Tomcat 프로세스를 찾을 수 없습니다.");
            } else {
                detailInfo.insert(0, String.format("발견된 Tomcat 프로세스: %d개\n", processCount));
            }
            
            processInfo.setDetailInfo(detailInfo.toString());
            logger.info("프로세스 상세 확인 완료 - 발견된 Tomcat 프로세스: {}개", processCount);
            
        } catch (Exception e) {
            logger.error("프로세스 상세 확인 중 오류", e);
            processInfo.setRunning(false);
            processInfo.setDetailInfo("오류: " + e.getMessage());
        }
        
        return processInfo;
    }
    
    // 프로세스 정보를 담는 내부 클래스
    private static class ProcessInfo {
        private boolean running;
        private String detailInfo;
        private double cpuUsage;
        private double memoryUsage;
        private String pid;
        
        public ProcessInfo() {
            this.running = false;
            this.detailInfo = "";
            this.cpuUsage = 0.0;
            this.memoryUsage = 0.0;
            this.pid = "";
        }
        
        public boolean isRunning() {
            return running;
        }
        
        public void setRunning(boolean running) {
            this.running = running;
        }
        
        public String getDetailInfo() {
            return detailInfo;
        }
        
        public void setDetailInfo(String detailInfo) {
            this.detailInfo = detailInfo;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
        }
        
        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        
        public double getMemoryUsage() {
            return memoryUsage;
        }
        
        public void setMemoryUsage(double memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
        
        public String getPid() {
            return pid;
        }
        
        public void setPid(String pid) {
            this.pid = pid;
        }
    }
    
    private void updateServiceStatus() {
        try {
            logger.info("DEX 서비스 상태 확인 중...");
            
            boolean isServiceAvailable = checkDexService();
            
            DexStatusDTO status = dexStatusMap.get("dex_service");
            if (isServiceAvailable) {
                status.setStatus("available");
                status.setColor("#28a745");
                status.setMessage("서비스 정상 작동");
                logger.info("DEX 서비스 정상 작동");
            } else {
                status.setStatus("unavailable");
                status.setColor("#dc3545");
                status.setMessage("서비스 응답 없음");
                logger.warn("DEX 서비스 응답 없음");
            }
            status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
        } catch (Exception e) {
            logger.error("서비스 상태 확인 중 오류", e);
            DexStatusDTO status = dexStatusMap.get("dex_service");
            status.setStatus("error");
            status.setColor("#dc3545");
            status.setMessage("확인 중 오류 발생: " + e.getMessage());
        }
    }
    
    private boolean checkDexService() {
        try {
            logger.info("DEX 서비스 확인 시작...");
            
            // 현재 애플리케이션의 메인 페이지 접근 테스트
            String testUrl = "http://localhost:8080/";
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "DEX-Monitor");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            logger.info("서비스 확인 완료 - 응답 코드: {}", responseCode);
            return responseCode == 200;
            
        } catch (Exception e) {
            logger.error("서비스 확인 중 오류", e);
            return false;
        }
    }
    
    public List<DexStatusDTO> getAllDexStatuses() {
        return new ArrayList<>(dexStatusMap.values());
    }
    
    public DexStatusDTO getDexStatus(String statusName) {
        return dexStatusMap.get(statusName);
    }
    
    // 수동으로 특정 상태 업데이트
    public void updateDexStatusManually(String statusName) {
        logger.info("수동 DEX 상태 확인 요청: {}", statusName);
        
        if ("dex_process".equals(statusName)) {
            updateProcessStatus();
        } else if ("dex_service".equals(statusName)) {
            updateServiceStatus();
        }
        
        logger.info("수동 DEX 상태 확인 완료: {}", statusName);
    }
} 