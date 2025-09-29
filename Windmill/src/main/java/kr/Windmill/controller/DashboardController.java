package kr.Windmill.controller;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.service.ConnectionService;
import kr.Windmill.service.DashboardSchedulerService;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.util.Log;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final Log cLog;
    
    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    private DashboardSchedulerService dashboardSchedulerService;
    
    @Autowired
    private ConnectionService connectionService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    public DashboardController(Log log) {
        this.cLog = log;
    }

    
    /**
     * 데이터의 해시값을 생성하는 메서드
     * @param data 해시를 생성할 데이터
     * @return MD5 해시값 (16진수 문자열)
     */
    private String generateHash(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(data);
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(jsonString.getBytes("UTF-8"));
            
            // 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 해시 생성 실패 시 데이터의 hashCode 사용
            return String.valueOf(data.hashCode());
        }
    }

    /**
     * 최근 메뉴 실행 기록 조회 (최근 10개)
     */
    @RequestMapping(path = "/Dashboard/menuExecutionLog", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getMenuExecutionLog(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 모니터링 로그 기록 (요청 시작은 제거)
            
            // 최근 10개 메뉴 실행 기록 조회 (템플릿 이름 포함)
            String sql = "SELECT " +
                "e.LOG_ID, e.USER_ID, e.TEMPLATE_ID, t.TEMPLATE_NAME, e.CONNECTION_ID, e.SQL_TYPE, " +
                "e.EXECUTION_STATUS, e.DURATION, e.AFFECTED_ROWS, e.ERROR_MESSAGE, " +
                "e.EXECUTION_START_TIME, e.EXECUTION_END_TIME " +
                "FROM EXECUTION_LOG e " +
                "LEFT JOIN SQL_TEMPLATE t ON e.TEMPLATE_ID = t.TEMPLATE_ID " +
                "ORDER BY e.EXECUTION_START_TIME DESC " +
                "FETCH FIRST 10 ROWS ONLY";
            
            List<Map<String, Object>> executionLogs = jdbcTemplate.queryForList(sql);
            
            // 결과 포맷팅
            List<Map<String, Object>> formattedLogs = new ArrayList<>();
            for (Map<String, Object> log : executionLogs) {
                Map<String, Object> formattedLog = new HashMap<>();
                formattedLog.put("logId", log.get("LOG_ID"));
                formattedLog.put("userId", log.get("USER_ID"));
                formattedLog.put("templateId", log.get("TEMPLATE_ID"));
                formattedLog.put("templateName", log.get("TEMPLATE_NAME"));
                formattedLog.put("connectionId", log.get("CONNECTION_ID"));
                formattedLog.put("sqlType", log.get("SQL_TYPE"));
                formattedLog.put("executionStatus", log.get("EXECUTION_STATUS"));
                formattedLog.put("duration", log.get("DURATION"));
                formattedLog.put("affectedRows", log.get("AFFECTED_ROWS"));
                formattedLog.put("errorMessage", log.get("ERROR_MESSAGE"));
                formattedLog.put("executionStartTime", log.get("EXECUTION_START_TIME"));
                formattedLog.put("executionEndTime", log.get("EXECUTION_END_TIME"));
                
                // 실행 상태에 따른 색상 설정
                String status = (String) log.get("EXECUTION_STATUS");
                String statusColor = "#666";
                if ("SUCCESS".equals(status)) {
                    statusColor = "#28a745";
                } else if ("FAIL".equals(status)) {
                    statusColor = "#dc3545";
                } else if ("PENDING".equals(status)) {
                    statusColor = "#ffc107";
                }
                formattedLog.put("statusColor", statusColor);
                
                // 실행 시간 포맷팅
                Integer duration = (Integer) log.get("DURATION");
                if (duration != null) {
                    if (duration < 1000) {
                        formattedLog.put("durationText", duration + "ms");
                    } else {
                        formattedLog.put("durationText", String.format("%.1fs", duration / 1000.0));
                    }
                } else {
                    formattedLog.put("durationText", "-");
                }
                
                formattedLogs.add(formattedLog);
            }
            
            result.put("success", true);
            result.put("data", formattedLogs);
            result.put("count", formattedLogs.size());
            
            // 조회 완료 로그 제거 (단순 정보성)
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "메뉴 실행 기록 조회 실패: " + e.getMessage());
            result.put("success", false);
            result.put("error", "메뉴 실행 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 모든 대시보드 데이터 조회 (통합 API)
     * 브라우저에서 호출하여 모든 DB의 모든 차트 데이터를 한 번에 받음
     */
    @RequestMapping(value = "/Dashboard/getAllData", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getAllDashboardData(
            @RequestParam(required = false) String connectionId,
            HttpServletRequest request, 
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 조회 시작 로그 제거 (단순 정보성)
            
            // 연결 정보 조회
            List<String> connectionIds = getActiveConnectionIds();
            
            // 특정 연결이 지정된 경우 해당 연결만 처리
            if (connectionId != null && !connectionId.trim().isEmpty()) {
                connectionIds = connectionIds.contains(connectionId) ? 
                    Arrays.asList(connectionId) : new ArrayList<>();
            }
            
            // 결과 데이터 구성 - 캐시된 데이터 우선 사용
            Map<String, Object> allData = new HashMap<>();
            // 환경설정에서 차트 매핑 목록 가져오기
            String[] chartMappings = getActiveChartMappings();
            
            for (String connId : connectionIds) {
                Map<String, Object> dbData = new HashMap<>();
                dbData.put("connectionId", connId);
                
                // 각 차트별 캐시된 데이터 조회
                for (String chartMapping : chartMappings) {
                    try {
                        // DashboardSchedulerService에서 캐시된 데이터 조회
                        Object cachedData = dashboardSchedulerService.getChartData(chartMapping, connId);
                        if (cachedData != null) {
                            // 캐시된 데이터가 있으면 사용
                            dbData.put(chartMapping, cachedData);
                        } else {
                            Map<String, Object> chartResult = getSingleChartData(chartMapping, connId, session);
                            dbData.put(chartMapping, chartResult);
                        }
                    } catch (Exception e) {
                        cLog.monitoringLog("DASHBOARD_ERROR", "차트 " + chartMapping + " [" + connId + "] 조회 실패: " + e.getMessage());
                        dbData.put(chartMapping, createErrorResult("차트 데이터 조회 실패: " + e.getMessage()));
                    }
                }
                
                allData.put(connId, dbData);
            }
            
            result.put("success", true);
            result.put("data", allData);
            result.put("timestamp", System.currentTimeMillis());
            
            // 조회 완료 로그 제거 (단순 정보성)
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "전체 대시보드 데이터 조회 실패: " + e.getMessage());
            result.put("success", false);
            result.put("error", "대시보드 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 단일 차트 데이터 조회 (내부 메서드)
     */
    private Map<String, Object> getSingleChartData(String chartMapping, String connectionId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 환경설정에서 차트에 해당하는 템플릿 ID 조회
            String templateId = getTemplateIdByChartMapping(chartMapping);
            
            if (templateId == null || templateId.trim().isEmpty()) {
                result.put("error", "차트 설정을 찾을 수 없습니다: " + chartMapping);
                result.put("errorType", "CHART_NOT_FOUND");
                return result;
            }
            String targetConnectionId = connectionId;
            
            // SqlTemplateExecuteDto 생성
            SqlTemplateExecuteDto executeDto = new SqlTemplateExecuteDto();
            executeDto.setTemplateId(templateId);
            executeDto.setConnectionId(targetConnectionId);
            executeDto.setLimit(1000);
            
            // SQL 실행
            Map<String, List> sqlResult = sqlExecuteService.executeTemplateSQL(executeDto);
            
            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            
            // SQL 에러 체크
            if (sqlResult.containsKey("error")) {
                result.put("error", sqlResult.get("error"));
                return result;
            }
            
            // success 필드로 성공 여부 확인
            List<Boolean> successList = (List<Boolean>) sqlResult.get("success");
            if (successList != null && !successList.isEmpty() && !successList.get(0)) {
                result.put("error", "SQL 실행 오류가 발생했습니다.");
                return result;
            }
            
            // 성공 결과 반환
            result.put("success", true);
            result.put("templateId", templateId);
            result.put("result", rowbody);
            result.put("hash", generateHash(rowbody));
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "차트 " + chartMapping + " [" + connectionId + "] 조회 실패: " + e.getMessage());
            result.put("error", "차트 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "EXCEPTION");
        }
        
        return result;
    }
    
    /**
     * 에러 결과 생성
     */
    private Map<String, Object> createErrorResult(String errorMessage) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", errorMessage);
        errorResult.put("errorType", "GENERAL_ERROR");
        return errorResult;
    }
    
    /**
     * 차트 매핑으로 템플릿 ID 조회
     */
    private String getTemplateIdByChartMapping(String chartMapping) {
        try {
            // 환경설정에서 대시보드 차트 설정 조회
            String dashboardChartsJson = systemConfigService.getConfigValue("DASHBOARD_CHARTS", "[]");
            
            // JSON 파싱하여 해당 차트의 템플릿 ID 조회
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, String>> charts = mapper.readValue(dashboardChartsJson, new TypeReference<List<Map<String, String>>>() {});
                
                for (Map<String, String> chart : charts) {
                    String chartId = chart.get("id");
                    if (chartMapping.equals(chartId)) {
                        // 차트 ID와 매핑되는 템플릿 ID 반환 (차트 ID를 템플릿 ID로 사용)
                        return chartId;
                    }
                }
            } catch (Exception e) {
                logger.warn("대시보드 차트 설정 JSON 파싱 실패: " + e.getMessage());
            }
            
            // 환경설정에서 찾지 못한 경우 기본 매핑 사용
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
                    return chartMapping; // 차트 ID를 그대로 템플릿 ID로 사용
            }
        } catch (Exception e) {
            logger.error("차트 매핑 템플릿 ID 조회 실패: " + chartMapping, e);
            return chartMapping; // 오류 시 차트 ID를 그대로 사용
        }
    }

    /**
     * 활성 차트 매핑 목록 조회 (환경설정에서)
     */
    private String[] getActiveChartMappings() {
        try {
            // 환경설정에서 대시보드 차트 설정 조회
            String dashboardChartsJson = systemConfigService.getConfigValue("DASHBOARD_CHARTS", "[]");
            
            // JSON 파싱하여 차트 ID 목록 생성
            List<String> chartIds = new ArrayList<>();
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, String>> charts = mapper.readValue(dashboardChartsJson, new TypeReference<List<Map<String, String>>>() {});
                
                for (Map<String, String> chart : charts) {
                    String chartId = chart.get("id");
                    if (chartId != null && !chartId.trim().isEmpty()) {
                        chartIds.add(chartId);
                    }
                }
            } catch (Exception e) {
                logger.warn("대시보드 차트 설정 JSON 파싱 실패, 기본 설정 사용: " + e.getMessage());
            }
            
            // 설정된 차트가 없으면 기본 차트 사용
            if (chartIds.isEmpty()) {
                return new String[]{"APPL_COUNT", "LOCK_WAIT_COUNT", "ACTIVE_LOG", "FILESYSTEM"};
            }
            
            return chartIds.toArray(new String[0]);
        } catch (Exception e) {
            logger.error("활성 차트 매핑 조회 실패, 기본 설정 사용", e);
            return new String[]{"APPL_COUNT", "LOCK_WAIT_COUNT", "ACTIVE_LOG", "FILESYSTEM"};
        }
    }

    /**
     * 활성화된 연결 ID 목록 조회 (연결 상태가 online인 것만)
     */
    private List<String> getActiveConnectionIds() {
        try {
            // ConnectionService에서 온라인 연결 ID 목록 조회
            return connectionService.getOnlineConnectionIds();
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "연결 ID 목록 조회 실패: " + e.getMessage());
            // 최후의 수단으로 빈 리스트 반환 (연결 상태를 확인할 수 없으므로)
            return new ArrayList<>();
        }
    }

    /**
     * 모니터링이 활성화된 연결이 있는지 확인
     */
    private boolean isAnyMonitoringEnabled() {
        try {
            // 로컬 DB에서 직접 모니터링이 활성화된 연결 정보 조회
            String sql = "SELECT CONNECTION_ID, MONITORING_ENABLED FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' AND MONITORING_ENABLED = true";
            List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);
            
            if (connections != null && !connections.isEmpty()) {
                // 모니터링 연결 발견 로그 제거 (단순 정보성)
                return true;
            }
            
            // 모니터링 연결 없음 로그 제거 (단순 정보성)
            return false;
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_MONITORING_CHECK_ERROR", "모니터링 활성화 확인 중 오류: " + e.getMessage());
            return false; // 오류 발생 시 false 반환 (더 안전한 접근)
        }
    }


} 