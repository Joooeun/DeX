package kr.Windmill.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.dto.connection.ConnectionStatusDto;
import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;

/**
 * 대시보드 스케줄러 서비스 각 차트별로 다른 주기로 데이터를 수집하고 캐시에 저장
 */
@Service
@DependsOn({"dataSource", "jdbcTemplate"})
public class DashboardSchedulerService {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DynamicJdbcManager.class);

	@Autowired
	private TemplateConfigService templateConfigService;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private ConnectionService connectionService;

	@Autowired
	private SQLExecuteService sqlExecuteService;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
	
	@Autowired
	private kr.Windmill.util.DynamicJdbcManager dynamicJdbcManager;

	// 스케줄러 저장소
	private final Map<String, ScheduledFuture<?>> schedulers = new ConcurrentHashMap<>();

	// 캐시 저장소 (실제 운영에서는 Redis 등 사용 권장)
	private final Map<String, Object> chartDataCache = new ConcurrentHashMap<>();
	

	private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

	/**
	 * 서버 시작 시 스케줄러 초기화
	 */
	@PostConstruct
	public void initializeSchedulers() {

		try {

			// RootPath 유효성 검증
			if (!Common.isRootPathValid()) {
				logger.warn("RootPath가 유효하지 않습니다. 5초 후 다시 시도합니다.");
				// 5초 후 다시 시도
				cleanupExecutor.schedule(this::initializeSchedulers, 5, TimeUnit.SECONDS);
				return;
			}
			
			// 커넥션풀 생성 여부 확인
			if (dynamicJdbcManager.getPoolCount() == 0) {
				logger.warn("커넥션풀이 생성되지 않았습니다. 5초 후 다시 시도합니다.");
				// 5초 후 다시 시도
				cleanupExecutor.schedule(this::initializeSchedulers, 5, TimeUnit.SECONDS);
				return;
			}
			
			Map<String, Integer> intervals = templateConfigService.getTemplateRefreshIntervals();

			for (Map.Entry<String, Integer> entry : intervals.entrySet()) {
				String chartMapping = entry.getKey();
				int refreshTimeout = entry.getValue();

				startScheduler(chartMapping, refreshTimeout);
			}
		} catch (Exception e) {
			System.err.println("❌ 대시보드 스케줄러 초기화 실패: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 서버 종료 시 스케줄러 정리
	 */
	@PreDestroy
	public void shutdownSchedulers() {

		for (Map.Entry<String, ScheduledFuture<?>> entry : schedulers.entrySet()) {
			String chartMapping = entry.getKey();
			ScheduledFuture<?> scheduler = entry.getValue();

			if (scheduler != null && !scheduler.isCancelled()) {
				scheduler.cancel(false);
			}
		}

		schedulers.clear();
		chartDataCache.clear();
	}

	/**
	 * 특정 차트 매핑의 스케줄러 시작
	 * 
	 * @param chartMapping   차트 매핑
	 * @param refreshTimeout 새로고침 주기 (밀리초)
	 */
	private void startScheduler(String chartMapping, int refreshTimeout) {
		// 기존 스케줄러가 있으면 중지
		stopScheduler(chartMapping);

		// 새로운 스케줄러 시작
		ScheduledFuture<?> scheduler = taskScheduler.scheduleAtFixedRate(() -> updateChartData(chartMapping), refreshTimeout*1000);

		schedulers.put(chartMapping, scheduler);
	}

	/**
	 * 특정 차트 매핑의 스케줄러 중지
	 * 
	 * @param chartMapping 차트 매핑
	 */
	private void stopScheduler(String chartMapping) {
		ScheduledFuture<?> scheduler = schedulers.get(chartMapping);
		if (scheduler != null && !scheduler.isCancelled()) {
			scheduler.cancel(false);
			schedulers.remove(chartMapping);
		}
	}

	/**
	 * 특정 차트 매핑의 데이터 업데이트
	 * 
	 * @param chartMapping 차트 매핑
	 */
	private void updateChartData(String chartMapping) {
		try {

			// 실시간으로 새로고침 주기 확인 및 업데이트
			int currentRefreshTimeout = templateConfigService.getRefreshInterval(chartMapping);
			
			if (currentRefreshTimeout > 0) {
				// 새로고침 주기가 변경되었는지 확인하고 필요시 스케줄러 재시작
				updateSchedulerIfNeeded(chartMapping, currentRefreshTimeout);
			}

			// 활성화된 연결 ID 목록 조회
			List<String> connectionIds = getActiveConnectionIds();

				for (String connectionId : connectionIds) {
					
					try {
						// 해당 차트 매핑의 템플릿 실행 (실시간 템플릿 정보 사용)
						Object chartData = executeTemplate(chartMapping, connectionId);
						
						// 캐시에 저장
						String cacheKey = chartMapping + "_" + connectionId;
						chartDataCache.put(cacheKey, chartData);
					} catch (Exception e) {
						System.err.println("❌ " + chartMapping + " [" + connectionId + "] 데이터 업데이트 실패: " + e.getMessage());
						
						// 연결 실패 시 에러 결과를 캐시에 저장
						String cacheKey = chartMapping + "_" + connectionId;
						chartDataCache.put(cacheKey, createErrorResult("연결 실패: " + e.getMessage()));
					}
				}

		} catch (Exception e) {
			System.err.println("❌ " + chartMapping + " 데이터 업데이트 중 오류: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 스케줄러 주기가 변경되었는지 확인하고 필요시 재시작
	 * 
	 * @param chartMapping      차트 매핑
	 * @param newRefreshTimeout 새로운 새로고침 주기
	 */
	private void updateSchedulerIfNeeded(String chartMapping, int newRefreshTimeout) {
		try {
			// 현재 스케줄러가 있는지 확인
			ScheduledFuture<?> currentScheduler = schedulers.get(chartMapping);

			if (currentScheduler == null || currentScheduler.isCancelled()) {
				// 스케줄러가 없거나 취소된 경우 새로 시작
				startScheduler(chartMapping, newRefreshTimeout);
			}
			// 주기 변경은 다음 스케줄링에서 자동으로 반영됨 (현재 실행 중인 스케줄러는 그대로 유지)

		} catch (Exception e) {
			System.err.println("❌ " + chartMapping + " 스케줄러 업데이트 실패: " + e.getMessage());
		}
	}

	/**
	 * 활성화된 연결 ID 목록 조회 (ConnectionService의 모니터링 상태 활용)
	 * 
	 * @return 연결 ID 목록
	 */
	private List<String> getActiveConnectionIds() {
		try {
			// ConnectionService에서 온라인 연결 ID 목록 조회
			List<String> onlineConnectionIds = connectionService.getOnlineConnectionIds();
			
			// 온라인 연결이 없는 경우 기본 연결 ID들 추가 (폴백)
			if (onlineConnectionIds.isEmpty()) {
				List<String> defaultConnections = new ArrayList<>();
				defaultConnections.add("pg_mac");
				defaultConnections.add("local_pg");
				defaultConnections.add("db2_12");
				return defaultConnections;
			}
			
			return onlineConnectionIds;

		} catch (Exception e) {
			System.err.println("연결 ID 목록 조회 실패: " + e.getMessage());
			// 오류 시 기본 연결 목록 반환
			List<String> defaultConnections = new ArrayList<>();
			defaultConnections.add("pg_mac");
			defaultConnections.add("local_pg");
			defaultConnections.add("db2_12");
			return defaultConnections;
		}
	}
	
	/**
	 * ConnectionService의 모니터링 상태를 확인하여 연결이 온라인인지 판단
	 * 
	 * @param connectionId 연결 ID
	 * @return 연결 온라인 여부
	 */
	private boolean isConnectionOnline(String connectionId) {
		try {
			// ConnectionService에서 연결 상태 조회
			ConnectionStatusDto status = connectionService.getConnectionStatus(connectionId);
			
			if (status == null) {
				// 상태 정보가 없으면 연결 테스트 수행
				return testConnectionDirectly(connectionId);
			}
			
			// 상태가 "connected"이면 온라인으로 판단
			boolean isOnline = "connected".equals(status.getStatus());
			
			if (!isOnline) {
			}
			
			return isOnline;
			
		} catch (Exception e) {
			System.err.println("연결 상태 확인 중 오류 [" + connectionId + "]: " + e.getMessage());
			// 오류 시 직접 테스트 수행
			return testConnectionDirectly(connectionId);
		}
	}
	
	/**
	 * 직접 연결 테스트 수행 (ConnectionService 상태가 없을 때만 사용)
	 * 
	 * @param connectionId 연결 ID
	 * @return 연결 성공 여부
	 */
	private boolean testConnectionDirectly(String connectionId) {
		try {
			// 2초 타임아웃으로 빠른 테스트
			Connection conn = null;
			try {
				conn = dynamicJdbcManager.getConnection(connectionId);
				// 간단한 쿼리로 연결 테스트
				try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
					stmt.setQueryTimeout(2); // 2초 타임아웃
					stmt.executeQuery();
				}
				return true;
			} catch (Exception e) {
				System.err.println("직접 연결 테스트 실패 [" + connectionId + "]: " + e.getMessage());
				return false;
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						// 무시
					}
				}
			}
		} catch (Exception e) {
			System.err.println("직접 연결 테스트 중 오류 [" + connectionId + "]: " + e.getMessage());
			return false;
		}
	}

	/**
	 * 특정 차트 매핑의 템플릿 실행
	 * 
	 * @param chartMapping 차트 매핑
	 * @param connectionId 연결 ID
	 * @return 차트 데이터
	 */
	private Object executeTemplate(String chartMapping, String connectionId) {
		try {
			// 템플릿 ID 조회
			String templateId = getTemplateIdByMapping(chartMapping);
			if (templateId == null) {
				System.err.println("템플릿 ID를 찾을 수 없습니다: " + chartMapping);
				return createErrorResult("템플릿을 찾을 수 없습니다: " + chartMapping);
			}

			// SQLExecuteService를 통해 템플릿 실행
			// SqlTemplateExecuteDto 생성
			SqlTemplateExecuteDto executeDto = new SqlTemplateExecuteDto();
			executeDto.setTemplateId(templateId);
			executeDto.setConnectionId(connectionId);
			executeDto.setAudit(false); // 배치 실행은 감사하지 않음
			
			// 템플릿 실행
			Map<String, List> sqlResult = sqlExecuteService.executeTemplateSQL(executeDto);

			// 결과 검증
			if (sqlResult == null) {
				return createErrorResult("SQL 실행 결과가 null입니다.");
			}

			// 에러 확인
			if (sqlResult.containsKey("error")) {
				Object errorObj = sqlResult.get("error");
				String errorMessage = (errorObj instanceof List) ? ((List<?>) errorObj).get(0).toString() : errorObj.toString();
				return createErrorResult(errorMessage);
			}

			// success 필드로 성공 여부 확인
			@SuppressWarnings("unchecked")
			List<Boolean> successList = (List<Boolean>) sqlResult.get("success");
			if (successList != null && !successList.isEmpty() && !successList.get(0)) {
				return createErrorResult("SQL 실행 오류가 발생했습니다.");
			}

			// 성공 결과 반환
			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("templateId", templateId);
			result.put("result", sqlResult);

			// hash는 List에서 첫 번째 요소를 String으로 변환
			List<?> hashList = (List<?>) sqlResult.get("hash");
			if (hashList != null && !hashList.isEmpty()) {
				result.put("hash", hashList.get(0).toString());
			} else {
				result.put("hash", "");
			}

			return result;

		} catch (Exception e) {
			System.err.println("템플릿 실행 실패 [" + chartMapping + ", " + connectionId + "]: " + e.getMessage());
			return createErrorResult("템플릿 실행 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 차트 매핑으로 템플릿 ID 조회 (실시간 DB 조회)
	 * 
	 * @param chartMapping 차트 매핑
	 * @return 템플릿 ID
	 */
	private String getTemplateIdByMapping(String chartMapping) {
		try {
			// DB에서 실시간으로 차트 매핑에 해당하는 템플릿 ID 조회
			String sql = "SELECT TEMPLATE_ID FROM SQL_TEMPLATE " + "WHERE CHART_MAPPING = ? AND STATUS = 'ACTIVE' " + "FETCH FIRST 1 ROWS ONLY";

			// JdbcTemplate을 사용하여 조회
			List<String> results = jdbcTemplate.queryForList(sql, String.class, chartMapping);

			if (results != null && !results.isEmpty()) {
				return results.get(0);
			} else {
				System.err.println("차트 매핑에 해당하는 활성 템플릿을 찾을 수 없습니다: " + chartMapping);
				return null;
			}

		} catch (Exception e) {
			System.err.println("템플릿 ID 조회 실패 [" + chartMapping + "]: " + e.getMessage());

			// DB 조회 실패 시 기본값 사용 (폴백)
			switch (chartMapping) {
			case "APPL_COUNT":
				return "201_Activity";
			case "LOCK_WAIT_COUNT":
				return "LOCK_WAIT_COUNT";
			case "ACTIVE_LOG":
				return "ACTIVE_LOG";
			case "FILESYSTEM":
				return "FILE_SYSTEM";
			default:
				return null;
			}
		}
	}

	/**
	 * 에러 결과 생성
	 */
	private Map<String, Object> createErrorResult(String errorMessage) {
		Map<String, Object> errorResult = new HashMap<>();
		errorResult.put("error", errorMessage);
		errorResult.put("errorType", "BATCH_ERROR");
		return errorResult;
	}

	/**
	 * 캐시된 차트 데이터 조회
	 * 
	 * @param chartMapping 차트 매핑
	 * @param connectionId 연결 ID
	 * @return 차트 데이터
	 */
	public Object getChartData(String chartMapping, String connectionId) {
		String cacheKey = chartMapping + "_" + connectionId;
		
		return chartDataCache.get(cacheKey);
	}

	/**
	 * 모든 캐시된 차트 데이터 조회
	 * 
	 * @return 모든 차트 데이터
	 */
	public Map<String, Object> getAllChartData() {
		return new ConcurrentHashMap<>(chartDataCache);
	}

	/**
	 * 특정 차트 매핑의 새로고침 주기 업데이트
	 * 
	 * @param chartMapping 차트 매핑
	 * @param newInterval  새로운 새로고침 주기 (밀리초)
	 */
	public void updateRefreshInterval(String chartMapping, int newInterval) {
		if (newInterval > 0) {
			startScheduler(chartMapping, newInterval);
		} else {
			stopScheduler(chartMapping);
		}
	}
}
