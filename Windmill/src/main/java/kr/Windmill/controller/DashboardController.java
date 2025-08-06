package kr.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.Windmill.service.LogInfoDTO;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.util.Common;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private Common com = new Common();
    
    @Autowired
    private SQLExecuteService sqlExecuteService;

    /**
     * 대시보드 차트 데이터 조회 공통 메서드
     * @param chartName 차트명 (예: applCount, lockWaitCount, activeLog, filesystem)
     */
    @RequestMapping(path = "/Dashboard/{chartName}", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getChartData(
            @PathVariable String chartName, 
            HttpServletRequest request, 
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 차트명을 대문자로 변환하여 SQL 파일명 생성
            String sqlName = chartName.toUpperCase();
            
            // SQL 실행
            Map<String, Object> sqlResult = executeDashboardSQL(sqlName);
            
            if (sqlResult.containsKey("error")) {
                result.put("error", sqlResult.get("error"));
                return result;
            }

            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            result.put("result", rowbody);

        } catch (Exception e) {
            logger.error("{} 실행 오류", chartName.toUpperCase(), e);
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
            logger.warn("대시보드 폴더가 존재하지 않습니다: {}", dashboardPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "대시보드 폴더를 찾을 수 없습니다: " + dashboardPath);
            return result;
        }
        
        File sqlFile = new File(sqlPath);
        File propertiesFile = new File(propertiesPath);

        if (!sqlFile.exists()) {
            logger.warn("SQL 파일을 찾을 수 없습니다: {}", sqlPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "SQL 파일을 찾을 수 없습니다: " + sqlPath);
            return result;
        }

        if (!propertiesFile.exists()) {
            logger.warn("Properties 파일을 찾을 수 없습니다: {}", propertiesPath);
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Properties 파일을 찾을 수 없습니다: " + propertiesPath);
            return result;
        }

        // SQL 파일 읽기
        String sql = com.FileRead(sqlFile);
        
        // Properties 파일에서 연결 정보 읽기
        String propertiesContent = com.FileRead(propertiesFile);
        String connectionName = "local"; // 기본값
        
        for (String line : propertiesContent.split("\r\n")) {
            if (line.startsWith("CONNECTION=")) {
                connectionName = line.split("=")[1];
                break;
            }
        }

        // LogInfoDTO 설정
        LogInfoDTO logInfo = new LogInfoDTO();
        logInfo.setConnection(connectionName);
        logInfo.setPath(sqlPath);
        logInfo.setSql(sql);
        logInfo.setParamList(new ArrayList<>()); // 파라미터 없음
        logInfo.setLimit(1000); // 기본 제한

        // 공통 SQL 실행 서비스 사용
        Map<String, List> sqlResult = sqlExecuteService.executeSQL(logInfo);
        Map<String, Object> result = new HashMap<>();
        result.putAll(sqlResult);
        return result;
    }
} 