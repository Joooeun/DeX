package kr.Windmill.controller;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.log.LogInfoDto;
import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

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
            HttpServletRequest request, 
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 모니터링 로그 기록
            cLog.monitoringLog("DASHBOARD", "차트 데이터 요청: " + chartName + " (해시: " + lastHash + ", 커넥션: " + connectionId + ")");
            
            // 1. 모니터링 활성화 확인 (임시로 비활성화)
            /*
            if (!isAnyMonitoringEnabled()) {
                cLog.monitoringLog("DASHBOARD_MONITORING_DISABLED", "모니터링이 활성화된 연결이 없어 차트 조회를 중단합니다.");
                result.put("error", "모니터링이 활성화된 연결이 없습니다.");
                result.put("errorType", "MONITORING_DISABLED");
                return result;
            }
            */
            
            // 2. DB 기반 차트 설정 확인
            Map<String, Object> chartTemplate = sqlTemplateService.getTemplateByChartMapping(chartName);
            
            if (chartTemplate == null) {
                // DB 기반 설정이 없으면 오류 반환
                cLog.monitoringLog("DASHBOARD_ERROR", "차트 설정을 찾을 수 없음: " + chartName);
                result.put("error", "차트 설정을 찾을 수 없습니다: " + chartName);
                result.put("errorType", "CHART_NOT_FOUND");
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
     * 모니터링이 활성화된 연결이 있는지 확인
     */
    private boolean isAnyMonitoringEnabled() {
        try {
            // 로컬 DB에서 모니터링이 활성화된 연결 정보 조회
            String sql = "SELECT CONNECTION_ID, STATUS, MONITORING_ENABLED FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE'";
            
            // 로컬 DB 연결을 위한 LogInfoDto 설정
            LogInfoDto logInfo = new LogInfoDto();
            logInfo.setConnectionId("local"); // 로컬 DB 연결 ID
            logInfo.setPath("MONITORING_CHECK");
            logInfo.setSql(sql);
            logInfo.setParamList(new ArrayList<>());
            logInfo.setLimit(1000);
            
            Map<String, List> result = sqlExecuteService.executeSQL(logInfo);
            List<Map<String, String>> rowbody = result.get("rowbody");
            
            if (rowbody != null && !rowbody.isEmpty()) {
                // 활성 상태인 연결 중에서 모니터링이 활성화된 연결 확인
                for (Map<String, String> row : rowbody) {
                    String connectionId = row.get("CONNECTION_ID");
                    String monitoringEnabled = row.get("MONITORING_ENABLED");
                    
                    if (connectionId != null && !connectionId.equals("local") && 
                        "TRUE".equalsIgnoreCase(monitoringEnabled)) {
                        cLog.monitoringLog("DASHBOARD_MONITORING_FOUND", "모니터링 활성화된 연결 발견: " + connectionId);
                        return true;
                    }
                }
            }
            
            cLog.monitoringLog("DASHBOARD_MONITORING_NONE", "모니터링 활성화된 연결이 없습니다. 활성 연결 수: " + (rowbody != null ? rowbody.size() : 0));
            return false;
        } catch (Exception e) {
            cLog.monitoringLog("DASHBOARD_MONITORING_CHECK_ERROR", "모니터링 활성화 확인 중 오류: " + e.getMessage());
            return false; // 오류 발생 시 false 반환 (더 안전한 접근)
        }
    }


} 