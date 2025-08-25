package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SqlContentService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 템플릿의 모든 추가 SQL 내용 조회 (기본 템플릿 제외)
     */
    public List<Map<String, Object>> getSqlContentsByTemplate(String templateId) {
        String sql = "SELECT CONTENT_ID, TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, " +
                    "CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP " +
                    "FROM SQL_CONTENT WHERE TEMPLATE_ID = ? ORDER BY VERSION DESC";
        
        return jdbcTemplate.queryForList(sql, templateId);
    }

    /**
     * 특정 DB 연결의 SQL 내용 조회 (기본 템플릿 우선, 없으면 추가 SQL 내용)
     */
    public Map<String, Object> getSqlContentByTemplateAndConnection(String templateId, String connectionId) {
        // 1. 먼저 해당 연결 ID가 포함된 추가 SQL 내용 조회
        String sql = "SELECT CONTENT_ID, TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, " +
                    "CREATED_BY, CREATED_TIMESTAMP, MODIFIED_BY, MODIFIED_TIMESTAMP " +
                    "FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND CONNECTION_ID LIKE ?";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId, "%" + connectionId + "%");
        
        // 2. 정확히 일치하는 연결 ID 찾기
        for (Map<String, Object> result : results) {
            String contentConnectionIds = (String) result.get("CONNECTION_ID");
            if (contentConnectionIds != null) {
                String[] connectionIds = contentConnectionIds.split(",");
                for (String id : connectionIds) {
                    if (id.trim().equals(connectionId)) {
                        return result;
                    }
                }
            }
        }
        
        // 3. 일치하는 것이 없으면 기본 템플릿 조회
        return getDefaultSqlContent(templateId);
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
        // 기본 템플릿 형태로 변환
        Map<String, Object> defaultContent = new HashMap<>();
        defaultContent.put("CONTENT_ID", "DEFAULT_" + templateId);
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
    public Map<String, Object> saveSqlContent(String contentId, String templateId, String connectionId, 
                                             String sqlContent, String userId) {
        Map<String, Object> result = new HashMap<>();

        if (templateId == null || templateId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "템플릿 ID가 필요합니다.");
            return result;
        }
        if (connectionId == null || connectionId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "DB 연결 ID가 필요합니다.");
            return result;
        }
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 내용이 필요합니다.");
            return result;
        }

        boolean isNew = (contentId == null || contentId.trim().isEmpty());
        
        if (isNew) {
            contentId = "CONTENT_" + templateId + "_" + connectionId.replace(",", "_") + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // 기존에 같은 템플릿-연결 조합이 있는지 확인
            String[] connectionIds = connectionId.split(",");
            for (String connId : connectionIds) {
                String checkSql = "SELECT COUNT(*) FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND CONNECTION_ID LIKE ?";
                int count = jdbcTemplate.queryForObject(checkSql, Integer.class, templateId, "%" + connId.trim() + "%");
                if (count > 0) {
                    result.put("success", false);
                    result.put("error", "이미 해당 DB 연결(" + connId.trim() + ")에 대한 SQL 내용이 존재합니다.");
                    return result;
                }
            }

            String insertSql = "INSERT INTO SQL_CONTENT (CONTENT_ID, TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, CREATED_BY) " +
                             "VALUES (?, ?, ?, ?, 1, ?)";
            jdbcTemplate.update(insertSql, contentId, templateId, connectionId, sqlContent, userId);
        } else {
            String updateSql = "UPDATE SQL_CONTENT SET SQL_CONTENT = ?, VERSION = VERSION + 1, " +
                             "MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                             "WHERE CONTENT_ID = ?";
            jdbcTemplate.update(updateSql, sqlContent, userId, contentId);
        }

        result.put("success", true);
        result.put("message", "SQL 내용이 저장되었습니다.");
        result.put("contentId", contentId);
        
        return result;
    }

    /**
     * SQL 내용 삭제 (추가 SQL 내용만)
     */
    @Transactional
    public Map<String, Object> deleteSqlContent(String contentId, String userId) {
        Map<String, Object> result = new HashMap<>();

        if (contentId == null || contentId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 내용 ID가 필요합니다.");
            return result;
        }

        // 삭제할 SQL 내용이 존재하는지 확인
        String checkSql = "SELECT TEMPLATE_ID FROM SQL_CONTENT WHERE CONTENT_ID = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, contentId);
        
        if (results.isEmpty()) {
            result.put("success", false);
            result.put("error", "해당 SQL 내용을 찾을 수 없습니다.");
            return result;
        }

        // SQL 내용 삭제
        String deleteSql = "DELETE FROM SQL_CONTENT WHERE CONTENT_ID = ?";
        jdbcTemplate.update(deleteSql, contentId);

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
     * DB 연결별 SQL 내용 복사 (추가 SQL 내용만)
     */
    @Transactional
    public Map<String, Object> copySqlContent(String sourceContentId, String targetConnectionId, String userId) {
        Map<String, Object> result = new HashMap<>();

        // 소스 SQL 내용 조회
        String sourceSql = "SELECT TEMPLATE_ID, SQL_CONTENT FROM SQL_CONTENT WHERE CONTENT_ID = ?";
        List<Map<String, Object>> sourceResults = jdbcTemplate.queryForList(sourceSql, sourceContentId);
        
        if (sourceResults.isEmpty()) {
            result.put("success", false);
            result.put("error", "소스 SQL 내용을 찾을 수 없습니다.");
            return result;
        }

        Map<String, Object> sourceContent = sourceResults.get(0);
        String templateId = (String) sourceContent.get("TEMPLATE_ID");
        String sqlContent = (String) sourceContent.get("SQL_CONTENT");

        // 대상 연결에 이미 SQL 내용이 있는지 확인 (콤마로 구분된 연결 ID 처리)
        String[] connectionIds = targetConnectionId.split(",");
        for (String connId : connectionIds) {
            String checkSql = "SELECT COUNT(*) FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND CONNECTION_ID LIKE ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, templateId, "%" + connId.trim() + "%");
            if (count > 0) {
                result.put("success", false);
                result.put("error", "이미 해당 DB 연결(" + connId.trim() + ")에 대한 SQL 내용이 존재합니다.");
                return result;
            }
        }

        // SQL 내용 복사
        String newContentId = "CONTENT_" + templateId + "_" + targetConnectionId.replace(",", "_") + "_" + UUID.randomUUID().toString().substring(0, 8);
        String insertSql = "INSERT INTO SQL_CONTENT (CONTENT_ID, TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, CREATED_BY) " +
                         "VALUES (?, ?, ?, ?, 1, ?)";
        jdbcTemplate.update(insertSql, newContentId, templateId, targetConnectionId, sqlContent, userId);

        result.put("success", true);
        result.put("message", "SQL 내용이 복사되었습니다.");
        result.put("contentId", newContentId);
        
        return result;
    }
}
