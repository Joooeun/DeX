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
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.SqlContent;
import kr.Windmill.dto.SqlTemplateInfo;
import kr.Windmill.dto.SqlTemplateParameter;
import kr.Windmill.dto.SqlTemplateSaveRequest;
import kr.Windmill.dto.SqlTemplateShortcut;
import kr.Windmill.util.Common;

@Service
public class SqlTemplateService {

	private static final Logger logger = LoggerFactory.getLogger(SqlTemplateService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PermissionService permissionService;

	@Autowired
	private SqlContentService sqlContentService;

	@Autowired
	private AuditLogService auditLogService;

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
			String mappedSql = "SELECT m.CATEGORY_ID, t.TEMPLATE_ID, t.TEMPLATE_NAME, t.TEMPLATE_TYPE " +
					"FROM SQL_TEMPLATE_CATEGORY_MAPPING m " +
					"JOIN SQL_TEMPLATE t ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
					"WHERE t.STATUS = 'ACTIVE' ORDER BY t.TEMPLATE_NAME ASC";
			List<Map<String, Object>> mapped = jdbcTemplate.queryForList(mappedSql);

			Map<String, List<Map<String, Object>>> categoryIdToTemplates = new HashMap<>();
			for (Map<String, Object> row : mapped) {
				String categoryId = (String) row.get("CATEGORY_ID");
				categoryIdToTemplates.computeIfAbsent(categoryId, k -> new ArrayList<>());
				Map<String, Object> node = new HashMap<>();
				String templateType = (String) row.get("TEMPLATE_TYPE");
				if (templateType == null) templateType = "SQL";
				
				node.put("type", templateType.toLowerCase());
				node.put("id", row.get("TEMPLATE_ID"));
				node.put("Name", row.get("TEMPLATE_NAME")); // 확장자 제외
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
			String uncategorizedSql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.TEMPLATE_TYPE FROM SQL_TEMPLATE t " +
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
					String templateType = (String) row.get("TEMPLATE_TYPE");
					if (templateType == null) templateType = "SQL";
					
					node.put("type", templateType.toLowerCase());
					node.put("id", row.get("TEMPLATE_ID"));
					node.put("Name", row.get("TEMPLATE_NAME")); // 확장자 제외
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
			if (permissionService.isAdmin(userId)) {
				return getFullMenuTree();
			}

			// 사용자별 권한 기반 메뉴 조회 (그룹을 통한 권한 체계)
			String sql = "SELECT DISTINCT c.CATEGORY_ID, c.CATEGORY_NAME, c.CATEGORY_ORDER, " +
					"t.TEMPLATE_ID, t.TEMPLATE_NAME, t.TEMPLATE_TYPE, m.MAPPING_ORDER " +
					"FROM SQL_TEMPLATE_CATEGORY c " +
					"JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON c.CATEGORY_ID = m.CATEGORY_ID " +
					"JOIN SQL_TEMPLATE t ON m.TEMPLATE_ID = t.TEMPLATE_ID " +
					"JOIN GROUP_CATEGORY_MAPPING gcm ON c.CATEGORY_ID = gcm.CATEGORY_ID " +
					"JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " +
					"WHERE c.STATUS = 'ACTIVE' AND t.STATUS = 'ACTIVE' " +
					"AND ugm.USER_ID = ? " +
					"ORDER BY c.CATEGORY_ORDER, c.CATEGORY_NAME, t.TEMPLATE_NAME ASC";

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
				String templateType = (String) row.get("TEMPLATE_TYPE");
				if (templateType == null) templateType = "SQL";
				
				template.put("type", templateType.toLowerCase());
				template.put("id", row.get("TEMPLATE_ID"));
				template.put("Name", row.get("TEMPLATE_NAME")); // 확장자 제외
				template.put("templateId", row.get("TEMPLATE_ID"));
				children.add(template);
			}
			
			// 카테고리 순서대로 정렬하여 트리 구성
			tree.addAll(categoryMap.values());
			

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
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}

