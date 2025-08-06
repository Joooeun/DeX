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

    @RequestMapping(path = "/Dashboard/applCount", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getApplCount(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            // APPL_COUNT SQL 실행
            Map<String, List> sqlResult = executeDashboardSQL("APPL_COUNT");
            
            if (sqlResult.containsKey("error")) {
                result.put("error", sqlResult.get("error"));
                return result;
            }

            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            List<String> labels = new ArrayList<>();
            List<Integer> data = new ArrayList<>();

            
            result.put("result", rowbody);
            result.put("labels", labels);
            result.put("data", data);

        } catch (Exception e) {
            logger.error("APPL_COUNT 실행 오류", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @RequestMapping(path = "/Dashboard/lockWaitCount", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getLockWaitCount(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            // LOCK_WAIT_COUNT SQL 실행
            Map<String, List> sqlResult = executeDashboardSQL("LOCK_WAIT_COUNT");
            
            if (sqlResult.containsKey("error")) {
                result.put("error", sqlResult.get("error"));
                return result;
            }

            // SQL 결과를 차트 데이터로 변환
            List<Map<String, String>> rowbody = (List<Map<String, String>>) sqlResult.get("rowbody");
            List<String> labels = new ArrayList<>();
            List<Integer> data = new ArrayList<>();
            result.put("result", rowbody);
            result.put("labels", labels);
            result.put("data", data);

        } catch (Exception e) {
            logger.error("LOCK_WAIT_COUNT 실행 오류", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 대시보드 SQL 실행 공통 메서드
     */
    private Map<String, List> executeDashboardSQL(String sqlName) throws Exception {
        // SQL 파일 경로 설정
        String sqlPath = "/Users/jooeunpark/git/DeX/Menu/src/001_DashBoard/" + sqlName + ".sql";
        String propertiesPath = "/Users/jooeunpark/git/DeX/Menu/src/001_DashBoard/" + sqlName + ".properties";
        
        File sqlFile = new File(sqlPath);
        File propertiesFile = new File(propertiesPath);

        if (!sqlFile.exists()) {
            throw new IOException("SQL 파일을 찾을 수 없습니다: " + sqlPath);
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

        // 공통 SQL 실행 서비스 사용 (LogInfoDTO에 이미 memberId와 ip가 설정되어 있음)
        return sqlExecuteService.executeSQL(logInfo);
    }
} 