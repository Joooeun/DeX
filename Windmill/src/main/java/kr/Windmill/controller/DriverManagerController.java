package kr.Windmill.controller;

import kr.Windmill.service.DynamicDriverManager;
import kr.Windmill.service.ConnectionPoolManager;
import kr.Windmill.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 동적 드라이버 관리 시스템 테스트 및 모니터링 컨트롤러
 */
@RestController
@RequestMapping("/DriverManager")
public class DriverManagerController {
    
    private static final Logger logger = LoggerFactory.getLogger(DriverManagerController.class);
    
    @Autowired
    private DynamicDriverManager dynamicDriverManager;
    
    @Autowired
    private ConnectionPoolManager connectionPoolManager;
    
    /**
     * 로드된 드라이버 목록 조회
     */
    @RequestMapping(path = "/drivers", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getLoadedDrivers() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, String> drivers = dynamicDriverManager.getLoadedDrivers();
            result.put("success", true);
            result.put("drivers", drivers);
            result.put("count", drivers.size());
            
            logger.info("로드된 드라이버 조회: {}개", drivers.size());
            
        } catch (Exception e) {
            logger.error("드라이버 목록 조회 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 연결 풀 상태 조회
     */
    @RequestMapping(path = "/pools", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getPoolStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Map<String, Object>> poolStatus = connectionPoolManager.getAllPoolStatus();
            result.put("success", true);
            result.put("pools", poolStatus);
            result.put("count", poolStatus.size());
            
            logger.info("연결 풀 상태 조회: {}개", poolStatus.size());
            
        } catch (Exception e) {
            logger.error("연결 풀 상태 조회 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 특정 연결 풀 상태 조회
     */
    @RequestMapping(path = "/pools/{connectionId}", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getPoolStatus(@PathVariable String connectionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> poolStatus = connectionPoolManager.getPoolStatus(connectionId);
            result.put("success", true);
            result.put("pool", poolStatus);
            
            logger.info("연결 풀 상태 조회: {}", connectionId);
            
        } catch (Exception e) {
            logger.error("연결 풀 상태 조회 실패: {}", connectionId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * JAR 파일에서 드라이버 로드
     */
    @RequestMapping(path = "/load", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> loadDriverFromJar(@RequestParam String jarPath) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                result.put("success", false);
                result.put("error", "JAR 파일을 찾을 수 없습니다: " + jarPath);
                return result;
            }
            
            dynamicDriverManager.loadDriverFromJar(jarFile);
            
            result.put("success", true);
            result.put("message", "드라이버 로드 성공: " + jarFile.getName());
            
            logger.info("드라이버 로드 성공: {}", jarFile.getName());
            
        } catch (Exception e) {
            logger.error("드라이버 로드 실패: {}", jarPath, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 드라이버 언로드
     */
    @RequestMapping(path = "/unload", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> unloadDriver(@RequestParam String jarPath) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            dynamicDriverManager.unloadDriver(jarPath);
            
            result.put("success", true);
            result.put("message", "드라이버 언로드 성공: " + jarPath);
            
            logger.info("드라이버 언로드 성공: {}", jarPath);
            
        } catch (Exception e) {
            logger.error("드라이버 언로드 실패: {}", jarPath, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 시스템 정보 조회
     */
    @RequestMapping(path = "/info", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, String> drivers = dynamicDriverManager.getLoadedDrivers();
            Map<String, Map<String, Object>> pools = connectionPoolManager.getAllPoolStatus();
            
            result.put("success", true);
            result.put("loadedDrivers", drivers.size());
            result.put("activePools", pools.size());
            result.put("drivers", drivers);
            result.put("pools", pools);
            
            // 시스템 정보
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("userHome", System.getProperty("user.home"));
            systemInfo.put("driverLibPath", Common.getJdbcDriverPath(""));
            
            result.put("systemInfo", systemInfo);
            
        } catch (Exception e) {
            logger.error("시스템 정보 조회 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}


