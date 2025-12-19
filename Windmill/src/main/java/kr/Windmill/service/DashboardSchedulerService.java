package kr.Windmill.service;

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
import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * 대시보드 스케줄러 서비스
 * 차트별로 다른 주기로 데이터를 수집하고 캐시에 저장
 */
@Service
@DependsOn({"dataSource", "jdbcTemplate"})
public class DashboardSchedulerService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DynamicJdbcManager.class);

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    private kr.Windmill.util.DynamicJdbcManager dynamicJdbcManager;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // 스케줄러 저장소
    private final Map<String, ScheduledFuture<?>> schedulers = new ConcurrentHashMap<>();

    // 캐시 저장소 (실제 운영에서는 Redis 등 사용 권장)
    private final Map<String, Object> chartDataCache = new ConcurrentHashMap<>();
    
    // 차트별 성공/실패 상태 저장 (차트ID_연결ID 형태의 키)
    private final Map<String, Boolean> chartSuccessStatus = new ConcurrentHashMap<>();
    
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
                cleanupExecutor.schedule(this::initializeSchedulers, 5, TimeUnit.SECONDS);
                return;
            }
            
            // 커넥션풀 생성 여부 확인
            if (dynamicJdbcManager.getPoolCount() == 0) {
                logger.warn("커넥션풀이 생성되지 않았습니다. 5초 후 다시 시도합니다.");
                cleanupExecutor.schedule(this::initializeSchedulers, 5, TimeUnit.SECONDS);
                return;
            }
            
            // 동적 차트 설정에서 각 차트의 템플릿 정보를 참조하여 간격 설정
            initializeDynamicSchedulers();
        } catch (Exception e) {
            System.err.println("❌ 대시보드 스케줄러 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 동적 차트 설정에 따른 스케줄러 초기화
     */
    private void initializeDynamicSchedulers() {
        try {
            String chartConfig = systemConfigService.getDashboardChartConfig();
            if (chartConfig == null || chartConfig.trim().isEmpty() || chartConfig.equals("{}")) {
                System.out.println("차트 설정이 없습니다. 스케줄러를 시작하지 않습니다.");
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(chartConfig, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> charts = (List<Map<String, Object>>) config.get("charts");
            
            if (charts != null) {
                for (Map<String, Object> chart : charts) {
                    String chartId = (String) chart.get("id");
                    String templateId = (String) chart.get("templateId");
                    
                    // 템플릿 정보에서 REFRESH_TIMEOUT 조회
                    int refreshTimeout = getTemplateRefreshTimeout(templateId);
                    
                    startScheduler(chartId, refreshTimeout);
                }
            }
        } catch (Exception e) {
            System.err.println("동적 스케줄러 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 템플릿의 새로고침 간격 조회
     */
    private int getTemplateRefreshTimeout(String templateId) {
        try {
            String sql = "SELECT REFRESH_TIMEOUT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ? AND STATUS = 'ACTIVE'";
            Integer timeout = jdbcTemplate.queryForObject(sql, Integer.class, templateId);
            return timeout != null && timeout > 0 ? timeout : 10; // 기본값 10초
        } catch (Exception e) {
            System.err.println("템플릿 새로고침 간격 조회 실패 [" + templateId + "]: " + e.getMessage());
            return 10; // 기본값 10초
        }
    }

    /**
     * 서버 종료 시 스케줄러 정리
     */
    @PreDestroy
    public void shutdownSchedulers() {
        for (Map.Entry<String, ScheduledFuture<?>> entry : schedulers.entrySet()) {
            ScheduledFuture<?> scheduler = entry.getValue();

            if (scheduler != null && !scheduler.isCancelled()) {
                scheduler.cancel(false);
            }
        }

        schedulers.clear();
        chartDataCache.clear();
        chartSuccessStatus.clear();
    }

    /**
     * 특정 차트 ID의 스케줄러 시작
     * 
     * @param chartId   차트 ID
     * @param refreshTimeout 새로고침 주기 (초)
     */
    private void startScheduler(String chartId, int refreshTimeout) {
        // 기존 스케줄러가 있으면 중지
        stopScheduler(chartId);

        // 새로운 스케줄러 시작
        ScheduledFuture<?> scheduler = taskScheduler.scheduleAtFixedRate(() -> updateChartData(chartId), refreshTimeout*1000);

        schedulers.put(chartId, scheduler);
    }

    /**
     * 특정 차트 ID의 스케줄러 중지
     * 
     * @param chartId 차트 ID
     */
    private void stopScheduler(String chartId) {
        ScheduledFuture<?> scheduler = schedulers.get(chartId);
        if (scheduler != null && !scheduler.isCancelled()) {
            scheduler.cancel(false);
            schedulers.remove(chartId);
        }
    }

    /**
     * 특정 차트 ID의 데이터 업데이트
     * 
     * @param chartId 차트 ID
     */
    private void updateChartData(String chartId) {
        try {
            // 차트 정보 조회
            Map<String, Object> chartInfo = getChartInfoById(chartId);
            if (chartInfo == null) {
                System.err.println("❌ 차트 정보를 찾을 수 없습니다: " + chartId);
                return;
            }

            // 활성화된 연결 ID 목록 조회
            List<String> connectionIds = getActiveConnectionIds();
            
            // 연결된 DB가 없으면 조회 시도하지 않음
            if (connectionIds.isEmpty()) {
                System.out.println("⚠️ " + chartId + " 연결된 DB가 없어 조회를 건너뜁니다.");
                return;
            }
            
            for (String connectionId : connectionIds) {
                String statusKey = chartId + "_" + connectionId;
                
                // 이전에 실패한 차트는 건너뛰기
                if (chartSuccessStatus.containsKey(statusKey) && !chartSuccessStatus.get(statusKey)) {
                    //System.out.println("⚠️ " + chartId + " [" + connectionId + "] 이전에 실패한 차트로 건너뜁니다.");
                    continue;
                }
                
                try {
                    // 해당 차트의 템플릿 실행
                    Object chartData = executeTemplateByTemplateId((String) chartInfo.get("templateId"), connectionId);
                    
                    // 캐시에 저장 (성공/실패 관계없이)
                    String cacheKey = chartId + "_" + connectionId;
                    chartDataCache.put(cacheKey, chartData);
                    
                    // 에러 결과인지 확인하여 상태 저장
                    if (chartData instanceof Map && ((Map<?, ?>) chartData).containsKey("error")) {
                        System.out.println("⚠️ " + chartId + " [" + connectionId + "] 조회 결과에 에러가 있어 상태를 실패로 저장합니다.");
                        chartSuccessStatus.put(statusKey, false);
                    } else {
                        // 성공한 경우 상태를 성공으로 저장
                        chartSuccessStatus.put(statusKey, true);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ " + chartId + " [" + connectionId + "] 데이터 업데이트 실패: " + e.getMessage());
                    
                    // 예외 발생 시에도 에러 데이터를 캐시에 저장
                    String cacheKey = chartId + "_" + connectionId;
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "차트 조회 실패: " + e.getMessage());
                    errorResult.put("success", false);
                    chartDataCache.put(cacheKey, errorResult);
                    chartSuccessStatus.put(statusKey, false);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ " + chartId + " 데이터 업데이트 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 차트 ID로 차트 정보 조회
     * 
     * @param chartId 차트 ID
     * @return 차트 정보
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getChartInfoById(String chartId) {
        try {
            String chartConfig = systemConfigService.getDashboardChartConfig();
            if (chartConfig == null || chartConfig.trim().isEmpty() || chartConfig.equals("{}")) {
                return null;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(chartConfig, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> charts = (List<Map<String, Object>>) config.get("charts");
            
            if (charts != null) {
                for (Map<String, Object> chart : charts) {
                    if (chartId.equals(chart.get("id"))) {
                        return chart;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("차트 정보 조회 실패: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 템플릿 ID로 템플릿 실행
     * 
     * @param templateId 템플릿 ID
     * @param connectionId 연결 ID
     * @return 실행 결과
     */
    @SuppressWarnings("unchecked")
    private Object executeTemplateByTemplateId(String templateId, String connectionId) {
        try {
            // SQLExecuteService를 통해 템플릿 실행
            SqlTemplateExecuteDto executeDto = new SqlTemplateExecuteDto();
            executeDto.setTemplateId(templateId);
            executeDto.setConnectionId(connectionId);
            executeDto.setLimit(1000);
            executeDto.setSkipMetadata(true);  // 모니터링 조회 시 메타데이터 조회 스킵
            
            @SuppressWarnings("rawtypes")
            Map<String, List> sqlResult = sqlExecuteService.executeTemplateSQL(executeDto);
            
            // SQL 에러 체크
            if (sqlResult.containsKey("error")) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", sqlResult.get("error"));
                return errorResult;
            }
            
            // success 필드로 성공 여부 확인
            List<Boolean> successList = (List<Boolean>) sqlResult.get("success");
            if (successList != null && !successList.isEmpty() && !successList.get(0)) {
                Map<String, Object> errorResult = new HashMap<>();
				errorResult.put("error", ((List) sqlResult.get("rowbody").get(0)).get(0));
                return errorResult;
            }
            
            // 성공 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("templateId", templateId);
            result.put("result", sqlResult.get("rowbody"));
           
            return result;
            
        } catch (Exception e) {
            System.err.println("템플릿 실행 실패 [" + templateId + "][" + connectionId + "]: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "템플릿 실행 중 오류가 발생했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 활성화된 연결 ID 목록 조회
     */
    private List<String> getActiveConnectionIds() {
        try {
            // ConnectionService에서 온라인 연결 ID 목록 조회
            List<String> onlineConnectionIds = connectionService.getOnlineConnectionIds();
            
            // 온라인 연결이 없으면 빈 리스트 반환 (조회 시도하지 않음)
            if (onlineConnectionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            return onlineConnectionIds;

        } catch (Exception e) {
            System.err.println("연결 ID 목록 조회 실패: " + e.getMessage());
            // 오류 시에도 빈 리스트 반환 (조회 시도하지 않음)
            return new ArrayList<>();
        }
    }

    /**
     * 캐시된 차트 데이터 조회
     * 
     * @param chartId 차트 ID
     * @param connectionId 연결 ID
     * @return 차트 데이터
     */
    public Object getChartData(String chartId, String connectionId) {
        String cacheKey = chartId + "_" + connectionId;
        return chartDataCache.get(cacheKey);
    }

    /**
     * 스케줄러 갱신 (설정 변경 시 호출)
     */
    public void refreshSchedulers() {
        try {
            System.out.println("🔄 스케줄러 갱신 시작...");
            
            // 1. 기존 스케줄러 모두 중지
            shutdownSchedulers();
            
            // 2. 캐시 초기화
            chartDataCache.clear();
            chartSuccessStatus.clear();
            
            // 3. 새로운 설정으로 재시작
            initializeDynamicSchedulers();
            
            System.out.println("✅ 스케줄러 갱신 완료");
        } catch (Exception e) {
            System.err.println("❌ 스케줄러 갱신 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 모든 차트의 에러 상태 리셋
     */
    public void resetAllErrorStatus() {
        try {
            System.out.println("🔄 모든 차트 에러 상태 리셋 시작...");
            
            // 성공/실패 상태 초기화
            chartSuccessStatus.clear();
            
            
            System.out.println("✅ 모든 차트 에러 상태 리셋 완료");
        } catch (Exception e) {
            System.err.println("❌ 에러 상태 리셋 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
