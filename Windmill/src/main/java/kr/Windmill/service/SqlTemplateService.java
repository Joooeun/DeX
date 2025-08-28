package kr.Windmill.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;
import kr.Windmill.util.Log;

@Service
public class SqlTemplateService {

	private static final Logger logger = LoggerFactory.getLogger(SqlTemplateService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final Common com;

	@Autowired
	public SqlTemplateService(Common common) {
		this.com = common;
	}

	/**
	 * 관리자용 전체 트리 조회 (카테고리 폴더 + 템플릿 노드)
	 */
	public List<Map<String, Object>> getFullMenuTree() {
		List<Map<String, Object>> tree = new ArrayList<>();

		try {
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
				node.put("Name", row.get("TEMPLATE_NAME") + ".sql");
				node.put("templateId", row.get("TEMPLATE_ID"));
				categoryIdToTemplates.get(categoryId).add(node);
			}

			for (Map<String, Object> c : categories) {
				Map<String, Object> folder = new HashMap<>();
				folder.put("type", "folder");
				folder.put("id", c.get("CATEGORY_ID"));
				folder.put("Name", c.get("CATEGORY_NAME"));
				folder.put("Path", c.get("CATEGORY_ID") + "Path"); // 기존 구조와 호환
				List<Map<String, Object>> children = categoryIdToTemplates.getOrDefault(c.get("CATEGORY_ID"), new ArrayList<>());
				folder.put("list", children);
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
				folder.put("Name", "미분류");
				folder.put("Path", "UNCATEGORIZEDPath"); // 기존 구조와 호환
				List<Map<String, Object>> children = new ArrayList<>();
				for (Map<String, Object> row : uncategorized) {
					Map<String, Object> node = new HashMap<>();
					node.put("type", "sql");
					node.put("id", row.get("TEMPLATE_ID"));
					node.put("Name", row.get("TEMPLATE_NAME") + ".sql");
					node.put("templateId", row.get("TEMPLATE_ID"));
					children.add(node);
				}
				folder.put("list", children);
				tree.add(folder);
			}

		} catch (Exception e) {
			logger.error("메뉴 트리 조회 실패", e);
		}

		return tree;
	}

	/**
	 * 사용자별 권한 기반 메뉴 트리 조회 (한 번의 쿼리로 권한 있는 카테고리와 템플릿만 조회)
	 */
	public List<Map<String, Object>> getUserMenuTree(String userId) {
		List<Map<String, Object>> tree = new ArrayList<>();

		try {
			// 관리자인 경우 전체 트리 반환
			if ("admin".equals(userId)) {
				return getFullMenuTree();
			}

			// 사용자별 권한 기반 메뉴 조회 (그룹을 통한 권한 체계)
			String sql = "SELECT DISTINCT c.CATEGORY_ID, c.CATEGORY_NAME, c.CATEGORY_ORDER, " +
					"t.TEMPLATE_ID, t.TEMPLATE_NAME, m.MAPPING_ORDER " +
					"FROM SQL_TEMPLATE_CATEGORY c " +
					"JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON c.CATEGORY_ID = m.CATEGORY_ID " +
					"JOIN SQL_TEMPLATE t ON m.TEMPLATE_ID = t.TEMPLATE_ID " +
					"JOIN GROUP_CATEGORY_MAPPING gcm ON c.CATEGORY_ID = gcm.CATEGORY_ID " +
					"JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " +
					"WHERE c.STATUS = 'ACTIVE' AND t.STATUS = 'ACTIVE' " +
					"AND ugm.USER_ID = ? " +
					"ORDER BY c.CATEGORY_ORDER, c.CATEGORY_NAME, m.MAPPING_ORDER, t.TEMPLATE_NAME";

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);

			// 카테고리별로 그룹화
			Map<String, Map<String, Object>> categoryMap = new HashMap<>();
			
			for (Map<String, Object> row : rows) {
				String categoryId = (String) row.get("CATEGORY_ID");
				
				// 카테고리 정보 추가
				if (!categoryMap.containsKey(categoryId)) {
					Map<String, Object> category = new HashMap<>();
					category.put("type", "folder");
					category.put("id", categoryId);
					category.put("Name", row.get("CATEGORY_NAME"));
					category.put("Path", categoryId + "Path");
					category.put("list", new ArrayList<Map<String, Object>>());
					categoryMap.put(categoryId, category);
				}
				
				// 템플릿 정보 추가
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) categoryMap.get(categoryId).get("list");
				
				Map<String, Object> template = new HashMap<>();
				template.put("type", "sql");
				template.put("id", row.get("TEMPLATE_ID"));
				template.put("Name", row.get("TEMPLATE_NAME") + ".sql");
				template.put("templateId", row.get("TEMPLATE_ID"));
				children.add(template);
			}
			
