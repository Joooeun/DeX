package kr.Windmill.controller;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.dto.connection.ConnectionStatusDto;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.DashboardSchedulerService;
import kr.Windmill.service.ConnectionService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.util.Log;

@Controller
public class DashboardController {

    private final Log cLog;
    
    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    private SqlTemplateService sqlTemplateService;
    
    @Autowired
    private DashboardSchedulerService dashboardSchedulerService;
    
    @Autowired
    private ConnectionService connectionService;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
     * 연결상태 + 차트정보 통합 조회 API
     */
    @RequestMapping(value = "/Dashboard/getIntegratedData", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getIntegratedData(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 사용자 ID 가져오기
            String userId = (String) session.getAttribute("memberId");
            if (userId == null) {
                result.put("success", false);
                result.put("error", "로그인이 필요합니다.");
                return result;
            }
            
            // 1. 연결 상태 조회 (사용자 권한에 따라 필터링)
            List<ConnectionStatusDto> connections = connectionService.getConnectionStatusesForUser(userId);
            
            // 2. 차트 설정 조회
            String chartConfig = systemConfigService.getDashboardChartConfig();
            Map<String, Object> allData = new HashMap<>();
            
            if (chartConfig != null && !chartConfig.trim().isEmpty() && !chartConfig.equals("{}")) {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = mapper.readValue(chartConfig, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> charts = (List<Map<String, Object>>) config.get("charts");
                
                if (charts != null) {
                    for (Map<String, Object> chart : charts) {
                        String chartId = (String) chart.get("id");
                        
                        for (ConnectionStatusDto conn : connections) {
                            String connectionId = conn.getConnectionId();
                            
                            // 캐시된 차트 데이터 조회
                        Object cachedData = dashboardSchedulerService.getChartData(chartId, connectionId);
                        if (cachedData != null) {
                            String dataKey = connectionId + "_charts";
                            if (!allData.containsKey(dataKey)) {
                                allData.put(dataKey, new HashMap<String, Object>());
                            }
                            @SuppressWarnings("unchecked")
                            Map<String, Object> connectionCharts = (Map<String, Object>) allData.get(dataKey);
                            connectionCharts.put(chartId, cachedData);
                        }
                        }
                    }
                }
            }
            
            // 3. 응답 구조 생성
            List<Map<String, Object>> connectionList = new ArrayList<>();
            for (ConnectionStatusDto conn : connections) {
                Map<String, Object> connectionData = new HashMap<>();
                connectionData.put("connectionId", conn.getConnectionId());
                connectionData.put("status", conn.getStatus());
                connectionData.put("connectionName", conn.getConnectionId()); // 연결 이름은 connectionId 사용
                
                // 해당 연결의 차트 데이터 추가
                String dataKey = conn.getConnectionId() + "_charts";
                if (allData.containsKey(dataKey)) {
                    connectionData.put("charts", allData.get(dataKey));
                } else {
                    connectionData.put("charts", new HashMap<String, Object>());
                }
                
                connectionList.add(connectionData);
            }
            
            result.put("success", true);
            result.put("connections", connectionList);
            
            // 차트 설정을 파싱하여 객체로 전달
            if (chartConfig != null && !chartConfig.trim().isEmpty() && !chartConfig.equals("{}")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Object parsedConfig = mapper.readValue(chartConfig, Object.class);
                    result.put("chartConfig", parsedConfig);
                } catch (Exception e) {
                    result.put("chartConfig", null);
                }
            } else {
                result.put("chartConfig", null);
            }
            
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "통합 데이터 조회 실패: " + e.getMessage());
            result.put("success", false);
            result.put("error", "통합 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 모니터링 템플릿 데이터 조회
     */
    @RequestMapping(path = "/Dashboard/monitoringTemplate", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getMonitoringTemplateData(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 모니터링 템플릿 설정 확인
            String monitoringConfig = systemConfigService.getDashboardMonitoringTemplateConfig();
            if (monitoringConfig == null || monitoringConfig.trim().isEmpty() || monitoringConfig.equals("{}")) {
                result.put("success", true);
                result.put("data", null);
                result.put("hasConfig", false);
                return result;
            }
            
            // 캐시된 모니터링 템플릿 데이터 조회
            Object monitoringData = dashboardSchedulerService.getMonitoringTemplateData();
            
            result.put("success", true);
            result.put("data", monitoringData);
            result.put("hasConfig", true);
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "모니터링 템플릿 데이터 조회 실패: " + e.getMessage());
            result.put("success", false);
            result.put("error", "모니터링 템플릿 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
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
            // 차트 매핑 기능이 제거됨 - 빈 배열 사용
            String[] chartMappings = {};
            
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
     * 단일 차트 데이터 조회 (DEPRECATED - 차트 매핑 기능 제거됨)
     * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
     */
    @Deprecated
    private Map<String, Object> getSingleChartData(String chartMapping, String connectionId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        // 차트 매핑 기능이 제거되어 에러 반환
        result.put("error", "차트 매핑 기능이 제거되었습니다: " + chartMapping);
        result.put("errorType", "CHART_MAPPING_DEPRECATED");
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



} 