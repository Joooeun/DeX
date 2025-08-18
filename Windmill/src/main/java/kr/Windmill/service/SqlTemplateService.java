package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlTemplateService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 관리자용 전체 트리 조회 (카테고리 폴더 + 템플릿 노드)
     */
    public List<Map<String, Object>> getFullMenuTree() {
        List<Map<String, Object>> tree = new ArrayList<>();

        // 1) 카테고리 폴더
        String categorySql = "SELECT CATEGORY_ID, CATEGORY_NAME FROM SQL_TEMPLATE_CATEGORY WHERE STATUS = 'ACTIVE' ORDER BY CATEGORY_ORDER, CATEGORY_NAME";
        List<Map<String, Object>> categories = jdbcTemplate.queryForList(categorySql);

        // 2) 카테고리별 템플릿
        String mappedSql = "SELECT m.CATEGORY_ID, t.TEMPLATE_ID, t.TEMPLATE_NAME " +
                "FROM SQL_TEMPLATE_CATEGORY_MAPPING m " +
                "JOIN SQL_TEMPLATE t ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                "WHERE t.STATUS = 'ACTIVE' ORDER BY m.MAPPING_ORDER, t.TEMPLATE_NAME";
        List<Map<String, Object>> mapped = jdbcTemplate.queryForList(mappedSql);

        Map<String, List<Map<String, Object>>> categoryIdToTemplates = new HashMap<>();
        for (Map<String, Object> row : mapped) {
            String categoryId = (String) row.get("CATEGORY_ID");
            categoryIdToTemplates.computeIfAbsent(categoryId, k -> new ArrayList<>());
            Map<String, Object> node = new HashMap<>();
            node.put("type", "sql");
            node.put("id", row.get("TEMPLATE_ID"));
            node.put("name", row.get("TEMPLATE_NAME") + ".sql");
            categoryIdToTemplates.get(categoryId).add(node);
        }

        for (Map<String, Object> c : categories) {
            Map<String, Object> folder = new HashMap<>();
            folder.put("type", "folder");
            folder.put("id", c.get("CATEGORY_ID"));
            folder.put("name", c.get("CATEGORY_NAME"));
            List<Map<String, Object>> children = categoryIdToTemplates.getOrDefault(c.get("CATEGORY_ID"), new ArrayList<>());
            folder.put("children", children);
            tree.add(folder);
        }

        // 3) 미분류 템플릿 (어떤 카테고리에도 매핑되지 않은 템플릿)
        String uncategorizedSql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME FROM SQL_TEMPLATE t " +
                "LEFT JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                "WHERE t.STATUS = 'ACTIVE' AND m.TEMPLATE_ID IS NULL ORDER BY t.TEMPLATE_NAME";
        List<Map<String, Object>> uncategorized = jdbcTemplate.queryForList(uncategorizedSql);
        if (!uncategorized.isEmpty()) {
            Map<String, Object> folder = new HashMap<>();
            folder.put("type", "folder");
            folder.put("id", "UNCATEGORIZED");
            folder.put("name", "미분류");
            List<Map<String, Object>> children = new ArrayList<>();
            for (Map<String, Object> row : uncategorized) {
                Map<String, Object> node = new HashMap<>();
                node.put("type", "sql");
                node.put("id", row.get("TEMPLATE_ID"));
                node.put("name", row.get("TEMPLATE_NAME") + ".sql");
                children.add(node);
            }
            folder.put("children", children);
            tree.add(folder);
        }

        return tree;
    }

    /**
     * 템플릿 상세 조회
     */
    public Map<String, Object> getSqlTemplateDetail(String templateId) {
        Map<String, Object> result = new HashMap<>();
        if (templateId == null || templateId.trim().isEmpty()) {
            result.put("error", "SQL ID가 지정되지 않았습니다.");
            return result;
        }

        String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, SQL_CONTENT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);
        if (rows.isEmpty()) {
            result.put("error", "해당 SQL 템플릿을 찾을 수 없습니다: " + templateId);
            return result;
        }

        Map<String, Object> row = rows.get(0);
        result.put("menuId", row.get("TEMPLATE_ID"));
        result.put("menuName", row.get("TEMPLATE_NAME"));
        result.put("menuPath", "");
        result.put("sqlContent", row.get("SQL_CONTENT"));

        // 파라미터를 config 형태로 변환
        String paramSql = "SELECT PARAMETER_NAME, DEFAULT_VALUE FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ? ORDER BY PARAMETER_ORDER";
        List<Map<String, Object>> params = jdbcTemplate.queryForList(paramSql, templateId);
        Map<String, Object> config = new HashMap<>();
        for (Map<String, Object> p : params) {
            String name = (String) p.get("PARAMETER_NAME");
            Object defVal = p.get("DEFAULT_VALUE");
            if (name != null) {
                config.put(name, defVal == null ? "" : defVal.toString());
            }
        }
        result.put("config", config);
        return result;
    }

    /**
     * 템플릿 저장 (신규/수정)
     */
    @Transactional
    public Map<String, Object> saveSqlTemplate(String templateId, String templateName, String pathOrCategoryId,
                                               String sqlContent, String configContent, String userId) {
        Map<String, Object> result = new HashMap<>();

        if (templateName == null || templateName.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 이름을 입력해주세요.");
            return result;
        }
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 내용을 입력해주세요.");
            return result;
        }

        boolean isNew = (templateId == null || templateId.trim().isEmpty());
        if (isNew) {
            templateId = generateTemplateId(templateName);
            String insertSql = "INSERT INTO SQL_TEMPLATE (TEMPLATE_ID, TEMPLATE_NAME, SQL_CONTENT, CREATED_BY) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, templateId, templateName, sqlContent, userId);
        } else {
            String updateSql = "UPDATE SQL_TEMPLATE SET TEMPLATE_NAME = ?, SQL_CONTENT = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE TEMPLATE_ID = ?";
            jdbcTemplate.update(updateSql, templateName, sqlContent, userId, templateId);
        }

        // 파라미터 재구성 (간단: key=value 라인 파싱)
        jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ?", templateId);
        List<String[]> parsed = parseConfig(configContent);
        int order = 0;
        for (String[] kv : parsed) {
            String name = kv[0];
            String value = kv[1];
            String pSql = "INSERT INTO SQL_TEMPLATE_PARAMETER (TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, IS_REQUIRED, DEFAULT_VALUE) VALUES (?, ?, 'STRING', ?, FALSE, ?)";
            jdbcTemplate.update(pSql, templateId, name, order++, value);
        }

        // 카테고리 매핑: pathOrCategoryId 값이 카테고리 아이디라면 매핑(옵션)
        if (pathOrCategoryId != null && !pathOrCategoryId.trim().isEmpty() && !"UNCATEGORIZED".equalsIgnoreCase(pathOrCategoryId)) {
            Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ID = ?", Integer.class, pathOrCategoryId);
            if (cnt != null && cnt > 0) {
                // 기존 모든 매핑 제거 후 단일 카테고리에 매핑 (간단 정책)
                jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ?", templateId);
                jdbcTemplate.update("INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, CREATED_BY) VALUES (?, ?, ?)", templateId, pathOrCategoryId, userId);
            }
        }

        result.put("success", true);
        result.put("sqlId", templateId);
        result.put("message", "SQL 템플릿이 저장되었습니다.");
        return result;
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public Map<String, Object> deleteSqlTemplate(String templateId, String userId) {
        Map<String, Object> result = new HashMap<>();
        if (templateId == null || templateId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL ID가 지정되지 않았습니다.");
            return result;
        }

        jdbcTemplate.update("DELETE FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?", templateId);
        result.put("success", true);
        result.put("message", "SQL 템플릿이 삭제되었습니다.");
        return result;
    }

    private String generateTemplateId(String templateName) {
        String clean = templateName.replaceAll("[^a-zA-Z0-9_]", "_");
        return clean + "_" + System.currentTimeMillis();
    }

    private List<String[]> parseConfig(String configContent) {
        List<String[]> list = new ArrayList<>();
        if (configContent == null || configContent.trim().isEmpty()) {
            return list;
        }
        String[] lines = configContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx > 0) {
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                if (!key.isEmpty()) {
                    list.add(new String[]{key, val});
                }
            }
        }
        return list;
    }
}


