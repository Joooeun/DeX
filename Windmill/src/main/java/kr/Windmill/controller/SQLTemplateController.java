package kr.Windmill.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.dto.SqlTemplateSaveRequest;
import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.SqlContentService;
import kr.Windmill.service.PermissionService;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.util.Common;
import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.dto.SqlTemplateSaveRequest;
import kr.Windmill.dto.ValidationResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

@Controller
public class SQLTemplateController {

	private static final Logger logger = LoggerFactory.getLogger(SQLTemplateController.class);

	@Autowired
	private SqlTemplateService sqlTemplateService;

	@Autowired
	private SqlContentService sqlContentService;

	@Autowired
	private PermissionService permissionService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SQLExecuteService sqlExecuteService;

	@Autowired
	private SystemConfigService systemConfigService;

	@Autowired
	private Common com;

	@RequestMapping(path = "/SQLTemplate", method = RequestMethod.GET)
	public ModelAndView sqlTemplateMain(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");
		String templateId = request.getParameter("templateId");

		// 로그인 확인
		if (userId == null) {
			mv.setViewName("redirect:/index");
			return mv;
		}

		// templateId가 있으면 특정 템플릿 실행 페이지로 이동
		if (templateId != null && !templateId.trim().isEmpty()) {
			return executeSqlTemplate(request, mv, session, templateId);
		}

		// SQL 템플릿 메뉴 권한 확인
		if (!permissionService.checkMenuPermission(userId, "MENU_SQL_TEMPLATE")) {
			logger.warn("SQL 템플릿 메뉴 권한 없음 - userId: {}", userId);
			mv.setViewName("redirect:/index");
			return mv;
		}

		mv.setViewName("SQLTemplate");
		return mv;
	}

