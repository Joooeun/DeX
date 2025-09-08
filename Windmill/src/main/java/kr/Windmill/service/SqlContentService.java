package kr.Windmill.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SqlContentService {

    private static final Logger logger = LoggerFactory.getLogger(SqlContentService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 템플릿의 모든 추가 SQL 내용 조회 (기본 템플릿 제외)
     */
    public List<Map<String, Object>> getSqlContentsByTemplate(String templateId) {
        String sql = "SELECT TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, " +
                    "CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP " +
                    "FROM SQL_CONTENT WHERE TEMPLATE_ID = ? ORDER BY VERSION DESC";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId);
        
        // 각 SQL 내용에 연결 존재 여부 정보 추가
        for (Map<String, Object> result : results) {
            String connectionId = (String) result.get("CONNECTION_ID");
            if (connectionId != null && !connectionId.trim().isEmpty()) {
                try {
                    boolean connectionExists = isConnectionExists(connectionId);
                    result.put("CONNECTION_EXISTS", connectionExists);
                } catch (Exception e) {
                    logger.warn("연결 존재 여부 확인 실패: {} - {}", connectionId, e.getMessage());
                    result.put("CONNECTION_EXISTS", true); // 확인 실패 시 기본값으로 true 설정
                }
            } else {
                result.put("CONNECTION_EXISTS", true); // 기본 템플릿은 항상 존재
            }
        }
        
        return results;
    }

    /**
     * 특정 연결 ID의 SQL 내용 조회 (기본 템플릿 우선, 없으면 추가 SQL 내용)
     * CONNECTION_ID는 쉼표로 구분된 여러 ID를 포함할 수 있음
     */
    public Map<String, Object> getSqlContentByTemplateAndConnectionId(String templateId, String connectionId) {
        // 1. 먼저 해당 연결 ID가 포함된 추가 SQL 내용 조회 (쉼표 구분 방식)
        String sql = "SELECT TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, " +
                    "CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP " +
                    "FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND " +
                    "(CONNECTION_ID = ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ?)";
        
        String likeStart = connectionId + ",%";
        String likeEnd = "%," + connectionId;
        String likeMiddle = "%," + connectionId + ",%";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId, connectionId, likeStart, likeEnd, likeMiddle);
        
        // 2. 일치하는 연결 ID가 있으면 반환
        if (!results.isEmpty()) {
            return results.get(0);
        }
        
        // 3. 일치하는 것이 없으면 기본 템플릿 조회
        return getDefaultSqlContent(templateId);
    }
    
    /**
     * 특정 DB 타입의 SQL 내용 조회 (기본 템플릿 우선, 없으면 추가 SQL 내용)
     * @deprecated CONNECTION_ID 기반으로 변경됨. getSqlContentByTemplateAndConnectionId 사용 권장
     */
    @Deprecated
    public Map<String, Object> getSqlContentByTemplateAndDbType(String templateId, String dbType) {
        // DB 타입을 연결 ID로 변환 (실제 구현에서는 DB 타입과 연결 ID 매핑 테이블 필요)
        String connectionId = convertDbTypeToConnectionId(dbType);
        return getSqlContentByTemplateAndConnectionId(templateId, connectionId);
    }
    
    /**
     * DB 타입을 연결 ID로 변환 (임시 구현)
     * 실제 구현에서는 DATABASE_CONNECTION 테이블에서 DB_TYPE으로 연결 ID를 조회해야 함
     */
    private String convertDbTypeToConnectionId(String dbType) {
        if (dbType == null) return null;
        
        // 임시 매핑 - 실제로는 DATABASE_CONNECTION 테이블에서 조회해야 함
        switch (dbType.toUpperCase()) {
            case "ORACLE":
                return "oracle_connection";
            case "DB2":
                return "db2_connection";
            case "MYSQL":
                return "mysql_connection";
            default:
                return "default_connection";
        }
    }

    /**
     * 템플릿의 기본 SQL 내용 조회 (SQL_TEMPLATE.SQL_CONTENT에서 조회)
     */
    public Map<String, Object> getDefaultSqlContent(String templateId) {
        String sql = "SELECT TEMPLATE_ID, SQL_CONTENT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId);
        if (results.isEmpty()) {
            return null;
        }
        
        Map<String, Object> result = results.get(0);
        // 기본 템플릿 형태로 변환 (복합 키 방식)
        Map<String, Object> defaultContent = new HashMap<>();
        defaultContent.put("TEMPLATE_ID", templateId);
        defaultContent.put("CONNECTION_ID", null);  // 기본 템플릿은 NULL
        defaultContent.put("SQL_CONTENT", result.get("SQL_CONTENT"));
        defaultContent.put("VERSION", 1);
        defaultContent.put("CREATED_BY", "SYSTEM");
        defaultContent.put("CREATED_TIMESTAMP", null);
        defaultContent.put("MODIFIED_BY", null);
        defaultContent.put("MODIFIED_TIMESTAMP", null);
        
        return defaultContent;
    }

    /**
     * SQL 내용 저장 (신규/수정) - 추가 SQL 내용만
     */
    @Transactional
    public Map<String, Object> saveSqlContent(String templateId, String connectionId, String sqlContent, String userId) {
        Map<String, Object> result = new HashMap<>();

        if (templateId == null || templateId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "템플릿 ID가 필요합니다.");
            return result;
        }
        if (connectionId == null || connectionId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "연결 ID가 필요합니다.");
            return result;
        }
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 내용이 필요합니다.");
            return result;
        }

        // 복합 키 (TEMPLATE_ID, CONNECTION_ID)로 INSERT 또는 UPDATE
        // 기존에 같은 템플릿-연결ID 조합이 있는지 확인 (쉼표 구분 방식)
        String checkSql = "SELECT COUNT(*) FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND " +
                        "(CONNECTION_ID = ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ?)";
        String likeStart = connectionId + ",%";
        String likeEnd = "%," + connectionId;
        String likeMiddle = "%," + connectionId + ",%";
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, templateId, connectionId, likeStart, likeEnd, likeMiddle);
        
        if (count > 0) {
            // 기존 데이터가 있으면 UPDATE
            String updateSql = "UPDATE SQL_CONTENT SET SQL_CONTENT = ?, VERSION = VERSION + 1, " +
                              "MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                              "WHERE TEMPLATE_ID = ? AND " +
                              "(CONNECTION_ID = ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ?)";
            jdbcTemplate.update(updateSql, sqlContent, userId, templateId, connectionId, likeStart, likeEnd, likeMiddle);
            result.put("message", "SQL 내용이 업데이트되었습니다.");
        } else {
            // 기존 데이터가 없으면 INSERT
            String insertSql = "INSERT INTO SQL_CONTENT (TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, CREATED_BY) " +
                             "VALUES (?, ?, ?, 1, ?)";
            jdbcTemplate.update(insertSql, templateId, connectionId, sqlContent, userId);
            result.put("message", "SQL 내용이 저장되었습니다.");
        }

        result.put("success", true);
        result.put("templateId", templateId);
        result.put("connectionId", connectionId);
        
        return result;
    }
    
    /**
     * SQL 내용 저장 (신규/수정) - DB 타입 기반 (하위 호환성)
     * @deprecated CONNECTION_ID 기반으로 변경됨. saveSqlContent(String, String, String, String, String) 사용 권장
     */
    @Deprecated
    public Map<String, Object> saveSqlContentByDbType(String contentId, String templateId, String dbType, 
                                                      String sqlContent, String userId) {
        String connectionId = convertDbTypeToConnectionId(dbType);
        return saveSqlContent(templateId, connectionId, sqlContent, userId);
    }

    /**
     * SQL 내용 삭제 (추가 SQL 내용만)
     */
    @Transactional
    public Map<String, Object> deleteSqlContent(String templateId, String connectionId, String userId) {
        Map<String, Object> result = new HashMap<>();

        if (templateId == null || templateId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "템플릿 ID가 필요합니다.");
            return result;
        }
        if (connectionId == null || connectionId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "연결 ID가 필요합니다.");
            return result;
        }

        // SQL 내용 존재 여부 확인 (쉼표 구분 방식)
        String checkSql = "SELECT COUNT(*) FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND " +
                        "(CONNECTION_ID = ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ?)";
        String likeStart = connectionId + ",%";
        String likeEnd = "%," + connectionId;
        String likeMiddle = "%," + connectionId + ",%";
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, templateId, connectionId, likeStart, likeEnd, likeMiddle);
        
        if (count == 0) {
            result.put("success", false);
            result.put("error", "해당 SQL 내용을 찾을 수 없습니다.");
            return result;
        }

        // SQL 내용 삭제 (쉼표 구분 방식)
        String deleteSql = "DELETE FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND " +
                          "(CONNECTION_ID = ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ? OR CONNECTION_ID LIKE ?)";
        jdbcTemplate.update(deleteSql, templateId, connectionId, likeStart, likeEnd, likeMiddle);

        result.put("success", true);
        result.put("message", "SQL 내용이 삭제되었습니다.");
        
        return result;
    }

    /**
     * 템플릿의 모든 추가 SQL 내용 삭제
     */
    @Transactional
    public void deleteAllSqlContentsByTemplate(String templateId) {
        String deleteSql = "DELETE FROM SQL_CONTENT WHERE TEMPLATE_ID = ?";
        jdbcTemplate.update(deleteSql, templateId);
    }

    
    /**
     * 연결 ID가 존재하는지 확인
     * 
     * @param connectionId 연결 ID
     * @return 연결 존재 여부
     */
    public boolean isConnectionExists(String connectionId) {
        try {
            // DB 연결 확인
            String dbSql = "SELECT COUNT(*) FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
            int dbCount = jdbcTemplate.queryForObject(dbSql, Integer.class, connectionId);
            if (dbCount > 0) {
                return true;
            }
            
            // SFTP 연결 확인
            String sftpSql = "SELECT COUNT(*) FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ?";
            int sftpCount = jdbcTemplate.queryForObject(sftpSql, Integer.class, connectionId);
            return sftpCount > 0;
        } catch (Exception e) {
            logger.debug("연결 존재 여부 확인 실패: {} - {}", connectionId, e.getMessage());
            return false;
        }
    }
}
