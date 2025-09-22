package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import kr.Windmill.mapper.TemplateMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 템플릿 설정 서비스
 * SQL_TEMPLATE 테이블에서 차트별 새로고침 주기 조회
 */
@Service
public class TemplateConfigService {
    
    @Autowired
    private TemplateMapper templateMapper;
    
    /**
     * 차트 매핑별 새로고침 주기 조회
     * @return Map<차트매핑, 새로고침주기(밀리초)>
     */
    public Map<String, Integer> getTemplateRefreshIntervals() {
        try {
            List<TemplateConfig> configs = templateMapper.getChartMappingConfigs();
            
            Map<String, Integer> intervals = new HashMap<>();
            for (TemplateConfig config : configs) {
                intervals.put(config.getChartMapping(), config.getRefreshTimeout()== 0 ? 10 : config.getRefreshTimeout());
            }
            
            return intervals;
        } catch (Exception e) {
            System.err.println("템플릿 설정 조회 실패: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 특정 차트 매핑의 새로고침 주기 조회
     * @param chartMapping 차트 매핑 (APPL_COUNT, LOCK_WAIT_COUNT, ACTIVE_LOG, FILESYSTEM)
     * @return 새로고침 주기 (밀리초), 설정이 없으면 0
     */
    public int getRefreshInterval(String chartMapping) {
        try {
            Integer interval = templateMapper.getRefreshIntervalByChartMapping(chartMapping);
            return interval != 0 ? interval : 10;
        } catch (Exception e) {
            System.err.println("차트 매핑 [" + chartMapping + "] 새로고침 주기 조회 실패: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 템플릿 설정 DTO
     */
    public static class TemplateConfig {
        private String chartMapping;
        private Integer refreshTimeout;
        
        public String getChartMapping() {
            return chartMapping;
        }
        
        public void setChartMapping(String chartMapping) {
            this.chartMapping = chartMapping;
        }
        
        public Integer getRefreshTimeout() {
            return refreshTimeout;
        }
        
        public void setRefreshTimeout(Integer refreshTimeout) {
            this.refreshTimeout = refreshTimeout;
        }
    }
}

