package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemConfigService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static Map<String, String> configCache = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000; // 30초
    
    /**
     * 설정값을 가져옵니다 (캐시 사용)
     */
    public String getConfigValue(String configKey) {
        // 캐시가 만료되었으면 갱신
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            refreshCache();
        }
        
        return configCache.get(configKey);
    }
    
    /**
     * 설정값을 가져옵니다 (기본값 포함)
     */
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 정수형 설정값을 가져옵니다
     */
    public int getIntConfigValue(String configKey, int defaultValue) {
        String value = getConfigValue(configKey);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // 파싱 실패 시 기본값 반환
            }
        }
        return defaultValue;
    }
    
    /**
     * 설정값을 업데이트합니다 (없으면 INSERT)
     */
    public boolean updateConfigValue(String configKey, String configValue) {
        try {
            // 먼저 UPDATE 시도
            String updateSql = "UPDATE SYSTEM_CONFIG SET CONFIG_VALUE = ?, UPDATED_DATE = CURRENT_TIMESTAMP WHERE CONFIG_KEY = ?";
            int updated = jdbcTemplate.update(updateSql, configValue, configKey);
            
            if (updated > 0) {
                // 캐시 갱신
                refreshCache();
                return true;
            } else {
                // UPDATE가 실패하면 INSERT 시도
                String insertSql = "INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, CONFIG_DESC, CREATED_DATE, UPDATED_DATE) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                String description = getDefaultDescription(configKey);
                jdbcTemplate.update(insertSql, configKey, configValue, description);
                
                // 캐시 갱신
                refreshCache();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 설정 키에 대한 기본 설명을 반환합니다
     */
    private String getDefaultDescription(String configKey) {
        switch (configKey) {
            case "DASHBOARD_CHART_1_ID":
                return "첫 번째 차트 ID";
            case "DASHBOARD_CHART_1_TYPE":
                return "첫 번째 차트 형식";
            case "DASHBOARD_CHART_2_ID":
                return "두 번째 차트 ID";
            case "DASHBOARD_CHART_2_TYPE":
                return "두 번째 차트 형식";
            case "DASHBOARD_CHART_3_ID":
                return "세 번째 차트 ID";
            case "DASHBOARD_CHART_3_TYPE":
                return "세 번째 차트 형식";
            case "DASHBOARD_CHART_4_ID":
                return "네 번째 차트 ID";
            case "DASHBOARD_CHART_4_TYPE":
                return "네 번째 차트 형식";
            default:
                return "시스템 설정";
        }
    }
    
    /**
     * 캐시를 갱신합니다
     */
    private void refreshCache() {
        try {
            String sql = "SELECT CONFIG_KEY, CONFIG_VALUE FROM SYSTEM_CONFIG";
            jdbcTemplate.query(sql, (rs) -> {
                configCache.put(rs.getString("CONFIG_KEY"), rs.getString("CONFIG_VALUE"));
            });
            lastCacheUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 모든 설정값을 가져옵니다
     */
    public Map<String, String> getAllConfigValues() {
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            refreshCache();
        }
        return new HashMap<>(configCache);
    }
}
