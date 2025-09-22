package kr.Windmill.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import kr.Windmill.service.TemplateConfigService.TemplateConfig;
import java.util.List;

/**
 * 템플릿 관련 MyBatis 매퍼
 */
@Mapper
public interface TemplateMapper {
    
    /**
     * 차트 매핑별 새로고침 주기 조회
     * @return 차트 매핑별 설정 정보
     */
    @Select("SELECT " +
            "    CHART_MAPPING as chartMapping, " +
            "    REFRESH_TIMEOUT as refreshTimeout " +
            "FROM SQL_TEMPLATE " +
            "WHERE STATUS = 'ACTIVE' " +
            "AND CHART_MAPPING IS NOT NULL " +
            "AND CHART_MAPPING IN ('APPL_COUNT', 'LOCK_WAIT_COUNT', 'ACTIVE_LOG', 'FILESYSTEM') " +
            "ORDER BY CHART_MAPPING")
    List<TemplateConfig> getChartMappingConfigs();
    
    /**
     * 특정 차트 매핑의 새로고침 주기 조회
     * @param chartMapping 차트 매핑
     * @return 새로고침 주기 (밀리초)
     */
    @Select("SELECT REFRESH_TIMEOUT " +
            "FROM SQL_TEMPLATE " +
            "WHERE STATUS = 'ACTIVE' " +
            "AND CHART_MAPPING = #{chartMapping} " +
            "FETCH FIRST 1 ROWS ONLY")
    Integer getRefreshIntervalByChartMapping(String chartMapping);
}

