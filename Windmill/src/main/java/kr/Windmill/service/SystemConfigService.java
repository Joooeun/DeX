package kr.Windmill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);
    
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
     * 설정값을 업데이트합니다
     */
    public boolean updateConfigValue(String configKey, String configValue) {
        try {
            
            // 먼저 존재 여부 확인
            String checkSql = "SELECT COUNT(*) FROM SYSTEM_CONFIG WHERE CONFIG_KEY = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, configKey);
            
            if (count > 0) {
                // 업데이트
                String updateSql = "UPDATE SYSTEM_CONFIG SET CONFIG_VALUE = ?, UPDATED_DATE = CURRENT_TIMESTAMP WHERE CONFIG_KEY = ?";
                int updated = jdbcTemplate.update(updateSql, configValue, configKey);
                if (updated > 0) {
                    refreshCache();
                    return true;
                }
                return false;
            } else {
                // 삽입
                String insertSql = "INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, CREATED_DATE, UPDATED_DATE) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                int inserted = jdbcTemplate.update(insertSql, configKey, configValue);
                if (inserted > 0) {
                    refreshCache();
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            logger.error("설정값 업데이트 실패 [{}]: {}", configKey, e.getMessage(), e);
            return false;
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
            logger.error("캐시 갱신 실패: {}", e.getMessage(), e);
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
    
    /**
     * 대시보드 차트 설정을 가져옵니다
     */
    public String getDashboardChartConfig() {
        return getConfigValue("DASHBOARD_CHART_CONFIG", "{}");
    }
    
    /**
     * 대시보드 차트 설정을 저장합니다
     */
    public boolean saveDashboardChartConfig(String chartConfig) {
        return updateConfigValue("DASHBOARD_CHART_CONFIG", chartConfig);
    }
    
    /**
     * DASHBOARD_CHART_CONFIG가 없으면 기본값으로 생성합니다
     */
    public void ensureConfigExists() {
        try {
            String sql = "SELECT COUNT(*) FROM SYSTEM_CONFIG WHERE CONFIG_KEY = 'DASHBOARD_CHART_CONFIG'";
            int count = jdbcTemplate.queryForObject(sql, Integer.class);
            
            if (count == 0) {
                // 기본값으로 빈 JSON 객체 삽입
                String insertSql = "INSERT INTO SYSTEM_CONFIG (CONFIG_KEY, CONFIG_VALUE, CREATED_DATE, UPDATED_DATE) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                jdbcTemplate.update(insertSql, "DASHBOARD_CHART_CONFIG", "{}");
                refreshCache();
            }
        } catch (Exception e) {
            logger.error("설정 존재 확인 및 생성 실패: {}", e.getMessage(), e);
        }
    }
}
