package kr.Windmill.service;

import org.springframework.stereotype.Service;

/**
 * 템플릿 설정 서비스
 * SQL_TEMPLATE 테이블에서 차트별 새로고침 주기 조회
 */
@Service
public class TemplateConfigService {
    
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

