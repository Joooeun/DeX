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
     * 차트 매핑별 새로고침 주기 조회 (DEPRECATED - 차트 매핑 기능 제거됨)
     * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
     * @return 빈 Map 반환
     */
    @Deprecated
    public Map<String, Integer> getTemplateRefreshIntervals() {
        // 차트 매핑 기능이 제거되어 빈 Map 반환
        return new HashMap<>();
    }
    
    /**
     * 특정 차트 매핑의 새로고침 주기 조회 (DEPRECATED - 차트 매핑 기능 제거됨)
     * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
     * @param chartMapping 차트 매핑 (사용되지 않음)
     * @return 기본값 10 반환
     */
    @Deprecated
    public int getRefreshInterval(String chartMapping) {
        // 차트 매핑 기능이 제거되어 기본값 반환
        return 10;
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

