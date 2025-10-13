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
     * 차트 매핑별 새로고침 주기 조회 (DEPRECATED - 차트 매핑 기능 제거됨)
     * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
     * @return 빈 리스트 반환
     */
    @Deprecated
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
     * 특정 차트 매핑의 새로고침 주기 조회 (DEPRECATED - 차트 매핑 기능 제거됨)
     * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
     * @param chartMapping 차트 매핑 (사용되지 않음)
     * @return 기본값 0 반환
     */
    @Deprecated
    @Select("SELECT REFRESH_TIMEOUT " +
            "FROM SQL_TEMPLATE " +
            "WHERE STATUS = 'ACTIVE' " +
            "AND CHART_MAPPING = #{chartMapping} " +
            "FETCH FIRST 1 ROWS ONLY")
    Integer getRefreshIntervalByChartMapping(String chartMapping);
}

