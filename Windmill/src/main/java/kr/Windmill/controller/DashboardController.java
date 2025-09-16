package kr.Windmill.controller;

import java.security.MessageDigest;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final Common com;
    private final Log cLog;
    
    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    private SqlTemplateService sqlTemplateService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public DashboardController(Common common, Log log) {
        this.com = common;
        this.cLog = log;
    }

    /**
     * 대시보드 차트 데이터 조회 공통 메서드 (해시 비교 방식)
     * DB 기반 차트 설정을 우선 확인하고, 없으면 파일 기반으로 폴백
     * @param chartName 차트명 (예: applCount, lockWaitCount, activeLog, filesystem)
     * @param lastHash 클라이언트가 보낸 이전 해시값
     * @param connectionId 선택된 커넥션 ID
     */
    @RequestMapping(path = "/Dashboard/{chartName}", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getChartData(
            @PathVariable String chartName,
            @RequestParam(required = false) String lastHash,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String checkTemplate,
            HttpServletRequest request, 
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 모니터링 로그 기록
            cLog.monitoringLog("DASHBOARD", "차트 데이터 요청: " + chartName + " (해시: " + lastHash + ", 커넥션: " + connectionId + ", checkTemplate: " + checkTemplate + ")");
            
            // 2. DB 기반 차트 설정 확인
            Map<String, Object> chartTemplate = sqlTemplateService.getTemplateByChartMapping(chartName);
            
            if (chartTemplate == null) {
                // DB 기반 설정이 없으면 오류 반환
                cLog.monitoringLog("DASHBOARD_ERROR", "차트 설정을 찾을 수 없음: " + chartName);
                result.put("error", "차트 설정을 찾을 수 없습니다: " + chartName);
                result.put("errorType", "CHART_NOT_FOUND");
                return result;
            }
            
            // checkTemplate=true인 경우 템플릿 정보만 반환
            if ("true".equals(checkTemplate)) {
                cLog.monitoringLog("DASHBOARD_TEMPLATE_INFO", "템플릿 정보만 반환: " + chartName);
                result.put("success", true);
                result.put("template", chartTemplate);
                return result;
            }
            
            // 3. SQL 템플릿 실행
            String templateId = (String) chartTemplate.get("TEMPLATE_ID");
            String targetConnectionId = connectionId;
            
            // 연결 ID가 없으면 템플릿의 접근 가능한 연결 중 첫 번째 사용
            if (targetConnectionId == null || targetConnectionId.trim().isEmpty()) {
                Object accessibleConnectionsObj = chartTemplate.get("accessibleConnections");
                if (accessibleConnectionsObj instanceof List && !((List<?>) accessibleConnectionsObj).isEmpty()) {
                    targetConnectionId = (String) ((List<?>) accessibleConnectionsObj).get(0);
                } else {
                    result.put("error", "차트 실행을 위한 연결 ID가 지정되지 않았습니다.");
                    return result;
                }
            }
            
            // SqlTemplateExecuteDto 생성
            SqlTemplateExecuteDto executeDto = new SqlTemplateExecuteDto();
            executeDto.setTemplateId(templateId);
            executeDto.setConnectionId(targetConnectionId);
            executeDto.setLimit(1000); // 대시보드용으로 충분한 행 수 설정
            
            // SQL 실행
            Map<String, List> sqlResult = sqlExecuteService.executeTemplateSQL(executeDto);
            
            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            
            // SQL 에러 체크 (단순화)
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
            
            // 현재 데이터의 해시값 생성
            String currentHash = generateHash(rowbody);
            
            // 템플릿 ID 추가
            result.put("templateId", templateId);
            
            // 템플릿 정보 추가 (새로고침 간격 포함)
            result.put("template", chartTemplate);
            
            // 해시값 비교
            if (currentHash.equals(lastHash)) {
                // 데이터가 변경되지 않음
                cLog.monitoringLog("DASHBOARD_CACHE", "차트 " + chartName + " 데이터 변경 없음 (캐시 사용)");
                result.put("changed", false);
                result.put("hash", currentHash);
            } else {
                // 데이터가 변경됨
                cLog.monitoringLog("DASHBOARD_UPDATE", "차트 " + chartName + " 데이터 업데이트 (행 수: " + rowbody.size() + ")");
                result.put("changed", true);
                result.put("hash", currentHash);
                result.put("result", rowbody);
            }

        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_EXCEPTION", "차트 " + chartName + " 예외 발생: " + e.getMessage());
            result.put("error", "차트 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "EXCEPTION");
        }

        return result;
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
            // 모니터링 로그 기록
            cLog.monitoringLog("DASHBOARD", "메뉴 실행 기록 조회 요청");
            
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
            
            cLog.monitoringLog("DASHBOARD", "메뉴 실행 기록 조회 완료: " + formattedLogs.size() + "건");
            
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_ERROR", "메뉴 실행 기록 조회 실패: " + e.getMessage());
            result.put("success", false);
            result.put("error", "메뉴 실행 기록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
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
                cLog.monitoringLog("DASHBOARD_MONITORING_FOUND", "모니터링 활성화된 연결 발견: 총 " + connections.size() + "개");
                return true;
            }
            
            cLog.monitoringLog("DASHBOARD_MONITORING_NONE", "모니터링 활성화된 연결이 없습니다.");
            return false;
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_MONITORING_CHECK_ERROR", "모니터링 활성화 확인 중 오류: " + e.getMessage());
            return false; // 오류 발생 시 false 반환 (더 안전한 접근)
        }
    }


} 