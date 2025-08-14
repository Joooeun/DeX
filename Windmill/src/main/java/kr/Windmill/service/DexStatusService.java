package kr.Windmill.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.Windmill.dto.system.DexStatusDto;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Service
public class DexStatusService {

	private final Common com;
	private final Log cLog;
	private final Map<String, DexStatusDto> dexStatusMap = new ConcurrentHashMap<>();
	private ScheduledExecutorService scheduler;
	private volatile boolean isRunning = false;

	@Autowired
	public DexStatusService(Common common, Log log) {
		this.com = common;
		this.cLog = log;
	}

	@PostConstruct
	public void startMonitoring() {
		cLog.monitoringLog("DEX_STATUS", "DEX 상태 모니터링 시작");
		isRunning = true;

		// 초기 상태 설정
		initializeDexStatus();

		// ScheduledExecutorService 사용
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "DexStatusMonitor");
			t.setDaemon(true);
			return t;
		});

		// 30초마다 상태 업데이트 실행
		scheduler.scheduleAtFixedRate(this::updateAllDexStatuses, 0, 30, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void stopMonitoring() {
		cLog.monitoringLog("DEX_STATUS", "DEX 상태 모니터링 중지");
		System.out.println("=== DexStatusService 정리 시작 ===");
		isRunning = false;

		if (scheduler != null && !scheduler.isShutdown()) {
			System.out.println("DexStatusMonitor 스케줄러 종료 중...");
			try {
				// 스케줄러 종료 요청
				scheduler.shutdown();

				// 최대 5초 대기
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					cLog.monitoringLog("DEX_STATUS_WARN", "스케줄러가 5초 내에 종료되지 않았습니다. 강제 종료합니다.");
					System.out.println("DexStatusMonitor 스케줄러 종료 시도...");
					scheduler.shutdownNow();

					// 추가 2초 대기
					if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
						cLog.monitoringLog("DEX_STATUS_ERROR", "스케줄러 강제 종료 실패");
						System.out.println("DexStatusMonitor 스케줄러 강제 종료 실패");
					} else {
						System.out.println("DexStatusMonitor 스케줄러 강제 종료 완료");
					}
				} else {
					cLog.monitoringLog("DEX_STATUS", "스케줄러가 정상적으로 종료되었습니다.");
					System.out.println("DexStatusMonitor 스케줄러 정상 종료 완료");
				}
			} catch (InterruptedException e) {
				cLog.monitoringLog("DEX_STATUS_WARN", "스케줄러 종료 대기 중 인터럽트 발생");
				System.out.println("DexStatusMonitor 스케줄러 종료 중 인터럽트 발생");
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
		} else {
			System.out.println("DexStatusMonitor 스케줄러가 이미 종료되었거나 존재하지 않습니다.");
		}

		// 상태 맵 정리
		dexStatusMap.clear();
		System.out.println("DEX 상태 맵 정리 완료");
		cLog.monitoringLog("DEX_STATUS", "DEX 상태 모니터링 중지 완료");
		System.out.println("=== DexStatusService 정리 완료 ===");
	}

	private void initializeDexStatus() {
		// DEX 프로세스 상태 초기화
		DexStatusDto processStatus = new DexStatusDto("dex_process", "DEX 프로세스", "checking", "#ffc107", "프로세스 상태 확인 중");
		dexStatusMap.put("dex_process", processStatus);

		// DEX 서비스 상태 초기화
		DexStatusDto serviceStatus = new DexStatusDto("dex_service", "DEX 서비스", "checking", "#ffc107", "서비스 상태 확인 중");
		dexStatusMap.put("dex_service", serviceStatus);

		cLog.monitoringLog("DEX_STATUS", "DEX 상태 초기화 완료");
	}

	private void updateAllDexStatuses() {
		if (!isRunning) {
			return; // 모니터링이 중지된 경우 실행하지 않음
		}

		try {
			cLog.monitoringLog("DEX_STATUS", "=== DEX 상태 업데이트 시작 ===");

			// DEX 프로세스 상태 확인
			updateProcessStatus();

			// DEX 서비스 상태 확인
			updateServiceStatus();

			cLog.monitoringLog("DEX_STATUS", "=== DEX 상태 업데이트 완료 ===");

		} catch (Exception e) {
			cLog.monitoringLog("DEX_STATUS_ERROR", "DEX 상태 업데이트 중 오류 발생: " + e.getMessage());
		}
	}

	private void updateProcessStatus() {
		try {
			cLog.monitoringLog("DEX_PROCESS", "DEX 프로세스 상태 확인 중...");

			ProcessInfo processInfo = checkDexProcessDetailed();

			DexStatusDto status = dexStatusMap.get("dex_process");
			if (processInfo.isRunning()) {
				status.setStatus("running");
				status.setColor("#28a745");
				status.setMessage("프로세스 정상 실행 중");
				status.setDetailInfo(processInfo.getDetailInfo());
				status.setCpuUsage(processInfo.getCpuUsage());
				status.setMemoryUsage(processInfo.getMemoryUsage());
				status.setPid(processInfo.getPid());
				cLog.monitoringLog("DEX_PROCESS", "DEX 프로세스 정상 실행 중 - PID: " + processInfo.getPid() + ", CPU: " + processInfo.getCpuUsage()
						+ "%, MEM: " + processInfo.getMemoryUsage() + "%");
			} else {
				status.setStatus("stopped");
				status.setColor("#dc3545");
				status.setMessage("프로세스 중지됨");
				status.setDetailInfo("Tomcat 프로세스를 찾을 수 없습니다.");
				status.setCpuUsage(0.0);
				status.setMemoryUsage(0.0);
				status.setPid("");
				cLog.monitoringLog("DEX_PROCESS_WARN", "DEX 프로세스 중지됨");
			}
			status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

		} catch (Exception e) {
			cLog.monitoringLog("DEX_PROCESS_ERROR", "프로세스 상태 확인 중 오류: " + e.getMessage());
			DexStatusDto status = dexStatusMap.get("dex_process");
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
			cLog.monitoringLog("DEX_PROCESS", "DEX 프로세스 상세 확인 시작...");

			// ps aux 명령어로 상세 프로세스 정보 확인
			ProcessBuilder pb = new ProcessBuilder("ps", "aux");
			Process process = pb.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuilder detailInfo = new StringBuilder();
			int processCount = 0;

			while ((line = reader.readLine()) != null) {
				// Tomcat 관련 프로세스 검색
				if (line.toLowerCase().contains("tomcat") || (line.toLowerCase().contains("java") && line.toLowerCase().contains("catalina"))) {
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
							cLog.monitoringLog("DEX_PROCESS_WARN", "CPU/메모리 값 파싱 실패: CPU=" + cpu + ", MEM=" + mem);
						}

						detailInfo.append(
								String.format("PID: %s, CPU: %s%%, MEM: %s%%, VSZ: %sKB, RSS: %sKB, TIME: %s\n", pid, cpu, mem, vsz, rss, time));
					} else {
						detailInfo.append("프로세스: ").append(line.trim()).append("\n");
					}

					cLog.monitoringLog("DEX_PROCESS", "발견된 Tomcat 프로세스: " + line.trim());
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
			cLog.monitoringLog("DEX_PROCESS", "프로세스 상세 확인 완료 - 발견된 Tomcat 프로세스: " + processCount + "개");

		} catch (Exception e) {
			cLog.monitoringLog("DEX_PROCESS_ERROR", "프로세스 상세 확인 중 오류: " + e.getMessage());
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
			cLog.monitoringLog("DEX_SERVICE", "DEX 서비스 상태 확인 중...");

			boolean isServiceAvailable = checkDexService();

			DexStatusDto status = dexStatusMap.get("dex_service");
			if (isServiceAvailable) {
				status.setStatus("available");
				status.setColor("#28a745");
				status.setMessage("서비스 정상 작동");
				cLog.monitoringLog("DEX_SERVICE", "DEX 서비스 정상 작동");
			} else {
				status.setStatus("unavailable");
				status.setColor("#dc3545");
				status.setMessage("서비스 응답 없음");
				cLog.monitoringLog("DEX_SERVICE_WARN", "DEX 서비스 응답 없음");
			}
			status.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

		} catch (Exception e) {
			cLog.monitoringLog("DEX_SERVICE_ERROR", "서비스 상태 확인 중 오류: " + e.getMessage());
			DexStatusDto status = dexStatusMap.get("dex_service");
			status.setStatus("error");
			status.setColor("#dc3545");
			status.setMessage("확인 중 오류 발생: " + e.getMessage());
		}
	}

	private boolean checkDexService() {
		try {
			cLog.monitoringLog("DEX_SERVICE", "DEX 서비스 확인 시작...");

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

			cLog.monitoringLog("DEX_SERVICE", "서비스 확인 완료 - 응답 코드: " + responseCode);
			return responseCode == 200;

		} catch (Exception e) {
			cLog.monitoringLog("DEX_SERVICE_ERROR", "서비스 확인 중 오류: " + e.getMessage());
			return false;
		}
	}

	public List<DexStatusDto> getAllDexStatuses() {
		return new ArrayList<>(dexStatusMap.values());
	}

	public DexStatusDto getDexStatus(String statusName) {
		return dexStatusMap.get(statusName);
	}

	// 수동으로 특정 상태 업데이트
	public void updateDexStatusManually(String statusName) {
		cLog.monitoringLog("DEX_STATUS_MANUAL", "수동 DEX 상태 확인 요청: " + statusName);

		if ("dex_process".equals(statusName)) {
			updateProcessStatus();
		} else if ("dex_service".equals(statusName)) {
			updateServiceStatus();
		}

		cLog.monitoringLog("DEX_STATUS_MANUAL", "수동 DEX 상태 확인 완료: " + statusName);
	}
}