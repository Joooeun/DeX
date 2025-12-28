package kr.Windmill.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.PermissionService;

@Controller
public class ETLController {

	private static final Logger logger = LoggerFactory.getLogger(ETLController.class);

	@Autowired
	private SqlTemplateService sqlTemplateService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PermissionService permissionService;

	/**
	 * ETL 작업 목록 화면
	 */
	@RequestMapping(path = "/ETL", method = RequestMethod.GET)
	public ModelAndView etlJobList(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");

		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			mv.setViewName("redirect:/index");
			return mv;
		}

		mv.setViewName("ETLJobList");
		return mv;
	}

	/**
	 * ETL 작업 편집 화면
	 */
	@RequestMapping(path = "/ETL/editor", method = RequestMethod.GET)
	public ModelAndView etlJobEditor(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		String userId = (String) session.getAttribute("memberId");
		String jobId = request.getParameter("jobId");

		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			mv.setViewName("redirect:/index");
			return mv;
		}

		// 편집 모드인 경우 jobId 전달
		if (jobId != null && !jobId.trim().isEmpty()) {
			mv.addObject("jobId", jobId);
		}

		mv.setViewName("ETLJobEditor");
		return mv;
	}

	/**
	 * ETL 작업 목록 조회 (데모용 - 더미 데이터)
	 */
	@ResponseBody
	@RequestMapping(path = "/ETL/jobs", method = RequestMethod.GET)
	public Map<String, Object> getETLJobs(HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");
		
		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}
		
		try {
			// 데모용 더미 데이터
			java.util.List<Map<String, Object>> jobs = new java.util.ArrayList<>();
			
			Map<String, Object> job1 = new HashMap<>();
			job1.put("jobId", "ETL001");
			job1.put("jobName", "사용자 데이터 이관");
			job1.put("jobDescription", "사용자 테이블을 타겟 DB로 이관");
			job1.put("sourceTemplateId", "TMP001");
			job1.put("sourceTemplateName", "사용자 조회");
			job1.put("sourceConnectionId", "pg");
			job1.put("targetConnectionId", "pgmac");
			job1.put("targetTableName", "USER_DATA");
			job1.put("status", "ACTIVE");
			job1.put("lastExecutionTime", "2024-01-15 10:30:00");
			job1.put("lastExecutionStatus", "SUCCESS");
			jobs.add(job1);
			
			Map<String, Object> job2 = new HashMap<>();
			job2.put("jobId", "ETL002");
			job2.put("jobName", "주문 데이터 동기화");
			job2.put("jobDescription", "주문 정보를 실시간으로 동기화");
			job2.put("sourceTemplateId", "TMP002");
			job2.put("sourceTemplateName", "주문 조회");
			job2.put("sourceConnectionId", "pg");
			job2.put("targetConnectionId", "pgmac");
			job2.put("targetTableName", "ORDER_DATA");
			job2.put("status", "ACTIVE");
			job2.put("lastExecutionTime", "2024-01-15 11:15:00");
			job2.put("lastExecutionStatus", "SUCCESS");
			jobs.add(job2);
			
			result.put("success", true);
			result.put("data", jobs);
		} catch (Exception e) {
			logger.error("ETL 작업 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "작업 목록 조회 중 오류가 발생했습니다.");
		}
		
		return result;
	}

	/**
	 * 소스 데이터 미리보기 (데모용)
	 */
	@ResponseBody
	@RequestMapping(path = "/ETL/preview", method = RequestMethod.POST)
	public Map<String, Object> previewSourceData(HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");
		
		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}
		
		try {
			// 데모용 더미 데이터
			java.util.List<String> headers = new java.util.ArrayList<>();
			headers.add("USER_ID");
			headers.add("USER_NAME");
			headers.add("BIRTH_DATE");
			headers.add("ADDRESS");
			headers.add("AGE");
			
			java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
			java.util.List<String> row1 = new java.util.ArrayList<>();
			row1.add("U001");
			row1.add("홍길동");
			row1.add("1990-01-15");
			row1.add("서울시 강남구 테헤란로 123");
			row1.add("34");
			rows.add(row1);
			
			java.util.List<String> row2 = new java.util.ArrayList<>();
			row2.add("U002");
			row2.add("김철수");
			row2.add("1985-05-20");
			row2.add("부산시 해운대구 해운대해변로 456");
			row2.add("39");
			rows.add(row2);
			
			java.util.List<String> row3 = new java.util.ArrayList<>();
			row3.add("U003");
			row3.add("이영희");
			row3.add("1992-11-30");
			row3.add("대전시 유성구 대학로 789");
			row3.add("32");
			rows.add(row3);
			
			result.put("success", true);
			result.put("rowhead", headers);
			result.put("rowbody", rows);
		} catch (Exception e) {
			logger.error("소스 데이터 미리보기 실패", e);
			result.put("success", false);
			result.put("message", "데이터 미리보기 중 오류가 발생했습니다.");
		}
		
		return result;
	}

	/**
	 * 타겟 SQL 템플릿 목록 조회 (INSERT/UPDATE 타입만)
	 */
	@ResponseBody
	@RequestMapping(path = "/ETL/target-templates", method = RequestMethod.GET)
	public Map<String, Object> getTargetTemplates(HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");
		
		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}
		
		try {
			// INSERT, UPDATE, 또는 타입이 없는 템플릿 조회 (ETL용)
			String sql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, TEMPLATE_TYPE " +
					"FROM SQL_TEMPLATE " +
					"WHERE STATUS = 'ACTIVE' " +
					"AND (TEMPLATE_TYPE IN ('INSERT', 'UPDATE', 'UPSERT') OR TEMPLATE_TYPE IS NULL) " +
					"ORDER BY TEMPLATE_NAME";
			
			List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql);
			
			result.put("success", true);
			result.put("data", templates);
		} catch (Exception e) {
			logger.error("타겟 템플릿 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "타겟 템플릿 목록 조회 중 오류가 발생했습니다.");
		}
		
		return result;
	}

	/**
	 * 타겟 템플릿 상세 조회 및 파라미터 추출
	 */
	@ResponseBody
	@RequestMapping(path = "/ETL/target-template/detail", method = RequestMethod.GET)
	public Map<String, Object> getTargetTemplateDetail(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String userId = (String) session.getAttribute("memberId");
		
		// 관리자 권한 확인
		if (userId == null || !permissionService.isAdmin(userId)) {
			result.put("success", false);
			result.put("message", "관리자 권한이 필요합니다.");
			return result;
		}
		
		String templateId = request.getParameter("templateId");
		
		if (templateId == null || templateId.trim().isEmpty()) {
			result.put("success", false);
			result.put("message", "템플릿 ID가 필요합니다.");
			return result;
		}
		
		try {
			// 템플릿 상세 조회
			Map<String, Object> templateResult = sqlTemplateService.getSqlTemplateDetail(templateId, userId);
			if (!(Boolean) templateResult.get("success")) {
				result.put("success", false);
				result.put("message", "템플릿을 찾을 수 없습니다.");
				return result;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> templateData = (Map<String, Object>) templateResult.get("data");
			String sqlContent = (String) templateData.get("sqlContent");
			
			// SQL에서 ${paramName} 형태의 파라미터 추출
			List<String> parameters = extractParametersFromSql(sqlContent);
			
			// 파라미터 정보 조회 (DB에 저장된 파라미터)
			Map<String, Object> paramResult = sqlTemplateService.getTemplateParameters(templateId);
			List<Map<String, Object>> dbParameters = new ArrayList<>();
			if ((Boolean) paramResult.get("success")) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> params = (List<Map<String, Object>>) paramResult.get("data");
				dbParameters = params;
			}
			
			Map<String, Object> data = new HashMap<>();
			data.put("templateId", templateData.get("templateId"));
			data.put("templateName", templateData.get("sqlName"));
			data.put("templateDesc", templateData.get("sqlDesc"));
			data.put("sqlContent", sqlContent);
			data.put("parameters", parameters); // SQL에서 추출한 파라미터 목록
			data.put("dbParameters", dbParameters); // DB에 저장된 파라미터 정보
			
			result.put("success", true);
			result.put("data", data);
		} catch (Exception e) {
			logger.error("타겟 템플릿 상세 조회 실패", e);
			result.put("success", false);
			result.put("message", "템플릿 상세 조회 중 오류가 발생했습니다.");
		}
		
		return result;
	}

	/**
	 * SQL에서 ${paramName} 형태의 파라미터 추출
	 */
	private List<String> extractParametersFromSql(String sql) {
		List<String> parameters = new ArrayList<>();
		if (sql == null || sql.trim().isEmpty()) {
			return parameters;
		}
		
		// ${paramName} 패턴 찾기
		Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
		Matcher matcher = pattern.matcher(sql);
		
		while (matcher.find()) {
			String paramName = matcher.group(1).trim();
			if (!parameters.contains(paramName)) {
				parameters.add(paramName);
			}
		}
		
		return parameters;
	}
}