			// 카테고리 순서대로 정렬하여 트리 구성
			tree.addAll(categoryMap.values());
			
			// 미분류 템플릿은 현재 권한 체계에서 제외 (필요시 별도 권한 체계 구현 필요)
			// TODO: 미분류 템플릿에 대한 권한 체계 정의 필요

		} catch (Exception e) {
			logger.error("사용자 메뉴 트리 조회 실패: " + userId, e);
		}

		return tree;
	}

	/**
	 * 템플릿 상세 조회
	 */
	public Map<String, Object> getSqlTemplateDetail(String templateId) {
		Map<String, Object> result = new HashMap<>();
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "SQL ID가 지정되지 않았습니다.");
			return result;
		}

					String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, ACCESSIBLE_CONNECTION_IDS, CHART_MAPPING, VERSION, STATUS, EXECUTION_LIMIT, REFRESH_TIMEOUT, NEWLINE, AUDIT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);
		if (rows.isEmpty()) {
			result.put("success", false);
			result.put("error", "해당 SQL 템플릿을 찾을 수 없습니다: " + templateId);
			return result;
		}

		Map<String, Object> row = rows.get(0);
		Map<String, Object> data = new HashMap<>();
		data.put("sqlId", row.get("TEMPLATE_ID"));
		data.put("sqlName", row.get("TEMPLATE_NAME"));
		data.put("sqlDesc", row.get("TEMPLATE_DESC"));
		data.put("sqlContent", row.get("SQL_CONTENT"));
		data.put("accessibleConnectionIds", row.get("ACCESSIBLE_CONNECTION_IDS"));
		data.put("chartMapping", row.get("CHART_MAPPING"));
		data.put("sqlVersion", row.get("VERSION"));
		data.put("sqlStatus", row.get("STATUS"));
		data.put("executionLimit", row.get("EXECUTION_LIMIT"));
		data.put("refreshTimeout", row.get("REFRESH_TIMEOUT"));
		data.put("newline", row.get("NEWLINE"));
		data.put("audit", row.get("AUDIT"));

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
		data.put("configContent", configToString(config));

		result.put("success", true);
		result.put("data", data);
		return result;
	}

	/**
	 * 템플릿 파라미터 조회
	 */
	public Map<String, Object> getTemplateParameters(String templateId) {
		Map<String, Object> result = new HashMap<>();
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}

		try {
			String sql = "SELECT PARAMETER_NAME, PARAMETER_TYPE, DEFAULT_VALUE, IS_REQUIRED, PARAMETER_ORDER, IS_READONLY, IS_HIDDEN, IS_DISABLED, DESCRIPTION FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ? ORDER BY PARAMETER_ORDER";
			List<Map<String, Object>> parameters = jdbcTemplate.queryForList(sql, templateId);

			result.put("success", true);
			result.put("data", parameters);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "파라미터 조회 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 템플릿 단축키 조회
	 */
	public Map<String, Object> getTemplateShortcuts(String templateId) {
		Map<String, Object> result = new HashMap<>();
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}

		try {
			String sql = "SELECT SHORTCUT_KEY, SHORTCUT_NAME, TARGET_TEMPLATE_ID, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE, IS_ACTIVE FROM SQL_TEMPLATE_SHORTCUT WHERE SOURCE_TEMPLATE_ID = ? ORDER BY SHORTCUT_KEY";
			List<Map<String, Object>> shortcuts = jdbcTemplate.queryForList(sql, templateId);

			result.put("success", true);
			result.put("data", shortcuts);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "단축키 조회 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 차트 매핑으로 템플릿 조회
	 */
	public Map<String, Object> getTemplateByChartMapping(String chartId) {
		try {
			String sql = "SELECT t.* " +
					"FROM SQL_TEMPLATE t " +
					"WHERE t.CHART_MAPPING = ? AND t.STATUS = 'ACTIVE'";
			
			List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, chartId);
			
			if (!results.isEmpty()) {
				Map<String, Object> template = results.get(0);
				
				// 접근 가능한 DB 연결 목록 조회 (ACCESSIBLE_CONNECTION_IDS에서 파싱)
				String accessibleConnectionIds = (String) template.get("ACCESSIBLE_CONNECTION_IDS");
				List<String> connections = new ArrayList<>();
				
				if (accessibleConnectionIds != null && !accessibleConnectionIds.trim().isEmpty()) {
					String[] ids = accessibleConnectionIds.split(",");
					for (String id : ids) {
						String trimmedId = id.trim();
						if (!trimmedId.isEmpty()) {
							connections.add(trimmedId);
						}
					}
				}
				
				template.put("accessibleConnections", connections);
				
				return template;
			}
			
			return null;
		} catch (Exception e) {
			logger.error("차트 매핑 템플릿 조회 실패: {}", chartId, e);
			return null;
		}
	}

	/**
	 * 차트 매핑 중복 체크
	 */
	public boolean isChartMappingExists(String chartId, String excludeTemplateId) {
		try {
			String sql = "SELECT COUNT(*) FROM SQL_TEMPLATE WHERE CHART_MAPPING = ? AND STATUS = 'ACTIVE'";
			List<Object> params = new ArrayList<>();
			params.add(chartId);
			
			if (excludeTemplateId != null && !excludeTemplateId.trim().isEmpty()) {
				sql += " AND TEMPLATE_ID != ?";
				params.add(excludeTemplateId);
			}
			
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params.toArray());
			return count != null && count > 0;
		} catch (Exception e) {
			logger.error("차트 매핑 중복 체크 실패: {}", chartId, e);
			return false;
		}
	}

	/**
	 * 차트 매핑 해제
	 */
	public boolean clearChartMapping(String templateId) {
		try {
			String sql = "UPDATE SQL_TEMPLATE SET CHART_MAPPING = NULL, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE TEMPLATE_ID = ?";
			int result = jdbcTemplate.update(sql, templateId);
			return result > 0;
		} catch (Exception e) {
			logger.error("차트 매핑 해제 실패: {}", templateId, e);
			return false;
		}
	}

	/**
	 * 템플릿에 접근 가능한 DB 연결 목록 조회
	 */
	public List<Map<String, Object>> getAccessibleConnections(String templateId, String userId) {
		try {
			// 템플릿의 접근 가능한 연결 ID 조회
			String templateSql = "SELECT ACCESSIBLE_CONNECTION_IDS FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
			List<Map<String, Object>> templateResults = jdbcTemplate.queryForList(templateSql, templateId);
			
			if (templateResults.isEmpty()) {
				return new ArrayList<>();
			}
			
			String accessibleConnectionIds = (String) templateResults.get(0).get("ACCESSIBLE_CONNECTION_IDS");
			
			// 모든 활성 DB 연결 조회
			String connectionSql = "SELECT CONNECTION_ID, DB_TYPE, HOST_IP, PORT, DATABASE_NAME, USERNAME, STATUS " +
								 "FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY DB_TYPE, CONNECTION_ID";
			List<Map<String, Object>> allConnections = jdbcTemplate.queryForList(connectionSql);
			
			// 접근 가능한 연결 ID가 설정되지 않았으면 모든 연결 반환
			if (accessibleConnectionIds == null || accessibleConnectionIds.trim().isEmpty()) {
				return allConnections;
			}
			
			// 설정된 연결 ID만 필터링
			List<String> allowedConnectionIds = Arrays.asList(accessibleConnectionIds.split(","));
			List<Map<String, Object>> accessibleConnections = new ArrayList<>();
			
			for (Map<String, Object> connection : allConnections) {
				String connectionId = (String) connection.get("CONNECTION_ID");
				if (allowedConnectionIds.contains(connectionId)) {
					accessibleConnections.add(connection);
				}
			}
			
			return accessibleConnections;
		} catch (Exception e) {
			logger.error("접근 가능한 DB 연결 조회 실패: " + templateId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 템플릿 목록 조회 (단축키 대상용)
	 */
	public Map<String, Object> getTemplateList() {
		Map<String, Object> result = new HashMap<>();
		try {
			String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME FROM SQL_TEMPLATE WHERE STATUS = 'ACTIVE' ORDER BY TEMPLATE_NAME";
			List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql);

			result.put("success", true);
			result.put("data", templates);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "템플릿 목록 조회 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 템플릿 저장 (신규/수정) - 파라미터 및 단축키 포함
	 */
	@Transactional
    	public Map<String, Object> saveSqlTemplate(String templateId, String templateName, String templateDesc, 
                                               Integer version, String status, Integer executionLimit, 
                                               Integer refreshTimeout, Boolean newline, Boolean audit, String categoryIds,
                                               String accessibleConnectionIds, String chartMapping, String sqlContent, String configContent, String parametersJson, 
                                               String shortcutsJson, String additionalSqlContents, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (templateName == null || templateName.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "SQL 이름을 입력해주세요.");
			return result;
		}

		// 기본값 설정
        if (version == null) version = 1;
        if (status == null) status = "ACTIVE";
        if (executionLimit == null) executionLimit = 0;
        if (refreshTimeout == null) refreshTimeout = 0;
        if (newline == null) newline = true;
        if (audit == null) audit = false;

		boolean isNew = (templateId == null || templateId.trim().isEmpty());
		if (isNew) {
			templateId = generateTemplateId(templateName);
			String insertSql = "INSERT INTO SQL_TEMPLATE (TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, ACCESSIBLE_CONNECTION_IDS, CHART_MAPPING, VERSION, STATUS, EXECUTION_LIMIT, REFRESH_TIMEOUT, NEWLINE, AUDIT, CREATED_BY) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			jdbcTemplate.update(insertSql, templateId, templateName, templateDesc, sqlContent, accessibleConnectionIds, chartMapping, version, status, executionLimit, refreshTimeout, newline, audit, userId);
		} else {
			String updateSql = "UPDATE SQL_TEMPLATE SET TEMPLATE_NAME = ?, TEMPLATE_DESC = ?, SQL_CONTENT = ?, ACCESSIBLE_CONNECTION_IDS = ?, CHART_MAPPING = ?, VERSION = ?, STATUS = ?, EXECUTION_LIMIT = ?, REFRESH_TIMEOUT = ?, NEWLINE = ?, AUDIT = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE TEMPLATE_ID = ?";
			jdbcTemplate.update(updateSql, templateName, templateDesc, sqlContent, accessibleConnectionIds, chartMapping, version, status, executionLimit, refreshTimeout, newline, audit, userId, templateId);
		}

		// 기존 카테고리 매핑 삭제
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ?", templateId);

		// 새로운 카테고리 매핑 생성
		if (categoryIds != null && !categoryIds.trim().isEmpty()) {
			String[] categoryIdArray = categoryIds.split(",");
			for (String categoryId : categoryIdArray) {
				categoryId = categoryId.trim();
				if (!categoryId.isEmpty()) {
                    Integer maxOrder = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(MAPPING_ORDER), 0) FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE CATEGORY_ID = ?", 
                        Integer.class, categoryId);
					int newOrder = (maxOrder != null ? maxOrder : 0) + 1;

                    jdbcTemplate.update(
                        "INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES (?, ?, ?, ?)",
                        templateId, categoryId, newOrder, userId);
				}
			}
		}

		// 파라미터 처리
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ?", templateId);

		if (parametersJson != null && !parametersJson.trim().isEmpty()) {
			try {

				List<Map<String, Object>> parameters = com.getListFromString(parametersJson);

				for (Map<String, Object> param : parameters) {

					String name = (String) param.get("name");
					String type = (String) param.get("type");
					String defaultValue = (String) param.get("defaultValue");
					Boolean required = (Boolean) param.get("required");
					Integer order = (Integer) param.get("order");

					if (name != null && !name.trim().isEmpty()) {
						String description = (String) param.get("description");
						Boolean readonly = (Boolean) param.get("readonly");
						Boolean hidden = (Boolean) param.get("hidden");
						Boolean disabled = (Boolean) param.get("disabled");

						// 속성값들을 개별 필드로 저장
						Boolean isReadonly = readonly != null ? readonly : false;
						Boolean isHidden = hidden != null ? hidden : false;
						Boolean isDisabled = disabled != null ? disabled : false;

                        jdbcTemplate.update(
                            "INSERT INTO SQL_TEMPLATE_PARAMETER (TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, DEFAULT_VALUE, IS_REQUIRED, PARAMETER_ORDER, IS_READONLY, IS_HIDDEN, IS_DISABLED, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            templateId, name.trim(), type != null ? type : "STRING", defaultValue, required != null ? required : false, order != null ? order : 1, 
                            isReadonly, isHidden, isDisabled, description);
					}
				}
			} catch (Exception e) {
                // logger.warn("파라미터 JSON 파싱 실패: " + e.getMessage()); // Original code had this line commented out
				// 기존 방식으로 fallback
				if (configContent != null && !configContent.trim().isEmpty()) {
					String[] lines = configContent.split("\n");
					int order = 1;
					for (String line : lines) {
						line = line.trim();
						if (!line.isEmpty() && line.contains("=")) {
							String[] parts = line.split("=", 2);
							if (parts.length == 2) {
								String paramName = parts[0].trim();
								String paramDefaultValue = parts[1].trim();

                                jdbcTemplate.update(
                                    "INSERT INTO SQL_TEMPLATE_PARAMETER (TEMPLATE_ID, PARAMETER_NAME, DEFAULT_VALUE, PARAMETER_ORDER, IS_READONLY, IS_HIDDEN, IS_DISABLED, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                    templateId, paramName, paramDefaultValue, order++, false, false, false, null);
							}
						}
					}
				}
			}
		}

		// 단축키 처리
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_SHORTCUT WHERE SOURCE_TEMPLATE_ID = ?", templateId);

		if (shortcutsJson != null && !shortcutsJson.trim().isEmpty()) {
			try {
				List<Map<String, Object>> shortcuts = parseShortcutsJson(shortcutsJson);
				for (Map<String, Object> shortcut : shortcuts) {
					String key = (String) shortcut.get("key");
					String name = (String) shortcut.get("name");
					String targetTemplateId = (String) shortcut.get("targetTemplateId");
					Boolean autoExecute = (Boolean) shortcut.get("autoExecute");
					Boolean isActive = (Boolean) shortcut.get("isActive");

					if (key != null && !key.trim().isEmpty() && name != null && !name.trim().isEmpty() && targetTemplateId != null && !targetTemplateId.trim().isEmpty()) {
						String description = (String) shortcut.get("description");
						String sourceColumns = (String) shortcut.get("sourceColumns");
                        jdbcTemplate.update(
                            "INSERT INTO SQL_TEMPLATE_SHORTCUT (SOURCE_TEMPLATE_ID, TARGET_TEMPLATE_ID, SHORTCUT_KEY, SHORTCUT_NAME, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE, IS_ACTIVE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            templateId, targetTemplateId.trim(), key.trim(), name.trim(), description, sourceColumns, autoExecute != null ? autoExecute : true, isActive != null ? isActive : true);
					}
				}
			} catch (Exception e) {
                // logger.warn("단축키 JSON 파싱 실패: " + e.getMessage()); // Original code had this line commented out
			}
		}

		// 추가 SQL 내용 처리
		if (additionalSqlContents != null && !additionalSqlContents.trim().isEmpty()) {
			try {
				List<Map<String, Object>> additionalContents = com.getListFromString(additionalSqlContents);
				for (Map<String, Object> content : additionalContents) {
					String dbType = (String) content.get("dbType");
					String contentSql = (String) content.get("sqlContent");
					
					if (dbType != null && !dbType.trim().isEmpty() && contentSql != null && !contentSql.trim().isEmpty()) {
						// 기존 SQL_CONTENT 삭제
						jdbcTemplate.update("DELETE FROM SQL_CONTENT WHERE TEMPLATE_ID = ? AND DB_TYPE = ?", templateId, dbType);
						
						// 새 SQL_CONTENT 추가
						String contentId = "CONTENT_" + templateId + "_" + dbType + "_" + System.currentTimeMillis();
						jdbcTemplate.update(
							"INSERT INTO SQL_CONTENT (CONTENT_ID, TEMPLATE_ID, DB_TYPE, SQL_CONTENT, CREATED_BY) VALUES (?, ?, ?, ?, ?)",
							contentId, templateId, dbType, contentSql, userId);
					}
				}
			} catch (Exception e) {
				logger.warn("추가 SQL 내용 처리 실패: " + e.getMessage());
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

	/**
	 * 카테고리 목록 조회
	 */
	public List<Map<String, Object>> getCategories() {
		String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, CATEGORY_DESCRIPTION, CATEGORY_ORDER, STATUS FROM SQL_TEMPLATE_CATEGORY WHERE STATUS = 'ACTIVE' ORDER BY CATEGORY_ORDER, CATEGORY_NAME";
		return jdbcTemplate.queryForList(sql);
	}

	/**
	 * 카테고리 생성
	 */
	@Transactional
	public Map<String, Object> createCategory(String categoryName, String description, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (categoryName == null || categoryName.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "카테고리명을 입력해주세요.");
			return result;
		}

		try {
			String categoryId = "CATEGORY_" + System.currentTimeMillis();
			Integer maxOrder = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(CATEGORY_ORDER), 0) FROM SQL_TEMPLATE_CATEGORY", Integer.class);
			int newOrder = (maxOrder != null ? maxOrder : 0) + 1;

			String sql = "INSERT INTO SQL_TEMPLATE_CATEGORY (CATEGORY_ID, CATEGORY_NAME, CATEGORY_DESCRIPTION, CATEGORY_ORDER, CREATED_BY) VALUES (?, ?, ?, ?, ?)";
			jdbcTemplate.update(sql, categoryId, categoryName, description, newOrder, userId);

			result.put("success", true);
			result.put("categoryId", categoryId);
			result.put("message", "카테고리가 생성되었습니다.");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "카테고리 생성 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 카테고리 수정
	 */
	@Transactional
	public Map<String, Object> updateCategory(String categoryId, String categoryName, String description, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (categoryId == null || categoryId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "카테고리 ID가 지정되지 않았습니다.");
			return result;
		}

		if (categoryName == null || categoryName.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "카테고리명을 입력해주세요.");
			return result;
		}

		try {
			String sql = "UPDATE SQL_TEMPLATE_CATEGORY SET CATEGORY_NAME = ?, CATEGORY_DESCRIPTION = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE CATEGORY_ID = ?";
			jdbcTemplate.update(sql, categoryName, description, userId, categoryId);

			result.put("success", true);
			result.put("message", "카테고리가 수정되었습니다.");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "카테고리 수정 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 카테고리 삭제
	 */
	@Transactional
	public Map<String, Object> deleteCategory(String categoryId, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (categoryId == null || categoryId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "카테고리 ID가 지정되지 않았습니다.");
			return result;
		}

		try {
			// 카테고리에 속한 템플릿이 있는지 확인
            Integer templateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE CATEGORY_ID = ?", 
                Integer.class, categoryId);

			if (templateCount != null && templateCount > 0) {
				result.put("success", false);
				result.put("error", "카테고리에 속한 템플릿이 있어 삭제할 수 없습니다. 먼저 템플릿을 다른 카테고리로 이동하거나 미분류로 이동해주세요.");
				return result;
			}

			// 카테고리 삭제 (실제 삭제 대신 비활성화)
			String sql = "UPDATE SQL_TEMPLATE_CATEGORY SET STATUS = 'INACTIVE', MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE CATEGORY_ID = ?";
			jdbcTemplate.update(sql, userId, categoryId);

			result.put("success", true);
			result.put("message", "카테고리가 삭제되었습니다.");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "카테고리 삭제 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 카테고리별 템플릿 목록 조회
	 */
	public List<Map<String, Object>> getTemplatesByCategory(String categoryId) {
		if (categoryId == null || categoryId.trim().isEmpty() || "UNCATEGORIZED".equals(categoryId)) {
			// 미분류 템플릿 조회
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.CREATED_TIMESTAMP " +
                        "FROM SQL_TEMPLATE t " +
                        "LEFT JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                        "WHERE t.STATUS = 'ACTIVE' AND m.TEMPLATE_ID IS NULL " +
                        "ORDER BY t.CREATED_TIMESTAMP DESC";
			return jdbcTemplate.queryForList(sql);
		} else {
			// 특정 카테고리의 템플릿 조회
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.CREATED_TIMESTAMP " +
                        "FROM SQL_TEMPLATE t " +
                        "JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                        "WHERE t.STATUS = 'ACTIVE' AND m.CATEGORY_ID = ? " +
                        "ORDER BY m.MAPPING_ORDER, t.TEMPLATE_NAME";
			return jdbcTemplate.queryForList(sql, categoryId);
		}
	}

	/**
	 * 템플릿을 카테고리에 할당
	 */
	@Transactional
	public Map<String, Object> assignTemplateToCategory(String templateId, String categoryId, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}

		try {
			if (categoryId == null || categoryId.trim().isEmpty() || "UNCATEGORIZED".equals(categoryId)) {
				// 미분류로 이동 (기존 매핑 삭제)
				jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ?", templateId);
				result.put("message", "템플릿이 미분류로 이동되었습니다.");
			} else {
				// 특정 카테고리로 이동
				// 기존 매핑 삭제 후 새 매핑 추가
				jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ?", templateId);

                Integer maxOrder = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(MAPPING_ORDER), 0) FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE CATEGORY_ID = ?", 
                    Integer.class, categoryId);
				int newOrder = (maxOrder != null ? maxOrder : 0) + 1;

                jdbcTemplate.update(
                    "INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES (?, ?, ?, ?)",
                    templateId, categoryId, newOrder, userId);
				result.put("message", "템플릿이 카테고리에 할당되었습니다.");
			}

			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "템플릿 할당 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 카테고리 상세 정보 조회
	 */
	public Map<String, Object> getCategoryDetail(String categoryId) {
		if (categoryId == null || categoryId.trim().isEmpty()) {
			return null;
		}

		String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, CATEGORY_DESCRIPTION, CATEGORY_ORDER, STATUS FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ID = ?";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, categoryId);

		if (rows.isEmpty()) {
			return null;
		}

		return rows.get(0);
	}

	/**
	 * 템플릿의 접근 가능한 DB 연결 ID 목록 조회
	 */
	public List<String> getTemplateAccessibleConnections(String templateId) {
		if (templateId == null || templateId.trim().isEmpty()) {
			return new ArrayList<>();
		}

		String sql = "SELECT ACCESSIBLE_CONNECTION_IDS FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);

		if (rows.isEmpty()) {
			return new ArrayList<>();
		}

		String accessibleConnectionIds = (String) rows.get(0).get("ACCESSIBLE_CONNECTION_IDS");

		// ACCESSIBLE_CONNECTION_IDS가 null이거나 비어있으면 모든 DB 연결 허용
		if (accessibleConnectionIds == null || accessibleConnectionIds.trim().isEmpty()) {
			return getAllActiveConnectionIds();
		}

		// 콤마로 구분된 연결 ID들을 리스트로 변환
		List<String> connectionIds = new ArrayList<>();
		String[] ids = accessibleConnectionIds.split(",");
		for (String id : ids) {
			String trimmedId = id.trim();
			if (!trimmedId.isEmpty()) {
				connectionIds.add(trimmedId);
			}
		}

		return connectionIds;
	}

	/**
	 * 모든 활성 DB 연결 ID 목록 조회
	 */
	private List<String> getAllActiveConnectionIds() {
		String sql = "SELECT CONNECTION_ID FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

		List<String> connectionIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			String connectionId = (String) row.get("CONNECTION_ID");
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				connectionIds.add(connectionId);
			}
		}

		return connectionIds;
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
					list.add(new String[] { key, val });
				}
			}
		}
		return list;
	}

	/**
	 * config Map을 문자열로 변환
	 */
	private String configToString(Map<String, Object> config) {
		if (config == null || config.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> entry : config.entrySet()) {
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * 파라미터 JSON 파싱 (간단한 구현)
	 */
	private List<Map<String, Object>> parseParametersJson(String json) {
		List<Map<String, Object>> parameters = new ArrayList<>();
		try {
			// 간단한 JSON 파싱 (실제로는 Jackson이나 Gson 사용 권장)
			if (json.startsWith("[") && json.endsWith("]")) {
				String content = json.substring(1, json.length() - 1);
				String[] items = content.split("\\},\\s*\\{");

				for (String item : items) {
					item = item.replaceAll("[{}]", "");
					Map<String, Object> param = new HashMap<>();

					String[] pairs = item.split(",");
					for (String pair : pairs) {
						String[] kv = pair.split(":");
						if (kv.length == 2) {
							String key = kv[0].trim().replaceAll("\"", "");
							String value = kv[1].trim().replaceAll("\"", "");

							if ("required".equals(key)) {
								param.put(key, "true".equals(value));
							} else if ("order".equals(key)) {
								param.put(key, Integer.parseInt(value));
							} else {
								param.put(key, value);
							}
						}
					}
					parameters.add(param);
				}
			}
		} catch (Exception e) {
            // logger.error("JSON 파싱 실패: " + e.getMessage()); // Original code had this line commented out
		}
		return parameters;
	}

	/**
	 * 단축키 JSON 파싱
	 */
	private List<Map<String, Object>> parseShortcutsJson(String json) {
		List<Map<String, Object>> shortcuts = new ArrayList<>();
		try {
			if (json.startsWith("[") && json.endsWith("]")) {
				String content = json.substring(1, json.length() - 1);
				String[] items = content.split("\\},\\s*\\{");

				for (String item : items) {
					item = item.replaceAll("[{}]", "");
					Map<String, Object> shortcut = new HashMap<>();

					String[] pairs = item.split(",");
					for (String pair : pairs) {
						String[] kv = pair.split(":");
						if (kv.length == 2) {
							String key = kv[0].trim().replaceAll("\"", "");
							String value = kv[1].trim().replaceAll("\"", "");

							if ("autoExecute".equals(key) || "isActive".equals(key)) {
								shortcut.put(key, "true".equals(value));
							} else {
								shortcut.put(key, value);
							}
						}
					}
					shortcuts.add(shortcut);
				}
			}
		} catch (Exception e) {
            // logger.error("단축키 JSON 파싱 실패: " + e.getMessage()); // Original code had this line commented out
		}
		return shortcuts;
	}
}


