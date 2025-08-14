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
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
public class DashboardController {

    private final Common com;
    private final Log cLog;
    
    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    public DashboardController(Common common, Log log) {
        this.com = common;
        this.cLog = log;
    }

    /**
     * 대시보드 차트 데이터 조회 공통 메서드 (해시 비교 방식)
     * @param chartName 차트명 (예: applCount, lockWaitCount, activeLog, filesystem)
     * @param lastHash 클라이언트가 보낸 이전 해시값
     */
    @RequestMapping(path = "/Dashboard/{chartName}", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getChartData(
            @PathVariable String chartName,
            @RequestParam(required = false) String lastHash,
            HttpServletRequest request, 
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 차트명을 대문자로 변환하여 SQL 파일명 생성
            String sqlName = chartName.toUpperCase();
            
            // 모니터링 로그 기록
            cLog.monitoringLog("DASHBOARD", "차트 데이터 요청: " + chartName + " (해시: " + lastHash + ")");
            
            // SQL 실행
            Map<String, Object> sqlResult = executeDashboardSQL(sqlName);
            
            if (sqlResult.containsKey("error")) {
                cLog.monitoringLog("DASHBOARD_ERROR", "차트 " + chartName + " 실행 오류: " + sqlResult.get("error"));
                result.put("error", sqlResult.get("error"));
                return result;
            }

            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            
            // 현재 데이터의 해시값 생성
            String currentHash = generateHash(rowbody);
            
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
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 대시보드 SQL 실행 공통 메서드
     */
    private Map<String, Object> executeDashboardSQL(String sqlName) throws Exception {
        // SQL 파일 경로 설정 (Common 클래스의 SrcPath 사용)
        String dashboardPath = com.getSrcPath() + "001_DashBoard/";
        String sqlPath = dashboardPath + sqlName + ".sql";
        String propertiesPath = dashboardPath + sqlName + ".properties";
        
        // 001_DashBoard 폴더 존재 여부 확인
        File dashboardDir = new File(dashboardPath);
        if (!dashboardDir.exists() || !dashboardDir.isDirectory()) {
            cLog.monitoringLog("DASHBOARD_PATH_ERROR", "대시보드 폴더 없음: " + dashboardPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "대시보드 폴더를 찾을 수 없습니다: " + dashboardPath);
            return result;
        }
        
        File sqlFile = new File(sqlPath);
        File propertiesFile = new File(propertiesPath);

        if (!sqlFile.exists()) {
            cLog.monitoringLog("DASHBOARD_FILE_ERROR", "SQL 파일 없음: " + sqlPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "SQL 파일을 찾을 수 없습니다: " + sqlPath);
            return result;
        }

        if (!propertiesFile.exists()) {
            cLog.monitoringLog("DASHBOARD_FILE_ERROR", "Properties 파일 없음: " + propertiesPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Properties 파일을 찾을 수 없습니다: " + propertiesPath);
            return result;
        }

        // SQL 파일 읽기
        String sql = com.FileRead(sqlFile);
        
        // Properties 파일에서 연결 정보 읽기
        String propertiesContent = com.FileRead(propertiesFile);
        String connectionId = "local"; // 기본값
        
        for (String line : propertiesContent.split("\r\n")) {
            if (line.startsWith("CONNECTION=")) {
                connectionId = line.split("=")[1];
                break;
            }
        }

        // LogInfoDto 설정
        LogInfoDto logInfo = new LogInfoDto();
        logInfo.setConnectionId(connectionId);
        logInfo.setPath(sqlPath);
        logInfo.setSql(sql);
        logInfo.setParamList(new ArrayList<>()); // 파라미터 없음
        logInfo.setLimit(1000); // 기본 제한

        // 공통 SQL 실행 서비스 사용
        cLog.monitoringLog("DASHBOARD_SQL_EXEC", "SQL 실행 시작: " + sqlName + " (연결: " + connectionId + ")");
        Map<String, List> sqlResult = sqlExecuteService.executeSQL(logInfo);
        Map<String, Object> result = new HashMap<>();
        result.putAll(sqlResult);
        
        // SQL 실행 결과 로그
        if (sqlResult.containsKey("error")) {
            cLog.monitoringLog("DASHBOARD_SQL_ERROR", "SQL 실행 실패: " + sqlName + " - " + sqlResult.get("error"));
        } else {
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            cLog.monitoringLog("DASHBOARD_SQL_SUCCESS", "SQL 실행 성공: " + sqlName + " (결과 행 수: " + (rowbody != null ? rowbody.size() : 0) + ")");
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
} 