	/**
	 * 특정 SQL 템플릿 실행 페이지
	 */
	private ModelAndView executeSqlTemplate(HttpServletRequest request, ModelAndView mv, HttpSession session, String templateId) {
		String userId = (String) session.getAttribute("memberId");

		try {
			// 템플릿 정보 조회
			Map<String, Object> templateResult = sqlTemplateService.getSqlTemplateDetail(templateId);

			if (templateResult == null || !(Boolean) templateResult.get("success")) {
				mv.addObject("error", "템플릿을 찾을 수 없습니다.");
				mv.setViewName("error");
				return mv;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> templateInfo = (Map<String, Object>) templateResult.get("data");

			// 템플릿 정보를 모델에 추가
			mv.addObject("templateId", templateId);
			mv.addObject("templateName", templateInfo.get("sqlName"));
			mv.addObject("templateDescription", templateInfo.get("sqlDesc"));
			mv.addObject("sqlContent", templateInfo.get("sqlContent"));
			mv.addObject("limit", templateInfo.get("executionLimit"));
			mv.addObject("refreshtimeout", templateInfo.get("refreshTimeout"));
			mv.addObject("newline", templateInfo.get("newline"));
			mv.addObject("Connection", session.getAttribute("connectionId"));
			mv.addObject("Excute", request.getParameter("Excute") == null ? false : request.getParameter("Excute"));

			// DownloadEnable 설정 (IP 기반)
			String clientIp = request.getRemoteAddr();
			String downloadIpPattern = systemConfigService.getConfigValue("DOWNLOAD_IP_PATTERN", "10.240.13.*");
			boolean downloadEnable = isIpAllowed(com.getIp(request), downloadIpPattern);
			mv.addObject("DownloadEnable", downloadEnable);

			// 파라미터 정보 조회
			Map<String, Object> paramResult = sqlTemplateService.getTemplateParameters(templateId);
			if (paramResult.get("success").equals(true)) {
				mv.addObject("parameters", paramResult.get("data"));
				mv.addObject("sendvalue", request.getParameter("sendvalue"));
			}

			// 단축키 정보 조회
			Map<String, Object> shortcutResult = sqlTemplateService.getTemplateShortcuts(templateId);
			if (shortcutResult.get("success").equals(true)) {
				mv.addObject("ShortKey", shortcutResult.get("data"));
			}

			// 사용자 ID 추가
			mv.addObject("memberId", userId);

			// Path 변수 추가 (템플릿 ID 사용)
			mv.addObject("Path", templateId);

			// 접근 가능한 DB 연결 정보 조회
			List<Map<String, Object>> connections = sqlTemplateService.getAccessibleConnections(templateId, userId);
			mv.addObject("connections", connections);

			mv.setViewName("SQLExecute");
			return mv;

		} catch (Exception e) {
			logger.error("SQL 템플릿 실행 페이지 로드 실패", e);
			mv.addObject("error", "템플릿 로드 중 오류가 발생했습니다.");
			mv.setViewName("error");
			return mv;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/tree")
	public List<Map<String, Object>> getSqlTemplateTree(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			return null;
		}

		try {
			// DB 기반 트리 반환
			return sqlTemplateService.getFullMenuTree();
		} catch (Exception e) {
			logger.error("SQL 템플릿 트리 조회 실패", e);
			return null;
		}
	}

	// 사용자가 접근 가능한 카테고리 목록 조회 (관리자 전용)
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/categories")
	public Map<String, Object> getAuthorizedCategories(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			// 관리자는 모든 카테고리 접근 가능
			result.put("success", true);
			result.put("data", permissionService.getAllCategories());
		} catch (Exception e) {
			logger.error("카테고리 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "카테고리 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/detail")
	public Map<String, Object> getSqlTemplateDetail(HttpServletRequest request, HttpSession session) {
		String templateId = request.getParameter("templateId");

		try {
			return sqlTemplateService.getSqlTemplateDetail(templateId);
		} catch (Exception e) {
			logger.error("SQL 템플릿 상세 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 템플릿 조회 실패: " + e.getMessage());
			return result;
		}
	}

	/**
	 * 템플릿 전체 정보 통합 조회 (저장 구조와 동일)
	 * 파라미터, 단축키, SQL 컨텐츠를 한번에 조회
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/full-detail")
	public Map<String, Object> getFullTemplateDetail(HttpServletRequest request, HttpSession session) {
		String templateId = request.getParameter("templateId");

		try {
			return sqlTemplateService.getFullTemplateDetail(templateId);
		} catch (Exception e) {
			logger.error("SQL 템플릿 통합 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 템플릿 통합 조회 실패: " + e.getMessage());
			return result;
		}
	}


	/**
	 * SQL 템플릿 저장 (새로운 JSON 형식)
	 * 깔끔하고 타입 안전한 데이터 구조 사용
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/save", method = RequestMethod.POST, 
	                consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String, Object>> saveSqlTemplate(
			@RequestBody SqlTemplateSaveRequest request,
			HttpSession session) {
		
		String userId = (String) session.getAttribute("memberId");
		
		try {
			// 1. 데이터 검증
			ValidationResult validation = validateSaveRequest(request);
			if (!validation.isValid()) {
				return ResponseEntity.badRequest()
					.body(createErrorResponse(validation.getErrorMessage(), validation.getErrorCode()));
			}
			
			// 2. 템플릿 ID 설정 (신규인 경우)
			if (StringUtils.isEmpty(request.getTemplate().getTemplateId())) {
				String templateName = request.getTemplate().getTemplateName();
				
				// 중복 체크
				if (isTemplateIdExists(templateName)) {
					return ResponseEntity.badRequest()
						.body(createErrorResponse("이미 존재하는 템플릿 이름입니다: " + templateName, "DUPLICATE_TEMPLATE_NAME"));
				}
				
				// 템플릿 이름을 그대로 템플릿 ID로 사용
				request.getTemplate().setTemplateId(String.valueOf(System.currentTimeMillis()));
			}
			
			// 3. 서비스 호출
			Map<String, Object> result = sqlTemplateService.saveTemplateWithRelatedData(request, userId);
			
			if ((Boolean) result.get("success")) {
				return ResponseEntity.ok(result);
			} else {
				return ResponseEntity.status(500).body(result);
			}
			
		} catch (Exception e) {
			logger.error("SQL 템플릿 저장 실패", e);
			return ResponseEntity.status(500)
				.body(createErrorResponse("SQL 템플릿 저장 실패: " + e.getMessage(), "SAVE_ERROR"));
		}
	}

	/**
	 * 데이터 검증 메서드
	 */
	private ValidationResult validateSaveRequest(SqlTemplateSaveRequest request) {
		ValidationResult result = new ValidationResult();
		
		// 템플릿 기본 정보 검증
		if (request.getTemplate() == null) {
			return result.addError("템플릿 정보가 누락되었습니다. 템플릿의 기본 정보(이름, 설명, SQL 내용 등)를 모두 입력해주세요.", "TEMPLATE_REQUIRED");
		}
		
		if (StringUtils.isEmpty(request.getTemplate().getTemplateName())) {
			return result.addError("템플릿 이름을 입력해주세요. 템플릿을 식별할 수 있는 고유한 이름이 필요합니다.", "TEMPLATE_NAME_REQUIRED");
		}
		
		if (StringUtils.isEmpty(request.getTemplate().getSqlContent())) {
			return result.addError("기본 SQL 내용을 입력해주세요. 템플릿의 기본 SQL 쿼리가 필요합니다.", "SQL_CONTENT_REQUIRED");
		}
		
		// 템플릿 이름 길이 검증
		if (request.getTemplate().getTemplateName().length() > 100) {
			return result.addError("템플릿 이름이 너무 깁니다. 100자 이하로 입력해주세요. (현재: " + request.getTemplate().getTemplateName().length() + "자)", "TEMPLATE_NAME_TOO_LONG");
		}
		
		// 파라미터 검증
		if (request.getParameters() != null) {
			for (int i = 0; i < request.getParameters().size(); i++) {
				kr.Windmill.dto.SqlTemplateParameter param = request.getParameters().get(i);
				if (StringUtils.isEmpty(param.getParameterName())) {
					return result.addError("파라미터 " + (i + 1) + "번의 이름을 입력해주세요. 파라미터는 SQL 쿼리에서 사용할 변수명입니다.", "PARAMETER_NAME_REQUIRED");
				}
				if (param.getParameterOrder() == null || param.getParameterOrder() < 1) {
					return result.addError("파라미터 " + (i + 1) + "번의 순서를 1 이상의 숫자로 입력해주세요. (현재: " + param.getParameterOrder() + ")", "PARAMETER_ORDER_INVALID");
				}
				if (param.getParameterName().length() > 50) {
					return result.addError("파라미터 " + (i + 1) + "번의 이름이 너무 깁니다. 50자 이하로 입력해주세요. (현재: " + param.getParameterName().length() + "자)", "PARAMETER_NAME_TOO_LONG");
				}
				// 파라미터 타입 검증
				if (StringUtils.isEmpty(param.getParameterType())) {
					return result.addError("파라미터 " + (i + 1) + "번의 타입을 선택해주세요. (STRING, NUMBER, TEXT, SQL, LOG 중 선택)", "PARAMETER_TYPE_REQUIRED");
				}
				if (!isValidParameterType(param.getParameterType())) {
					return result.addError("파라미터 " + (i + 1) + "번의 타입이 올바르지 않습니다. STRING, NUMBER, TEXT, SQL, LOG 중 하나를 선택해주세요. (현재: " + param.getParameterType() + ")", "PARAMETER_TYPE_INVALID");
				}
			}
		}
		
		// 단축키 검증
		if (request.getShortcuts() != null) {
			for (int i = 0; i < request.getShortcuts().size(); i++) {
				kr.Windmill.dto.SqlTemplateShortcut shortcut = request.getShortcuts().get(i);
				if (StringUtils.isEmpty(shortcut.getShortcutKey())) {
					return result.addError("단축키 " + (i + 1) + "번의 키를 입력해주세요. 단축키는 사용자가 빠르게 접근할 수 있는 키 조합입니다.", "SHORTCUT_KEY_REQUIRED");
				}
				if (StringUtils.isEmpty(shortcut.getTargetTemplateId())) {
					return result.addError("단축키 " + (i + 1) + "번의 대상 템플릿 ID를 입력해주세요. 단축키가 실행할 템플릿을 지정해야 합니다.", "TARGET_TEMPLATE_ID_REQUIRED");
				}
				if (StringUtils.isEmpty(shortcut.getShortcutName())) {
					return result.addError("단축키 " + (i + 1) + "번의 이름을 입력해주세요. 단축키를 식별할 수 있는 이름이 필요합니다.", "SHORTCUT_NAME_REQUIRED");
				}
			}
		}
		
		// SQL 내용 검증
		if (request.getSqlContents() != null) {
			for (int i = 0; i < request.getSqlContents().size(); i++) {
				kr.Windmill.dto.SqlContent sqlContent = request.getSqlContents().get(i);
				if (StringUtils.isEmpty(sqlContent.getConnectionId())) {
					return result.addError("SQL 내용 " + (i + 1) + "번의 연결 ID를 입력해주세요. 데이터베이스 연결을 식별하는 ID가 필요합니다.", "CONNECTION_ID_REQUIRED");
				}
				// 임시 주석처리
				// if (StringUtils.isEmpty(sqlContent.getSqlContent())) {
				// 	return result.addError("SQL 내용 " + sqlContent.getConnectionId() + "탭의 SQL 쿼리를 입력해주세요. 해당 연결에서 실행할 SQL 문이 필요합니다.", "SQL_CONTENT_REQUIRED");
				// }
			}
		}
		
		return result;
	}
	
	/**
	 * 파라미터 타입이 유효한지 검증
	 */
	private boolean isValidParameterType(String parameterType) {
		if (StringUtils.isEmpty(parameterType)) {
			return false;
		}
		return parameterType.equals("STRING") || parameterType.equals("NUMBER") || 
			   parameterType.equals("TEXT") || parameterType.equals("SQL") || 
			   parameterType.equals("LOG");
	}


	/**
	 * 에러 응답 생성
	 */
	private Map<String, Object> createErrorResponse(String message, String errorCode) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("error", message);
		response.put("errorCode", errorCode);
		return response;
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

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/delete")
	public Map<String, Object> deleteSqlTemplate(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		try {
			String templateId = request.getParameter("templateId");

			if (!permissionService.isAdmin(userId)) {
				Map<String, Object> result = new HashMap<>();
				result.put("success", false);
				result.put("error", "관리자만 삭제할 수 있습니다.");
				return result;
			}

			return sqlTemplateService.deleteSqlTemplate(templateId, userId);

		} catch (Exception e) {
			logger.error("SQL 템플릿 삭제 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 템플릿 삭제 실패: " + e.getMessage());
			return result;
		}
	}


	/**
	 * 간단한 SQL 문법 검증
	 */
	private boolean validateSqlSyntax(String sqlContent) {
		if (sqlContent == null || sqlContent.trim().isEmpty()) {
			return false;
		}

		String sql = sqlContent.trim().toUpperCase();

		// 기본적인 SQL 키워드 검증
		boolean hasSelect = sql.contains("SELECT");
		boolean hasFrom = sql.contains("FROM");

		// SELECT 문인 경우
		if (hasSelect && hasFrom) {
			return true;
		}

		// INSERT, UPDATE, DELETE 문인 경우
		if (sql.contains("INSERT") || sql.contains("UPDATE") || sql.contains("DELETE")) {
			return true;
		}

		// CALL 문인 경우
		if (sql.contains("CALL")) {
			return true;
		}

		return false;
	}

	// =====================================================
	// 카테고리 관리 엔드포인트
	// =====================================================

	/**
	 * 카테고리 목록 조회
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/list")
	public Map<String, Object> getCategoryList(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			List<Map<String, Object>> categories = sqlTemplateService.getCategories();
			result.put("success", true);
			result.put("data", categories);
		} catch (Exception e) {
			logger.error("카테고리 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "카테고리 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	/**
	 * 카테고리 생성
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/create", method = RequestMethod.POST)
	public Map<String, Object> createCategory(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String categoryName = request.getParameter("categoryName");
			String description = request.getParameter("description");

			return sqlTemplateService.createCategory(categoryName, description, userId);
		} catch (Exception e) {
			logger.error("카테고리 생성 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "카테고리 생성 실패: " + e.getMessage());
			return result;
		}
	}

	/**
	 * 카테고리 수정
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/update", method = RequestMethod.POST)
	public Map<String, Object> updateCategory(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String categoryId = request.getParameter("categoryId");
			String categoryName = request.getParameter("categoryName");
			String description = request.getParameter("description");

			return sqlTemplateService.updateCategory(categoryId, categoryName, description, userId);
		} catch (Exception e) {
			logger.error("카테고리 수정 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "카테고리 수정 실패: " + e.getMessage());
			return result;
		}
	}

	/**
	 * 카테고리 삭제
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/delete", method = RequestMethod.POST)
	public Map<String, Object> deleteCategory(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String categoryId = request.getParameter("categoryId");

			return sqlTemplateService.deleteCategory(categoryId, userId);
		} catch (Exception e) {
			logger.error("카테고리 삭제 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "카테고리 삭제 실패: " + e.getMessage());
			return result;
		}
	}

	/**
	 * 카테고리별 템플릿 목록 조회
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/templates")
	public Map<String, Object> getTemplatesByCategory(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String categoryId = request.getParameter("categoryId");
			List<Map<String, Object>> templates = sqlTemplateService.getTemplatesByCategory(categoryId);

			result.put("success", true);
			result.put("data", templates);
		} catch (Exception e) {
			logger.error("카테고리별 템플릿 조회 실패", e);
			result.put("success", false);
			result.put("message", "템플릿 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	/**
	 * 템플릿을 카테고리에 할당
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/assign", method = RequestMethod.POST)
	public Map<String, Object> assignTemplateToCategory(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String templateId = request.getParameter("templateId");
			String categoryId = request.getParameter("categoryId");

			return sqlTemplateService.assignTemplateToCategory(templateId, categoryId, userId);
		} catch (Exception e) {
			logger.error("템플릿 카테고리 할당 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "템플릿 할당 실패: " + e.getMessage());
			return result;
		}
	}

	/**
	 * 카테고리 상세 정보 조회
	 */
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/detail")
	public Map<String, Object> getCategoryDetail(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (!permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}

		try {
			String categoryId = request.getParameter("categoryId");
			Map<String, Object> category = sqlTemplateService.getCategoryDetail(categoryId);

			if (category != null) {
				result.put("success", true);
				result.put("data", category);
			} else {
				result.put("success", false);
				result.put("message", "카테고리를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			logger.error("카테고리 상세 조회 실패", e);
			result.put("success", false);
			result.put("message", "카테고리 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/category/reorder", method = RequestMethod.POST)
	public Map<String, Object> reorderCategories(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("userId");
		Map<String, Object> result = new HashMap<>();

		try {
			String categoryId = request.getParameter("categoryId");
			String direction = request.getParameter("direction"); // "up" or "down"

			if (categoryId == null || direction == null) {
				result.put("success", false);
				result.put("message", "필수 파라미터가 누락되었습니다.");
				return result;
			}

			return sqlTemplateService.reorderCategory(categoryId, direction, userId);
		} catch (Exception e) {
			logger.error("카테고리 순서 변경 실패", e);
			result.put("success", false);
			result.put("message", "카테고리 순서 변경 중 오류가 발생했습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/parameters")
	public Map<String, Object> getTemplateParameters(HttpServletRequest request, HttpSession session) {
		String templateId = request.getParameter("templateId");

		try {
			return sqlTemplateService.getTemplateParameters(templateId);
		} catch (Exception e) {
			logger.error("템플릿 파라미터 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "파라미터 조회 실패: " + e.getMessage());
			return result;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/shortcuts")
	public Map<String, Object> getTemplateShortcuts(HttpServletRequest request, HttpSession session) {
		String templateId = request.getParameter("templateId");

		try {
			return sqlTemplateService.getTemplateShortcuts(templateId);
		} catch (Exception e) {
			logger.error("템플릿 단축키 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "단축키 조회 실패: " + e.getMessage());
			return result;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/list")
	public Map<String, Object> getTemplateList(HttpServletRequest request, HttpSession session) {
		try {
			return sqlTemplateService.getTemplateList();
		} catch (Exception e) {
			logger.error("템플릿 목록 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "템플릿 목록 조회 실패: " + e.getMessage());
			return result;
		}
	}

	// =====================================================
	// SQL 내용 관리 API
	// =====================================================

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/sql-contents")
	public Map<String, Object> getSqlContents(HttpServletRequest request, HttpSession session) {
		String templateId = request.getParameter("templateId");

		try {
			List<Map<String, Object>> contents = sqlContentService.getSqlContentsByTemplate(templateId);
			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("data", contents);
			return result;
		} catch (Exception e) {
			logger.error("SQL 내용 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 내용 조회 실패: " + e.getMessage());
			return result;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/sql-content/save")
	public Map<String, Object> saveSqlContent(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		try {
			String templateId = request.getParameter("templateId");
			String connectionId = request.getParameter("connectionId");
			String sqlContent = request.getParameter("sqlContent");

			return sqlContentService.saveSqlContent(templateId, connectionId, sqlContent, userId);

		} catch (Exception e) {
			logger.error("SQL 내용 저장 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 내용 저장 실패: " + e.getMessage());
			return result;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/sql-content/delete")
	public Map<String, Object> deleteSqlContent(HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		try {
			String templateId = request.getParameter("templateId");
			String connectionId = request.getParameter("connectionId");

			return sqlContentService.deleteSqlContent(templateId, connectionId, userId);

		} catch (Exception e) {
			logger.error("SQL 내용 삭제 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "SQL 내용 삭제 실패: " + e.getMessage());
			return result;
		}
	}


	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/db-connections")
	public Map<String, Object> getDbConnections(HttpServletRequest request, HttpSession session) {
		try {
			String sql = "SELECT CONNECTION_ID, DB_TYPE, HOST_IP, PORT, DATABASE_NAME, USERNAME, STATUS "
					+ "FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY DB_TYPE, CONNECTION_ID";
			List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("data", connections);
			return result;
		} catch (Exception e) {
			logger.error("DB 연결 목록 조회 실패", e);
			Map<String, Object> result = new HashMap<>();
			result.put("success", false);
			result.put("error", "DB 연결 목록 조회 실패: " + e.getMessage());
			return result;
		}
	}

	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/execute", method = RequestMethod.POST)
	public Map<String, Object> executeTemplate(@ModelAttribute SqlTemplateExecuteDto executeDto, HttpServletRequest request, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");
		Map<String, Object> result = new HashMap<>();

		try {
			// 세션 정보 설정
			executeDto.setMemberId(userId);
			executeDto.setIp(request.getRemoteAddr());

			// 필수 파라미터 검증
			if (executeDto.getTemplateId() == null || executeDto.getTemplateId().trim().isEmpty()) {
				result.put("success", false);
				result.put("error", "템플릿 ID가 필요합니다.");
				return result;
			}

			// limit 기본값 설정
			if (executeDto.getLimit() == null) {
				executeDto.setLimit(1000);
			}

			// 템플릿에서 audit 설정 조회
			try {
				Map<String, Object> templateInfo = sqlTemplateService.getSqlTemplateDetail(executeDto.getTemplateId());
				if (templateInfo.get("success").equals(true)) {
					@SuppressWarnings("unchecked")
					Map<String, Object> templateData = (Map<String, Object>) templateInfo.get("data");
					Boolean audit = (Boolean) templateData.get("audit");
					executeDto.setAudit(audit != null ? audit : false);
					executeDto.setTemplateName((String) templateData.get("sqlName"));

					logger.debug("템플릿 audit 설정 조회: templateId={}, audit={}", executeDto.getTemplateId(), audit);
				} else {
					executeDto.setAudit(false);
					logger.warn("템플릿 정보 조회 실패: {}", templateInfo.get("error"));
				}
			} catch (Exception e) {
				logger.warn("템플릿 audit 설정 조회 실패: {}", e.getMessage());
				executeDto.setAudit(false);
			}

			// 파라미터 파싱
			if (executeDto.getParameters() != null && !executeDto.getParameters().trim().isEmpty()) {
				try {
					List<Map<String, Object>> paramList = com.getListFromString(executeDto.getParameters());
					executeDto.setParameterList(paramList);
				} catch (Exception e) {
					logger.warn("파라미터 JSON 파싱 실패: {}", e.getMessage());
					result.put("success", false);
					result.put("error", "파라미터 형식이 올바르지 않습니다.");
					return result;
				}
			}

			// SQL 실행
			Map<String, List> executionResult = sqlExecuteService.executeTemplateSQL(executeDto);

			result.put("success", true);
			result.put("data", executionResult);
			
			// 실행시간 정보 추가
			if (executeDto.getExecutionTime() != null) {
				result.put("executionTime", executeDto.getExecutionTime().toMillis());
			}
			if (executeDto.getRows() != null) {
				result.put("rows", executeDto.getRows());
			}

		} catch (Exception e) {
			logger.error("SQL 템플릿 실행 중 오류 발생: {}", e.getMessage(), e);
			result.put("success", false);
			result.put("error", e.getMessage());
		}

		return result;
	}

	/**
	 * 차트 매핑 중복 체크 (DEPRECATED - 차트 매핑 기능 제거됨)
	 * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
	 */
	@Deprecated
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/chart-mapping/check", method = RequestMethod.POST)
	public Map<String, Object> checkChartMappingDuplicate(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		
		// 차트 매핑 기능이 제거되어 항상 중복 없음으로 반환
		result.put("success", true);
		result.put("exists", false);
		result.put("message", "차트 매핑 기능이 제거되었습니다.");
		
		return result;
	}

	/**
	 * 차트 매핑 업데이트 (DEPRECATED - 차트 매핑 기능 제거됨)
	 * @deprecated 차트 매핑 기능이 TEMPLATE_TYPE으로 대체됨
	 */
	@Deprecated
	@ResponseBody
	@RequestMapping(path = "/SQLTemplate/chart-mapping/update", method = RequestMethod.POST)
	public Map<String, Object> updateChartMapping(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		
		// 차트 매핑 기능이 제거되어 항상 성공으로 반환
		result.put("success", true);
		result.put("message", "차트 매핑 기능이 제거되었습니다.");
		
		return result;
	}
	
	/**
	 * IP 주소가 허용된 패턴과 일치하는지 확인합니다.
	 * @param clientIp 클라이언트 IP 주소
	 * @param pattern IP 패턴 (예: 10.240.13.*, 192.168.1.0/24, *)
	 * @return 허용 여부
	 */
	private boolean isIpAllowed(String clientIp, String pattern) {
		if (clientIp == null || pattern == null) {
			return false;
		}
		
		// 모든 IP 허용
		if ("*".equals(pattern.trim())) {
			return true;
		}
		
		// 와일드카드 패턴 처리 (예: 10.240.13.*)
		if (pattern.contains("*")) {
			// *를 정규식의 .*로 변환하고 .을 \.로 이스케이프
			String regex = pattern.replace(".", "\\.").replace("*", ".*");
			return clientIp.matches(regex);
		}
		
		// 정확한 IP 매칭
		return clientIp.equals(pattern);
	}
}