		String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, ACCESSIBLE_CONNECTION_IDS, TEMPLATE_TYPE, VERSION, STATUS, EXECUTION_LIMIT, REFRESH_TIMEOUT, NEWLINE, AUDIT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);
		if (rows.isEmpty()) {
			result.put("success", false);
			result.put("error", "해당 SQL 템플릿을 찾을 수 없습니다: " + templateId);
			return result;
		}

		Map<String, Object> row = rows.get(0);
		Map<String, Object> data = new HashMap<>();
		data.put("templateId", row.get("TEMPLATE_ID"));
		data.put("sqlName", row.get("TEMPLATE_NAME"));
		data.put("sqlDesc", row.get("TEMPLATE_DESC"));
		data.put("sqlContent", row.get("SQL_CONTENT"));
		data.put("accessibleConnectionIds", row.get("ACCESSIBLE_CONNECTION_IDS"));
		data.put("templateType", row.get("TEMPLATE_TYPE"));
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
	 * 템플릿에 접근 가능한 연결 목록 조회 (DB 또는 SFTP)
	 */
	public List<Map<String, Object>> getAccessibleConnections(String templateId, String userId) {
		try {
			// 템플릿 정보 조회 (타입과 접근 가능한 연결 ID)
			String templateSql = "SELECT TEMPLATE_TYPE, ACCESSIBLE_CONNECTION_IDS FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
			List<Map<String, Object>> templateResults = jdbcTemplate.queryForList(templateSql, templateId);
			
			if (templateResults.isEmpty()) {
				return new ArrayList<>();
			}
			
			Map<String, Object> templateData = templateResults.get(0);
			String templateType = (String) templateData.get("TEMPLATE_TYPE");
			String accessibleConnectionIds = (String) templateData.get("ACCESSIBLE_CONNECTION_IDS");
			
			List<Map<String, Object>> allConnections = new ArrayList<>();
			
			// 템플릿 타입에 따라 연결 조회
			if ("SHELL".equals(templateType)) {
				// SFTP 연결 조회
				String sftpSql = "SELECT SFTP_CONNECTION_ID as CONNECTION_ID, HOST_IP, PORT, USERNAME, STATUS " +
							   "FROM SFTP_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
				allConnections = jdbcTemplate.queryForList(sftpSql);
			} else {
				// DB 연결 조회
				String dbSql = "SELECT CONNECTION_ID, DB_TYPE, HOST_IP, PORT, DATABASE_NAME, USERNAME, STATUS " +
							 "FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
				allConnections = jdbcTemplate.queryForList(dbSql);
			}
			
			// 접근 가능한 연결 ID가 설정되지 않았으면 빈 리스트 반환 (템플릿 관리에서 선택한 연결이 없으면 연결 목록을 보여주지 않음)
			if (accessibleConnectionIds == null || accessibleConnectionIds.trim().isEmpty()) {
				return new ArrayList<>();
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
			logger.error("사용 가능 DB 조회 실패: " + templateId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 템플릿 목록 조회 (단축키 대상용)
	 */
	public Map<String, Object> getTemplateList() {
		Map<String, Object> result = new HashMap<>();
		try {
			String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_TYPE FROM SQL_TEMPLATE WHERE STATUS = 'ACTIVE' ORDER BY TEMPLATE_NAME";
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
	 * 템플릿 삭제
	 */
	@Transactional
	public Map<String, Object> deleteSqlTemplate(String templateId, String userId) {
		Map<String, Object> result = new HashMap<>();
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}

		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?", templateId);
		result.put("success", true);
		result.put("message", "SQL 템플릿이 삭제되었습니다.");
		return result;
	}

	/**
	 * audit_log 기록을 위한 템플릿 전체 정보 조회
	 * @param templateId 템플릿 ID
	 * @return 템플릿 전체 정보 (저장 구조와 동일)
	 */
	public Map<String, Object> getTemplateDataForAudit(String templateId) {
		try {
			// getFullTemplateDetail과 동일한 구조로 데이터 조회
			Map<String, Object> templateResult = getSqlTemplateDetail(templateId);
			if (!(Boolean) templateResult.get("success")) {
				return null;
			}
			
			Map<String, Object> paramResult = getTemplateParameters(templateId);
			Map<String, Object> shortcutResult = getTemplateShortcuts(templateId);
			List<Map<String, Object>> sqlContents = sqlContentService.getSqlContentsByTemplate(templateId);
			List<String> categories = getTemplateCategories(templateId);
			
			Map<String, Object> data = new HashMap<>();
			data.put("template", templateResult.get("data"));
			data.put("categories", categories);
			data.put("parameters", paramResult.get("data"));
			data.put("shortcuts", shortcutResult.get("data"));
			data.put("sqlContents", sqlContents);
			
			return data;
		} catch (Exception e) {
			logger.error("audit_log용 템플릿 정보 조회 실패: " + templateId, e);
			return null;
		}
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
	 * 카테고리 순서 변경 (위/아래)
	 */
	@Transactional
	public Map<String, Object> reorderCategory(String categoryId, String direction, String userId) {
		Map<String, Object> result = new HashMap<>();

		if (categoryId == null || categoryId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "카테고리 ID가 지정되지 않았습니다.");
			return result;
		}

		if (!"up".equals(direction) && !"down".equals(direction)) {
			result.put("success", false);
			result.put("error", "방향은 'up' 또는 'down'이어야 합니다.");
			return result;
		}

		try {
			// 현재 카테고리의 순서 조회
			Integer currentOrder = jdbcTemplate.queryForObject(
				"SELECT CATEGORY_ORDER FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ID = ? AND STATUS = 'ACTIVE'", 
				Integer.class, categoryId);

			if (currentOrder == null) {
				result.put("success", false);
				result.put("error", "카테고리를 찾을 수 없습니다.");
				return result;
			}

			// 교환할 카테고리 찾기
			String targetOrderSql;
			if ("up".equals(direction)) {
				// 위로 이동: 현재 순서보다 작은 순서 중 가장 큰 값
				targetOrderSql = "SELECT CATEGORY_ID, CATEGORY_ORDER FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ORDER < ? AND STATUS = 'ACTIVE' ORDER BY CATEGORY_ORDER DESC LIMIT 1";
			} else {
				// 아래로 이동: 현재 순서보다 큰 순서 중 가장 작은 값
				targetOrderSql = "SELECT CATEGORY_ID, CATEGORY_ORDER FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ORDER > ? AND STATUS = 'ACTIVE' ORDER BY CATEGORY_ORDER ASC LIMIT 1";
			}

			List<Map<String, Object>> targetCategories = jdbcTemplate.queryForList(targetOrderSql, currentOrder);
			
			if (targetCategories.isEmpty()) {
				result.put("success", false);
				result.put("error", "이동할 수 있는 위치가 없습니다.");
				return result;
			}

			Map<String, Object> targetCategory = targetCategories.get(0);
			String targetCategoryId = (String) targetCategory.get("CATEGORY_ID");
			Integer targetOrder = (Integer) targetCategory.get("CATEGORY_ORDER");

			// 순서 교환
			String updateCurrentSql = "UPDATE SQL_TEMPLATE_CATEGORY SET CATEGORY_ORDER = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE CATEGORY_ID = ?";
			String updateTargetSql = "UPDATE SQL_TEMPLATE_CATEGORY SET CATEGORY_ORDER = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE CATEGORY_ID = ?";

			jdbcTemplate.update(updateCurrentSql, targetOrder, userId, categoryId);
			jdbcTemplate.update(updateTargetSql, currentOrder, userId, targetCategoryId);

			result.put("success", true);
			result.put("message", "카테고리 순서가 변경되었습니다.");
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "카테고리 순서 변경 실패: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 카테고리별 템플릿 목록 조회 (관리자용 - 모든 상태 포함)
	 */
	public List<Map<String, Object>> getTemplatesByCategory(String categoryId) {
		if (categoryId == null || categoryId.trim().isEmpty() || "UNCATEGORIZED".equals(categoryId)) {
			// 미분류 템플릿 조회 (모든 상태 포함)
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.TEMPLATE_TYPE, t.STATUS, t.CREATED_TIMESTAMP " +
                        "FROM SQL_TEMPLATE t " +
                        "LEFT JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                        "WHERE m.TEMPLATE_ID IS NULL " +
                        "ORDER BY t.STATUS DESC, t.TEMPLATE_NAME ASC";
			return jdbcTemplate.queryForList(sql);
		} else {
			// 특정 카테고리의 템플릿 조회 (모든 상태 포함)
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.TEMPLATE_TYPE, t.STATUS, t.CREATED_TIMESTAMP " +
                        "FROM SQL_TEMPLATE t " +
                        "JOIN SQL_TEMPLATE_CATEGORY_MAPPING m ON t.TEMPLATE_ID = m.TEMPLATE_ID " +
                        "WHERE m.CATEGORY_ID = ? " +
                        "ORDER BY t.STATUS DESC, t.TEMPLATE_NAME ASC";
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
	 * 템플릿 전체 정보 통합 조회 (저장 구조와 동일)
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getFullTemplateDetail(String templateId) {
		Map<String, Object> result = new HashMap<>();
		
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("error", "템플릿 ID가 지정되지 않았습니다.");
			return result;
		}
		
		try {
			// 1. 기본 템플릿 정보 조회
			Map<String, Object> templateResult = getSqlTemplateDetail(templateId);
			if (!(Boolean) templateResult.get("success")) {
				result.put("success", false);
				result.put("error", "템플릿 기본 정보 조회 실패: " + templateResult.get("error"));
				return result;
			}
			
			// 2. 파라미터 정보 조회
			Map<String, Object> paramResult = getTemplateParameters(templateId);
			if (!(Boolean) paramResult.get("success")) {
				result.put("success", false);
				result.put("error", "파라미터 정보 조회 실패: " + paramResult.get("error"));
				return result;
			}
			
			// 3. 단축키 정보 조회
			Map<String, Object> shortcutResult = getTemplateShortcuts(templateId);
			if (!(Boolean) shortcutResult.get("success")) {
				result.put("success", false);
				result.put("error", "단축키 정보 조회 실패: " + shortcutResult.get("error"));
				return result;
			}
			
			// 4. SQL 컨텐츠 정보 조회
			List<Map<String, Object>> sqlContents = sqlContentService.getSqlContentsByTemplate(templateId);
			
			// 5. 카테고리 정보 조회
			List<String> categories = getTemplateCategories(templateId);
			
			// 6. 통합 응답 구성 (저장 구조와 동일 - SqlTemplateSaveRequest 구조)
			Map<String, Object> data = new HashMap<>();
			data.put("template", templateResult.get("data"));
			data.put("categories", categories);
			data.put("parameters", paramResult.get("data"));
			data.put("shortcuts", shortcutResult.get("data"));
			data.put("sqlContents", sqlContents);
			
			result.put("success", true);
			result.put("data", data);
			
		} catch (Exception e) {
			logger.error("템플릿 통합 조회 실패: " + templateId, e);
			result.put("success", false);
			result.put("error", "템플릿 통합 조회 실패: " + e.getMessage());
		}
		
		return result;
	}

	/**
	 * 템플릿의 카테고리 목록 조회
	 */
	private List<String> getTemplateCategories(String templateId) {
		List<String> categories = new ArrayList<>();
		try {
			String sql = "SELECT CATEGORY_ID FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ? ORDER BY MAPPING_ORDER";
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);
			
			for (Map<String, Object> row : rows) {
				String categoryId = (String) row.get("CATEGORY_ID");
				if (categoryId != null && !categoryId.trim().isEmpty()) {
					categories.add(categoryId);
				}
			}
		} catch (Exception e) {
			logger.warn("템플릿 카테고리 조회 실패: " + templateId, e);
		}
		return categories;
	}


	
	/**
	 * 템플릿 ID 중복 체크
	 */
	private boolean isTemplateIdExists(String templateId) {
		try {
			String sql = "SELECT COUNT(*) FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, templateId);
			return count != null && count > 0;
		} catch (Exception e) {
			logger.warn("템플릿 ID 중복 체크 실패: {}", e.getMessage());
			return false; // 오류 시 중복이 아닌 것으로 간주
		}
	}

	/**
	 * 템플릿 이름 중복 체크
	 */
	public boolean isTemplateNameExists(String templateName) {
		try {
			String sql = "SELECT COUNT(*) FROM SQL_TEMPLATE WHERE TEMPLATE_NAME = ? AND STATUS != 'DELETED'";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, templateName);
			return count != null && count > 0;
		} catch (Exception e) {
			logger.warn("템플릿 이름 중복 체크 실패: {}", e.getMessage());
			return false; // 오류 시 중복이 아닌 것으로 간주
		}
	}

	/**
	 * 템플릿 이름 중복 체크 (수정 시 자기 자신 제외)
	 */
	private boolean isTemplateNameExists(String templateName, String excludeTemplateId) {
		try {
			String sql = "SELECT COUNT(*) FROM SQL_TEMPLATE WHERE TEMPLATE_NAME = ? AND TEMPLATE_ID != ? AND STATUS != 'DELETED'";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, templateName, excludeTemplateId);
			return count != null && count > 0;
		} catch (Exception e) {
			logger.warn("템플릿 이름 중복 체크 실패: {}", e.getMessage());
			return false; // 오류 시 중복이 아닌 것으로 간주
		}
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
			// JSON 파싱 실패 시 빈 리스트 반환
		}
		return parameters;
	}

	/**
	 * 단축키 JSON 파싱
	 */
	private List<Map<String, Object>> parseShortcutsJson(String json) {
		List<Map<String, Object>> shortcuts = new ArrayList<>();
		try {
			// Jackson ObjectMapper 사용하여 정확한 JSON 파싱
			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> parsedShortcuts = mapper.readValue(json, List.class);
			
			for (Object obj : parsedShortcuts) {
				if (obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> shortcut = (Map<String, Object>) obj;
					
					// 불린 값 변환
					if (shortcut.containsKey("AUTO_EXECUTE")) {
						shortcut.put("AUTO_EXECUTE", Boolean.valueOf(shortcut.get("AUTO_EXECUTE").toString()));
					}
					if (shortcut.containsKey("IS_ACTIVE")) {
						shortcut.put("IS_ACTIVE", Boolean.valueOf(shortcut.get("IS_ACTIVE").toString()));
					}
					
					shortcuts.add(shortcut);
				}
			}
		} catch (Exception e) {
            logger.error("단축키 JSON 파싱 실패: " + e.getMessage(), e);
		}
		return shortcuts;
	}

	/**
	 * 새로운 JSON 형식으로 템플릿과 관련 데이터를 한 번에 저장
	 * 트랜잭션 기반으로 데이터 일관성 보장
	 */
	@Transactional
	public Map<String, Object> saveTemplateWithRelatedData(SqlTemplateSaveRequest request, String userId) {
		Map<String, Object> result = new HashMap<>();
		
		try {
			String templateId = request.getTemplate().getTemplateId();
			boolean isUpdate = StringUtils.hasText(templateId) && templateExists(templateId);
			
			if (isUpdate) {
				// UPDATE 방식 - 기존 템플릿 업데이트
				// 1. 템플릿 기본 정보 업데이트
				updateTemplateInfo(request.getTemplate(), userId);
				
				// 2. 관련 데이터 개별 업데이트 (삭제 후 재생성)
				updateCategoryMappings(templateId, request.getCategories(), userId);
				updateParameters(templateId, request.getParameters());
				updateShortcuts(templateId, request.getShortcuts());
				updateSqlContents(templateId, request.getSqlContents(), userId);
			} else {
				// INSERT 방식 - 새 템플릿 생성
				// 1. 템플릿 기본 정보 저장
				saveTemplateInfo(request.getTemplate(), userId);
				
				// 2. 관련 데이터 저장
				saveCategoryMappings(templateId, request.getCategories(), userId);
				saveParameters(templateId, request.getParameters());
				saveShortcuts(templateId, request.getShortcuts());
				saveSqlContents(templateId, request.getSqlContents(), userId);
			}
			
			result.put("success", true);
			result.put("templateId", templateId);
			result.put("categoryId", request.getCategories() != null && !request.getCategories().isEmpty() ? request.getCategories().get(0) : null);
			result.put("message", "SQL 템플릿이 성공적으로 저장되었습니다.");
			
		} catch (Exception e) {
			logger.error("템플릿 저장 중 오류 발생", e);
			result.put("success", false);
			
			// 구체적인 에러 메시지 생성
			String errorMessage = createDetailedErrorMessage(e, request);
			result.put("error", errorMessage);
			throw new RuntimeException("템플릿 저장 실패", e);
		}
		
		return result;
	}

	/**
	 * 상세한 에러 메시지 생성
	 */
	private String createDetailedErrorMessage(Exception e, SqlTemplateSaveRequest request) {
		String errorMessage = e.getMessage();
		String templateName = request.getTemplate() != null ? request.getTemplate().getTemplateName() : "알 수 없음";
		
		// 데이터베이스 제약 조건 위반 에러 분석
		if (errorMessage.contains("FOREIGN KEY")) {
			if (errorMessage.contains("TARGET_TEMPLATE_ID")) {
				return "단축키에서 참조하는 대상 템플릿이 존재하지 않습니다. " +
					   "단축키의 '대상 템플릿 ID'를 확인하고, 존재하는 템플릿 ID를 입력해주세요. " +
					   "(템플릿: " + templateName + ")";
			} else if (errorMessage.contains("CATEGORY_ID")) {
				return "지정된 카테고리가 존재하지 않습니다. " +
					   "카테고리 목록을 확인하고, 유효한 카테고리 ID를 입력해주세요. " +
					   "(템플릿: " + templateName + ")";
			} else if (errorMessage.contains("CONNECTION_ID")) {
				return "지정된 데이터베이스 연결이 존재하지 않습니다. " +
					   "연결 설정을 확인하고, 유효한 연결 ID를 입력해주세요. " +
					   "(템플릿: " + templateName + ")";
			}
		}
		
		// 중복 키 에러 분석
		if (errorMessage.contains("UNIQUE") || errorMessage.contains("DUPLICATE")) {
			if (errorMessage.contains("TEMPLATE_NAME")) {
				return "템플릿 이름이 이미 존재합니다. " +
					   "다른 이름을 사용하거나 기존 템플릿을 수정해주세요. " +
					   "(템플릿: " + templateName + ")";
			} else if (errorMessage.contains("SHORTCUT_KEY")) {
				return "단축키가 이미 사용 중입니다. " +
					   "다른 단축키를 사용하거나 기존 단축키를 수정해주세요. " +
					   "(템플릿: " + templateName + ")";
			} else if (errorMessage.contains("CONNECTION_ID")) {
				return "동일한 연결 ID에 대한 SQL 내용이 중복됩니다. " +
					   "각 연결 ID당 하나의 SQL 내용만 저장할 수 있습니다. " +
					   "(템플릿: " + templateName + ")";
			}
		}
		
		// NULL 제약 조건 위반
		if (errorMessage.contains("NOT NULL") || errorMessage.contains("NULL")) {
			return "필수 필드가 누락되었습니다. " +
				   "모든 필수 항목을 입력해주세요. " +
				   "(템플릿: " + templateName + ", 상세: " + errorMessage + ")";
		}
		
		// 길이 제한 초과
		if (errorMessage.contains("TOO LONG") || errorMessage.contains("LENGTH")) {
			return "입력된 데이터가 허용된 길이를 초과했습니다. " +
				   "데이터 길이를 확인하고 다시 입력해주세요. " +
				   "(템플릿: " + templateName + ", 상세: " + errorMessage + ")";
		}
		
		// 일반적인 데이터베이스 에러
		if (errorMessage.contains("SQL") || errorMessage.contains("DATABASE")) {
			return "데이터베이스 저장 중 오류가 발생했습니다. " +
				   "입력 데이터를 확인하고 다시 시도해주세요. " +
				   "(템플릿: " + templateName + ", 상세: " + errorMessage + ")";
		}
		
		// 기본 에러 메시지
		return "템플릿 저장 중 오류가 발생했습니다. " +
			   "입력 데이터를 확인하고 다시 시도해주세요. " +
			   "(템플릿: " + templateName + ", 상세: " + errorMessage + ")";
	}

	/**
	 * 템플릿 기본 정보 저장
	 */
	private void saveTemplateInfo(SqlTemplateInfo template, String userId) {
		String sql = "INSERT INTO SQL_TEMPLATE (TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, " +
		             "ACCESSIBLE_CONNECTION_IDS, TEMPLATE_TYPE, VERSION, STATUS, EXECUTION_LIMIT, " +
		             "REFRESH_TIMEOUT, NEWLINE, AUDIT, CREATED_BY, CREATED_TIMESTAMP) " +
		             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT TIMESTAMP)";
		
		String accessibleConnectionIds = String.join(",", template.getAccessibleConnectionIds());
		
		jdbcTemplate.update(sql, 
			template.getTemplateId(),
			template.getTemplateName(),
			template.getTemplateDesc(),
			template.getSqlContent(),
			accessibleConnectionIds,
			template.getTemplateType(),
			template.getVersion(),
			template.getStatus(),
			template.getExecutionLimit(),
			template.getRefreshTimeout(),
			template.getNewline(),
			template.getAudit(),
			userId);
	}

	/**
	 * 카테고리 매핑 저장
	 */
	private void saveCategoryMappings(String templateId, List<String> categories, String userId) {
		if (categories == null || categories.isEmpty()) {
			return;
		}
		
		String sql = "INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY, CREATED_TIMESTAMP) " +
		             "VALUES (?, ?, ?, ?, CURRENT TIMESTAMP)";
		
		for (int i = 0; i < categories.size(); i++) {
			jdbcTemplate.update(sql, templateId, categories.get(i), i + 1, userId);
		}
	}

	/**
	 * 파라미터 저장
	 */
	private void saveParameters(String templateId, List<SqlTemplateParameter> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return;
		}
		
		String sql = "INSERT INTO SQL_TEMPLATE_PARAMETER (TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, " +
		             "PARAMETER_ORDER, IS_REQUIRED, DEFAULT_VALUE, IS_READONLY, IS_HIDDEN, IS_DISABLED, " +
		             "DESCRIPTION, CREATED_TIMESTAMP) " +
		             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT TIMESTAMP)";
		
		for (SqlTemplateParameter param : parameters) {
			jdbcTemplate.update(sql,
				templateId,
				param.getParameterName(),
				param.getParameterType(),
				param.getParameterOrder(),
				param.getIsRequired(),
				param.getDefaultValue(),
				param.getIsReadonly(),
				param.getIsHidden(),
				param.getIsDisabled(),
				param.getDescription());
		}
	}

	/**
	 * 단축키 저장
	 */
	private void saveShortcuts(String templateId, List<SqlTemplateShortcut> shortcuts) {
		if (shortcuts == null || shortcuts.isEmpty()) {
			return;
		}
		
		String sql = "INSERT INTO SQL_TEMPLATE_SHORTCUT (SOURCE_TEMPLATE_ID, TARGET_TEMPLATE_ID, " +
		             "SHORTCUT_KEY, SHORTCUT_NAME, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, " +
		             "AUTO_EXECUTE, IS_ACTIVE, CREATED_TIMESTAMP) " +
		             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT TIMESTAMP)";
		
		for (SqlTemplateShortcut shortcut : shortcuts) {
			jdbcTemplate.update(sql,
				templateId,
				shortcut.getTargetTemplateId(),
				shortcut.getShortcutKey(),
				shortcut.getShortcutName(),
				shortcut.getShortcutDescription(),
				shortcut.getSourceColumnIndexes(),
				shortcut.getAutoExecute(),
				shortcut.getIsActive());
		}
	}

	/**
	 * SQL 내용 저장
	 */
	private void saveSqlContents(String templateId, List<SqlContent> sqlContents, String userId) {
		if (sqlContents == null || sqlContents.isEmpty()) {
			return;
		}
		
		String sql = "INSERT INTO SQL_CONTENT (TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, VERSION, " +
		             "CREATED_BY, CREATED_TIMESTAMP) " +
		             "VALUES (?, ?, ?, ?, ?, CURRENT TIMESTAMP)";
		
		for (SqlContent content : sqlContents) {
			jdbcTemplate.update(sql,
				templateId,
				content.getConnectionId(),
				content.getSqlContent(),
				content.getVersion(),
				userId);
		}
	}

	/**
	 * 템플릿 존재 여부 확인
	 */
	private boolean templateExists(String templateId) {
		String sql = "SELECT COUNT(*) FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, templateId);
		return count != null && count > 0;
	}

	/**
	 * 템플릿 기본 정보 업데이트
	 */
	private void updateTemplateInfo(SqlTemplateInfo template, String userId) {
		String sql = "UPDATE SQL_TEMPLATE SET " +
		             "TEMPLATE_NAME = ?, TEMPLATE_DESC = ?, SQL_CONTENT = ?, " +
		             "ACCESSIBLE_CONNECTION_IDS = ?, TEMPLATE_TYPE = ?, VERSION = ?, " +
		             "STATUS = ?, EXECUTION_LIMIT = ?, REFRESH_TIMEOUT = ?, " +
		             "NEWLINE = ?, AUDIT = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
		             "WHERE TEMPLATE_ID = ?";
		
		jdbcTemplate.update(sql,
			template.getTemplateName(),
			template.getTemplateDesc(),
			template.getSqlContent(),
			template.getAccessibleConnectionIds() != null ? String.join(",", template.getAccessibleConnectionIds()) : null,
			template.getTemplateType() != null ? template.getTemplateType() : "SQL",
			template.getVersion() != null ? template.getVersion() : 1,
			template.getStatus() != null ? template.getStatus() : "ACTIVE",
			template.getExecutionLimit() != null ? template.getExecutionLimit() : 0,
			template.getRefreshTimeout() != null ? template.getRefreshTimeout() : 0,
			template.getNewline() != null ? template.getNewline() : true,
			template.getAudit() != null ? template.getAudit() : false,
			userId,
			template.getTemplateId()
		);
	}

	/**
	 * 카테고리 매핑 업데이트
	 */
	private void updateCategoryMappings(String templateId, List<String> categories, String userId) {
		// 기존 카테고리 매핑 삭제
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING WHERE TEMPLATE_ID = ?", templateId);
		
		// 새 카테고리 매핑 저장
		saveCategoryMappings(templateId, categories, userId);
	}

	/**
	 * 파라미터 업데이트
	 */
	private void updateParameters(String templateId, List<SqlTemplateParameter> parameters) {
		// 기존 파라미터 삭제
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ?", templateId);
		
		// 새 파라미터 저장
		saveParameters(templateId, parameters);
	}

	/**
	 * 단축키 업데이트
	 */
	private void updateShortcuts(String templateId, List<SqlTemplateShortcut> shortcuts) {
		// 기존 단축키 삭제 (SOURCE_TEMPLATE_ID만 - 이 템플릿에서 나가는 단축키만)
		jdbcTemplate.update("DELETE FROM SQL_TEMPLATE_SHORTCUT WHERE SOURCE_TEMPLATE_ID = ?", templateId);
		
		// 새 단축키 저장
		saveShortcuts(templateId, shortcuts);
	}

	/**
	 * SQL 내용 업데이트
	 */
	private void updateSqlContents(String templateId, List<SqlContent> sqlContents, String userId) {
		// 기존 SQL 내용 삭제
		jdbcTemplate.update("DELETE FROM SQL_CONTENT WHERE TEMPLATE_ID = ?", templateId);
		
		// 새 SQL 내용 저장
		saveSqlContents(templateId, sqlContents, userId);
	}

	/**
	 * 템플릿 단축키 조회
	 */
	public List<Map<String, Object>> getShortKeys(String templateId) {
		try {
			String sql = "SELECT * FROM SQL_TEMPLATE_SHORTCUT WHERE SOURCE_TEMPLATE_ID = ? ORDER BY SHORTCUT_NAME";
			return jdbcTemplate.queryForList(sql, templateId);
		} catch (Exception e) {
			logger.error("단축키 조회 실패 - templateId: {}", templateId, e);
			return new ArrayList<>();
		}
	}
	
	/**
	 * SFTP 연결 정보 조회
	 */
	public Map<String, Object> getConnectionInfo(String connectionId) {
		Map<String, Object> result = new HashMap<>();
		
		try {
			String sql = "SELECT * FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ?";
			List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql, connectionId);
			
			if (!connections.isEmpty()) {
				result.put("success", true);
				result.put("data", connections.get(0));
			} else {
				result.put("success", false);
				result.put("error", "SFTP 연결을 찾을 수 없습니다: " + connectionId);
			}
		} catch (Exception e) {
			logger.error("SFTP 연결 정보 조회 실패 - connectionId: {}", connectionId, e);
			result.put("success", false);
			result.put("error", "SFTP 연결 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
		
		return result;
	}
	
}


