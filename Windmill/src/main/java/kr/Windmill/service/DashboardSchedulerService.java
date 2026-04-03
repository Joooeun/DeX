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
    private static final String ALARM_SEVERITY_VALUE = "심각";
    private static final String INSERT_DASHBOARD_ALARM_LOG_SQL =
            "INSERT INTO DASHBOARD_ALARM_LOG " +
            "(CONNECTION_ID, CHART_NAME, COLUMN1_VALUE, COLUMN2_VALUE, COLUMN3_VALUE, CHECKED_TIMESTAMP) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

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
    
    @Autowired
    private Common common;

    // 스케줄러 저장소
    private final Map<String, ScheduledFuture<?>> schedulers = new ConcurrentHashMap<>();

    // 캐시 저장소 (실제 운영에서는 Redis 등 사용 권장)
    private final Map<String, Object> chartDataCache = new ConcurrentHashMap<>();
    
    /**
     * 템플릿+연결별 연속 실패 횟수 (키: templateId_connectionId).
     * 설정된 횟수 이상이면 해당 주기 조회를 건너뜁니다. 성공 시 0으로 리셋(맵에서 제거).
     */
    private final Map<String, Integer> chartConsecutiveFailures = new ConcurrentHashMap<>();
    
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
            // 차트 스케줄러 초기화
            String chartConfig = systemConfigService.getDashboardChartConfig();
            if (chartConfig != null && !chartConfig.trim().isEmpty() && !chartConfig.equals("{}")) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> config = mapper.readValue(chartConfig, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> charts = (List<Map<String, Object>>) config.get("charts");
                
                if (charts != null) {
                    // templateId 기준으로 그룹화하여 중복 스케줄러 방지
                    Map<String, Integer> templateTimeouts = new HashMap<>();
                    
                    for (Map<String, Object> chart : charts) {
                        String templateId = (String) chart.get("templateId");
                        if (templateId == null || templateId.trim().isEmpty()) {
                            continue;
                        }
                        
                        // 템플릿 정보에서 REFRESH_TIMEOUT 조회
                        int refreshTimeout = getTemplateRefreshTimeout(templateId);
                        templateTimeouts.put(templateId, refreshTimeout);
                    }
                    
                    // 각 templateId별로 스케줄러 시작 (중복 방지)
                    for (Map.Entry<String, Integer> entry : templateTimeouts.entrySet()) {
                        startScheduler(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            // 모니터링 템플릿 스케줄러 초기화
            initializeMonitoringTemplateScheduler();
        } catch (Exception e) {
            System.err.println("동적 스케줄러 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 모니터링 템플릿 스케줄러 초기화
     */
    private void initializeMonitoringTemplateScheduler() {
        try {
            String monitoringConfig = systemConfigService.getDashboardMonitoringTemplateConfig();
            if (monitoringConfig == null || monitoringConfig.trim().isEmpty() || monitoringConfig.equals("{}")) {
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(monitoringConfig, Map.class);
            
            String templateId = (String) config.get("templateId");
            if (templateId == null || templateId.trim().isEmpty()) {
                return;
            }
            
            // 템플릿 정보에서 REFRESH_TIMEOUT 조회
            int refreshTimeout = getTemplateRefreshTimeout(templateId);
            
            // 모니터링 템플릿 스케줄러 시작
            startMonitoringTemplateScheduler(refreshTimeout);
        } catch (Exception e) {
            System.err.println("모니터링 템플릿 스케줄러 초기화 실패: " + e.getMessage());
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
        chartConsecutiveFailures.clear();
    }
    
    /**
     * 모든 스케줄러 중지 (차트 + 모니터링 템플릿)
     */
    public void stopAllSchedulers() {
        shutdownSchedulers();
    }

    /**
     * 특정 templateId의 스케줄러 시작
     * 
     * @param templateId   템플릿 ID
     * @param refreshTimeout 새로고침 주기 (초)
     */
    private void startScheduler(String templateId, int refreshTimeout) {
        // 기존 스케줄러가 있으면 중지
        stopScheduler(templateId);

        // 새로운 스케줄러 시작
        ScheduledFuture<?> scheduler = taskScheduler.scheduleAtFixedRate(() -> updateChartData(templateId), refreshTimeout*1000);

        schedulers.put(templateId, scheduler);
    }
    
    /**
     * 모니터링 템플릿 스케줄러 시작
     * 
     * @param refreshTimeout 새로고침 주기 (초)
     */
    private void startMonitoringTemplateScheduler(int refreshTimeout) {
        // 기존 모니터링 템플릿 스케줄러가 있으면 중지
        stopMonitoringTemplateScheduler();

        // 새로운 스케줄러 시작
        ScheduledFuture<?> scheduler = taskScheduler.scheduleAtFixedRate(() -> updateMonitoringTemplateData(), refreshTimeout*1000);

        schedulers.put("monitoring_template", scheduler);
    }
    
    /**
     * 모니터링 템플릿 스케줄러 중지
     */
    private void stopMonitoringTemplateScheduler() {
        ScheduledFuture<?> scheduler = schedulers.get("monitoring_template");
        if (scheduler != null && !scheduler.isCancelled()) {
            scheduler.cancel(false);
            schedulers.remove("monitoring_template");
        }
    }

    /**
     * 특정 templateId의 스케줄러 중지
     * 
     * @param templateId 템플릿 ID
     */
    private void stopScheduler(String templateId) {
        ScheduledFuture<?> scheduler = schedulers.get(templateId);
        if (scheduler != null && !scheduler.isCancelled()) {
            scheduler.cancel(false);
            schedulers.remove(templateId);
        }
    }

    /**
     * 특정 templateId의 데이터 업데이트 (모든 chartType에 대해)
     * 
     * @param templateId 템플릿 ID
     */
    private void updateChartData(String templateId) {
        try {
            // 해당 templateId의 모든 차트 설정 조회
            List<Map<String, Object>> chartConfigs = getChartConfigsByTemplateId(templateId);
            if (chartConfigs == null || chartConfigs.isEmpty()) {
                System.err.println("❌ 차트 설정을 찾을 수 없습니다: " + templateId);
                return;
            }

            // 활성화된 연결 ID 목록 조회
            List<String> connectionIds = getActiveConnectionIds();
            
            // 연결된 DB가 없으면 조회 시도하지 않음
            if (connectionIds.isEmpty()) {
                return;
            }
            
            // 템플릿 실행 (한 번만 실행하여 모든 chartType에 공유)
            Map<String, Object> templateDataByConnection = new HashMap<>();
            
            int maxConsecutive = systemConfigService.getDashboardChartMaxConsecutiveFailures();
            
            for (String connectionId : connectionIds) {
                String statusKey = templateId + "_" + connectionId;
                
                // 연속 실패가 설정 횟수 이상이면 해당 연결 조회 건너뜀 (캐시는 이전 값 유지)
                int priorFailures = chartConsecutiveFailures.getOrDefault(statusKey, 0);
                if (priorFailures >= maxConsecutive) {
                    logger.debug("차트 조회 일시 중단(연속 실패 한도) [{}] {}/{}", statusKey, priorFailures, maxConsecutive);
                    continue;
                }
                
                try {
                    // 템플릿 실행
                    Object chartData = executeTemplateByTemplateId(templateId, connectionId);
                    templateDataByConnection.put(connectionId, chartData);
                    
                    if (chartData instanceof Map && ((Map<?, ?>) chartData).containsKey("error")) {
                        int n = chartConsecutiveFailures.getOrDefault(statusKey, 0) + 1;
                        chartConsecutiveFailures.put(statusKey, n);
                        if (n >= maxConsecutive) {
                            logger.warn("차트 조회 중단 임계 도달 [{}] 연속 실패 {}회 (한도 {})", statusKey, n, maxConsecutive);
                        }
                    } else {
                        chartConsecutiveFailures.remove(statusKey);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ " + templateId + " [" + connectionId + "] 데이터 업데이트 실패: " + e.getMessage());
                    
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "차트 조회 실패: " + e.getMessage());
                    errorResult.put("success", false);
                    templateDataByConnection.put(connectionId, errorResult);
                    int n = chartConsecutiveFailures.getOrDefault(statusKey, 0) + 1;
                    chartConsecutiveFailures.put(statusKey, n);
                    if (n >= maxConsecutive) {
                        logger.warn("차트 조회 중단 임계 도달 [{}] 연속 실패 {}회 (한도 {})", statusKey, n, maxConsecutive);
                    }
                }
            }
            
            // 각 chartType별로 캐시에 저장
            for (Map<String, Object> chartConfig : chartConfigs) {
                String chartType = (String) chartConfig.get("chartType");
                if (chartType == null || chartType.trim().isEmpty()) {
                    continue;
                }
                
                for (String connectionId : connectionIds) {
                    Object chartData = templateDataByConnection.get(connectionId);
                    if (chartData != null) {
                        // 캐시 키: templateId__chart_type__chartType_connectionId
                        String cacheKey = templateId + "__chart_type__" + chartType + "_" + connectionId;
                        chartDataCache.put(cacheKey, chartData);
                        
                        // 알람 저장 (첫 번째 chartType에 대해서만)
                        if (chartConfigs.indexOf(chartConfig) == 0) {
                            saveAlarmRowsIfNeeded(templateId, chartConfig, connectionId, chartData);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ " + templateId + " 데이터 업데이트 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveAlarmRowsIfNeeded(String templateId, Map<String, Object> chartInfo, String connectionId, Object chartData) {
        if (!(chartData instanceof Map<?, ?>)) {
            return;
        }

        Map<?, ?> chartDataMap = (Map<?, ?>) chartData;
        Object result = chartDataMap.get("result");
        if (!(result instanceof List<?>)) {
            return;
        }

        String chartName = getChartName(templateId, chartInfo);
        for (Object rowObject : (List<?>) result) {
            if (!(rowObject instanceof List<?>)) {
                continue;
            }
            List<?> row = (List<?>) rowObject;
            if (row.isEmpty()) {
                continue;
            }

            String severityValue = toStringOrNull(row.get(0));
            if (severityValue == null || !ALARM_SEVERITY_VALUE.equals(severityValue.trim())) {
                continue;
            }

            String column1Value = toStringOrNull(getRowValue(row, 0));
            String column2Value = toStringOrNull(getRowValue(row, 1));
            String column3Value = toStringOrNull(getRowValue(row, 2));

            try {
                java.sql.Timestamp checkedTimestamp = new java.sql.Timestamp(System.currentTimeMillis());
                jdbcTemplate.update(
                        INSERT_DASHBOARD_ALARM_LOG_SQL,
                        connectionId,
                        chartName,
                        column1Value,
                        column2Value,
                        column3Value,
                        checkedTimestamp
                );
            } catch (Exception e) {
                logger.warn("대시보드 알람 로그 저장 실패 [{}][{}]: {}", templateId, connectionId, e.getMessage());
            }
        }
    }

    private Object getRowValue(List<?> row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String getChartName(String templateId, Map<String, Object> chartInfo) {
        // templateName은 서버에서 동적 조회하므로 여기서는 templateId 반환
        return templateId;
    }

    /**
     * templateId로 차트 설정 목록 조회 (같은 templateId의 모든 chartType)
     * 
     * @param templateId 템플릿 ID
     * @return 차트 설정 목록
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getChartConfigsByTemplateId(String templateId) {
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
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> chart : charts) {
                    String chartTemplateId = (String) chart.get("templateId");
                    if (templateId.equals(chartTemplateId)) {
                        result.add(chart);
                    }
                }
                return result.isEmpty() ? null : result;
            }
            return null;
        } catch (Exception e) {
            System.err.println("차트 설정 조회 실패: " + e.getMessage());
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
                @SuppressWarnings("unchecked")
                List<Object> rowbody = (List<Object>) sqlResult.get("rowbody");
                if (rowbody != null && !rowbody.isEmpty() && rowbody.get(0) instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> firstRow = (List<Object>) rowbody.get(0);
                    if (!firstRow.isEmpty()) {
                        errorResult.put("error", firstRow.get(0));
                    } else {
                        errorResult.put("error", "SQL 실행 실패");
                    }
                } else {
                    errorResult.put("error", "SQL 실행 실패");
                }
                return errorResult;
            }
            
            // 성공 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("templateId", templateId);
            result.put("result", sqlResult.get("rowbody"));
            result.put("rowhead", sqlResult.get("rowhead"));  // 컬럼 헤더 정보 추가
           
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
     * @param templateId 템플릿 ID
     * @param chartType 차트 타입
     * @param connectionId 연결 ID
     * @return 차트 데이터
     */
    public Object getChartData(String templateId, String chartType, String connectionId) {
        String cacheKey = templateId + "__chart_type__" + chartType + "_" + connectionId;
        return chartDataCache.get(cacheKey);
    }
    
    /**
     * 캐시된 차트 데이터 조회 (하위 호환성을 위한 오버로드)
     * 
     * @param chartId 차트 ID (구식, 사용 중단 예정)
     * @param connectionId 연결 ID
     * @return 차트 데이터
     * @deprecated templateId와 chartType을 사용하는 메서드를 사용하세요
     */
    @Deprecated
    public Object getChartData(String chartId, String connectionId) {
        // 기존 코드 호환성을 위해 chartId에서 templateId와 chartType 추출 시도
        // chartId가 templateId__chart_type__chartType 형식인 경우
        if (chartId.contains("__chart_type__")) {
            String[] parts = chartId.split("__chart_type__");
            if (parts.length == 2) {
                return getChartData(parts[0], parts[1], connectionId);
            }
        }
        // 구식 키 형식 시도
        String cacheKey = chartId + "_" + connectionId;
        return chartDataCache.get(cacheKey);
    }
    
    /**
     * 모니터링 템플릿 데이터 업데이트
     */
    private void updateMonitoringTemplateData() {
        try {
            String monitoringConfig = systemConfigService.getDashboardMonitoringTemplateConfig();
            if (monitoringConfig == null || monitoringConfig.trim().isEmpty() || monitoringConfig.equals("{}")) {
                Map<String, Object> noConfigResult = new HashMap<>();
                noConfigResult.put("success", false);
                noConfigResult.put("error", "모니터링 템플릿 설정이 없습니다. 대시보드 설정에서 모니터링 템플릿을 구성해주세요.");
                noConfigResult.put("hasConfig", false);
                chartDataCache.put("monitoring_template", noConfigResult);
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(monitoringConfig, Map.class);
            
            String templateId = (String) config.get("templateId");
            String connectionId = (String) config.get("connectionId");
            
            if (templateId == null || templateId.trim().isEmpty() || connectionId == null || connectionId.trim().isEmpty()) {
                Map<String, Object> invalidConfigResult = new HashMap<>();
                invalidConfigResult.put("success", false);
                invalidConfigResult.put("error", "모니터링 템플릿 설정이 올바르지 않습니다. templateId 또는 connectionId가 설정되지 않았습니다.");
                invalidConfigResult.put("hasConfig", true);
                if (templateId == null || templateId.trim().isEmpty()) {
                    invalidConfigResult.put("error", "모니터링 템플릿 설정이 올바르지 않습니다. templateId가 설정되지 않았습니다.");
                } else if (connectionId == null || connectionId.trim().isEmpty()) {
                    invalidConfigResult.put("error", "모니터링 템플릿 설정이 올바르지 않습니다. connectionId가 설정되지 않았습니다.");
                }
                chartDataCache.put("monitoring_template", invalidConfigResult);
                logger.warn("모니터링 템플릿 설정이 올바르지 않습니다: templateId={}, connectionId={}", templateId, connectionId);
                return;
            }
            
            // 연결 상태 확인
            List<String> activeConnectionIds = common.ConnectionnList();
            if (!activeConnectionIds.contains(connectionId)) {
                Map<String, Object> inactiveResult = new HashMap<>();
                inactiveResult.put("success", false);
                inactiveResult.put("error", "연결이 ACTIVE 상태가 아닙니다: " + connectionId);
                inactiveResult.put("hasConfig", true);
                inactiveResult.put("connectionId", connectionId);
                chartDataCache.put("monitoring_template", inactiveResult);
                logger.debug("모니터링 템플릿 연결이 ACTIVE 상태가 아닙니다: {}", connectionId);
                return;
            }
            
            String monitoringStatusKey = "monitoring_template_" + templateId + "_" + connectionId;
            int maxConsecutive = systemConfigService.getDashboardChartMaxConsecutiveFailures();
            int priorMonFailures = chartConsecutiveFailures.getOrDefault(monitoringStatusKey, 0);
            if (priorMonFailures >= maxConsecutive) {
                logger.debug("모니터링 템플릿 조회 일시 중단(연속 실패 한도) [{}] {}/{}", monitoringStatusKey, priorMonFailures, maxConsecutive);
                return;
            }
            
            try {
                // 템플릿 실행
                Object templateData = executeTemplateByTemplateId(templateId, connectionId);
                
                // 캐시에 저장
                chartDataCache.put("monitoring_template", templateData);
                
                if (templateData instanceof Map && ((Map<?, ?>) templateData).containsKey("error")) {
                    int n = chartConsecutiveFailures.getOrDefault(monitoringStatusKey, 0) + 1;
                    chartConsecutiveFailures.put(monitoringStatusKey, n);
                    if (n >= maxConsecutive) {
                        logger.warn("모니터링 템플릿 조회 중단 임계 도달 [{}] 연속 실패 {}회 (한도 {})", monitoringStatusKey, n, maxConsecutive);
                    }
                } else {
                    chartConsecutiveFailures.remove(monitoringStatusKey);
                }
                
            } catch (Exception e) {
                logger.error("❌ 모니터링 템플릿 데이터 업데이트 실패 [{}][{}]: {}", templateId, connectionId, e.getMessage(), e);
                
                // 예외 발생 시에도 에러 데이터를 캐시에 저장
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "모니터링 템플릿 조회 실패: " + e.getMessage());
                errorResult.put("success", false);
                errorResult.put("hasConfig", true);
                errorResult.put("templateId", templateId);
                errorResult.put("connectionId", connectionId);
                chartDataCache.put("monitoring_template", errorResult);
                int n = chartConsecutiveFailures.getOrDefault(monitoringStatusKey, 0) + 1;
                chartConsecutiveFailures.put(monitoringStatusKey, n);
                if (n >= maxConsecutive) {
                    logger.warn("모니터링 템플릿 조회 중단 임계 도달 [{}] 연속 실패 {}회 (한도 {})", monitoringStatusKey, n, maxConsecutive);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ 모니터링 템플릿 데이터 업데이트 중 오류: {}", e.getMessage(), e);
            
            // 최상위 예외 발생 시에도 에러 상태 저장
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "모니터링 템플릿 데이터 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            errorResult.put("hasConfig", true);
            chartDataCache.put("monitoring_template", errorResult);
        }
    }
    
    /**
     * 캐시된 모니터링 템플릿 데이터 조회
     * 
     * @return 모니터링 템플릿 데이터
     */
    public Object getMonitoringTemplateData() {
        return chartDataCache.get("monitoring_template");
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
            chartConsecutiveFailures.clear();
            
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
            
            // 연속 실패 카운터 초기화
            chartConsecutiveFailures.clear();
            
            
            System.out.println("✅ 모든 차트 에러 상태 리셋 완료");
        } catch (Exception e) {
            System.err.println("❌ 에러 상태 리셋 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

