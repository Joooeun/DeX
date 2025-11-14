package kr.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.ShellExecutionService;
import kr.Windmill.util.Common;

@Controller
public class SQLController {

	private static final Logger logger = LoggerFactory.getLogger(SQLController.class);
	private final Common com;
	private final SqlTemplateService sqlTemplateService;
	private final SystemConfigService systemConfigService;
	private final ShellExecutionService shellExecutionService;
	
	@Autowired
	public SQLController(Common common, SqlTemplateService sqlTemplateService, SystemConfigService systemConfigService, ShellExecutionService shellExecutionService) {
		this.com = common;
		this.sqlTemplateService = sqlTemplateService;
		this.systemConfigService = systemConfigService;
		this.shellExecutionService = shellExecutionService;
	}

	@RequestMapping(path = "/SQL")
	public ModelAndView SQLmain(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		// templateId 파라미터가 있으면 SQLTemplateController로 리다이렉트
		String templateId = request.getParameter("templateId");
		if (templateId != null && !templateId.trim().isEmpty()) {
			mv.addObject("sendvalue", request.getParameter("sendvalue"));
			String excuteParam = request.getParameter("Excute");
			try {
				// 한글 templateId를 URL 인코딩하여 HTTP 헤더 오류 방지
				String encodedTemplateId = URLEncoder.encode(templateId, StandardCharsets.UTF_8.toString());
				String redirectUrl = "/SQLTemplate?templateId=" + encodedTemplateId;
				if (excuteParam != null) {
					redirectUrl += "&Excute=" + excuteParam;
				}
				mv.setViewName("redirect:" + redirectUrl);
			} catch (Exception e) {
				logger.error("URL 인코딩 중 오류 발생: " + e.getMessage(), e);
				// 인코딩 실패 시 원본 templateId 사용 (fallback)
				String redirectUrl = "/SQLTemplate?templateId=" + templateId;
				if (excuteParam != null) {
					redirectUrl += "&Excute=" + excuteParam;
				}
				mv.setViewName("redirect:" + redirectUrl);
			}
			mv.addObject("Excute", request.getParameter("Excute") == null ? false : request.getParameter("Excute"));
			return mv;
		}

		// Path 파라미터가 없으면 에러 처리
		String pathParam = request.getParameter("Path");
		if (pathParam == null || pathParam.trim().isEmpty()) {
			mv.addObject("params", com.showMessageAndRedirect("잘못된 요청입니다.", null, "GET"));
			mv.setViewName("common/messageRedirect");
			return mv;
		}

		try {
			File file = new File(pathParam);

			mv.addObject("Path", file.getParent());
			mv.addObject("title", file.getName().replaceAll("\\..*", ""));

			boolean sql = com.FileRead(file).length() > 0;

			file = new File(pathParam.replace(".sql", ".properties"));
			List<Map<String, String>> ShortKey = new ArrayList<>();
			List<Map<String, String>> Param = new ArrayList<>();
			if (file.exists()) {

				String properties = com.FileRead(file);

				int num = 0;

				String values[] = null;

				if (request.getParameter("sendvalue") != null) {
					values = request.getParameter("sendvalue").split("\\s*\\&");
				}

				for (String line : properties.split("\r\n")) {

					if (line.startsWith("#")) {
						continue;
					}
					Map<String, String> map = new HashMap<>();
					if (line.split("=")[0].equals("PARAM")) {
						map.put("name", line.split("=")[1].split("\\&")[0]);
						map.put("type", line.split("=")[1].split("\\&")[1]);

						List vowelsList = Arrays.asList(line.split("=")[1].split("\\&"));

						if (vowelsList.contains("required")) {
							map.put("required", "required");
						}

						if (vowelsList.contains("disabled")) {
							map.put("disabled", "disabled");
						}
						if (vowelsList.contains("readonly")) {
							map.put("readonly", "readonly");
						}
						if (vowelsList.contains("hidden")) {
							map.put("hidden", "hidden");
						}

						if (values != null && values.length > num) {
							map.put("value", values[num++].replaceAll("\\s*$", ""));
						} else {
							map.put("value", "");
						}

						Param.add(map);

					} else if (line.split("=")[0].equals("SHORTKEY")) {
						map.put("key", line.split("=")[1].split("\\&")[0]);
						map.put("keytitle", line.split("=")[1].split("\\&")[1]);
						map.put("menu", line.split("=")[1].split("\\&")[2]);
						map.put("column", line.split("=")[1].split("\\&")[3]);

						List vowelsList = Arrays.asList(line.split("=")[1].split("\\&"));
						map.put("autoExecute", String.valueOf(!vowelsList.contains("disableAutoExecute")));

						ShortKey.add(map);
					} else if (line.split("=")[0].equals("REFRESHTIMEOUT")) {
						mv.addObject("refreshtimeout", line.split("=")[1]);
					} else if (line.split("=")[0].equals("LIMIT")) {
						mv.addObject("limit", line.split("=")[1]);
					} else if (line.split("=")[0].equals("NEWLINE")) {
						mv.addObject("newline", line.split("=")[1].toLowerCase());
					} else if (line.split("=")[0].equals("DB")) {
						mv.addObject("DB", line.split("=")[1]);
					} else if (line.split("=")[0].equals("DESC")) {
						mv.addObject("desc", line.split("=")[1]);
					} else if (line.split("=")[0].equals("SAVE")) {
						mv.addObject("save", line.split("=")[1].toLowerCase());
					}

				}

			}

			String downloadIpPattern = systemConfigService.getConfigValue("DOWNLOAD_IP_PATTERN", "10.240.13.*");
			boolean DownloadEnable = isIpAllowed(com.getIp(request), downloadIpPattern);

			mv.addObject("sql", sql);
			mv.addObject("Param", Param);
			mv.addObject("ShortKey", ShortKey);
			mv.addObject("Excute", request.getParameter("Excute") == null ? false : request.getParameter("Excute"));
			mv.addObject("Connection", session.getAttribute("Connection"));
			mv.addObject("DownloadEnable", DownloadEnable);

		} catch (IOException e) {

			logger.error("메뉴 정보 로드 실패", e);

			mv.addObject("params", com.showMessageAndRedirect("메뉴 정보를 불러오는데 실패했습니다. 관리자에게 문의해 주세요.", null, "GET"));
			mv.setViewName("common/messageRedirect");

		}

		return mv;
	}

	@RequestMapping(path = "/SQLExecute")
	public ModelAndView sqlExecute(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		String templateId = request.getParameter("templateId");
		String templateType = request.getParameter("templateType");
		String userId = (String) session.getAttribute("memberId");
		
		try {
			// 템플릿 정보 조회
			Map<String, Object> templateResult = sqlTemplateService.getSqlTemplateDetail(templateId);
			
			if (templateResult != null && (Boolean) templateResult.get("success")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> templateData = (Map<String, Object>) templateResult.get("data");
				
				// templateType이 없으면 템플릿 데이터에서 가져오기
				if (templateType == null || templateType.isEmpty()) {
					templateType = (String) templateData.get("templateType");
				}
				
				// 공통 데이터 설정
				mv.addObject("templateType", templateType);
				mv.addObject("sqlContent", templateData.get("sqlContent"));
				mv.addObject("templateId", templateId);
				mv.addObject("Excute", request.getParameter("Excute") == null ? false : request.getParameter("Excute"));
				
				mv.addObject("templateName", templateData.get("sqlName"));
				mv.addObject("templateDescription", templateData.get("sqlDesc"));
				mv.addObject("limit", templateData.get("executionLimit"));
				mv.addObject("refreshtimeout", templateData.get("refreshTimeout"));
				mv.addObject("newline", templateData.get("newline"));
				mv.addObject("Connection", session.getAttribute("connectionId"));
				
				
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
				
				// 템플릿 타입에 따른 연결 정보 조회
				if ("SHELL".equals(templateType)) {
					// Shell 타입: SFTP 연결 정보 조회
					List<Map<String, Object>> connections = sqlTemplateService.getAccessibleConnections(templateId, userId);
					mv.addObject("connections", connections);
				} else {
					// SQL 타입: DB 연결 정보 조회
					List<Map<String, Object>> connections = sqlTemplateService.getAccessibleConnections(templateId, userId);
					mv.addObject("connections", connections);
				}
				
				mv.setViewName("SQLExecute");
				return mv;
			} else {
				mv.addObject("error", "템플릿을 찾을 수 없습니다.");
				mv.setViewName("error");
				return mv;
			}
		} catch (Exception e) {
			logger.error("템플릿 실행 페이지 로드 실패", e);
			mv.addObject("error", "템플릿 로드 중 오류가 발생했습니다.");
			mv.setViewName("error");
			return mv;
		}
	}
	
	@RequestMapping(path = "/ShellExecute/run")
	@ResponseBody
	public Map<String, Object> runShell(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		
		try {
			String templateId = request.getParameter("templateId");
			String connectionId = request.getParameter("hostId");  // SFTP 연결 ID
			String parametersJson = request.getParameter("parameters");
			
			// 템플릿에서 스크립트 내용 조회 (SQL 처리와 동일한 방식)
			Map<String, Object> templateResult = sqlTemplateService.getSqlTemplateDetail(templateId);
			if (templateResult == null || !(Boolean) templateResult.get("success")) {
				result.put("success", false);
				result.put("error", "템플릿을 찾을 수 없습니다: " + templateId);
				return result;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> templateData = (Map<String, Object>) templateResult.get("data");
			String script = (String) templateData.get("sqlContent");
			
			if (script == null || script.trim().isEmpty()) {
				result.put("success", false);
				result.put("error", "Shell 스크립트가 비어있습니다.");
				return result;
			}
			
			// SFTP 연결 정보 조회
			Map<String, Object> connectionInfo = sqlTemplateService.getConnectionInfo(connectionId);
			if (connectionInfo == null || !(Boolean) connectionInfo.get("success")) {
				result.put("success", false);
				result.put("error", "SFTP 연결 정보를 찾을 수 없습니다: " + connectionId);
				return result;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> connectionData = (Map<String, Object>) connectionInfo.get("data");
			String hostName = (String) connectionData.get("HOST_NAME");
			String hostIp = (String) connectionData.get("HOST_IP");
			String username = (String) connectionData.get("USERNAME");
			String password = (String) connectionData.get("PASSWORD");
			Integer port = (Integer) connectionData.get("PORT");
			
			// 기본값 설정
			if (port == null) port = 22;
			if (username == null || username.trim().isEmpty()) {
				result.put("success", false);
				result.put("error", "SFTP 연결에 사용자명이 설정되지 않았습니다.");
				return result;
			}
			if (password == null || password.trim().isEmpty()) {
				result.put("success", false);
				result.put("error", "SFTP 연결에 비밀번호가 설정되지 않았습니다.");
				return result;
			}
			
			logger.info("Shell 실행 시작 - 호스트: {} ({}), 사용자: {}, 스크립트 길이: {}", 
					   hostName, hostIp, username, script.length());
			
			// 실제 Shell 실행
			Map<String, Object> executionResult = shellExecutionService.executeShellScript(
				hostIp, port, username, password, script, parametersJson, 300); // 5분 타임아웃
			
			// 결과 처리
			if ((Boolean) executionResult.get("success")) {
				result.put("success", true);
				String output = (String) executionResult.get("output");
				String error = (String) executionResult.get("error");
				
				// 출력과 에러를 결합하여 표시
				StringBuilder fullOutput = new StringBuilder();
				fullOutput.append("=== Shell 실행 결과 ===\n");
				fullOutput.append("호스트: ").append(hostName).append(" (").append(hostIp).append(")\n");
				fullOutput.append("사용자: ").append(username).append("\n");
				fullOutput.append("실행 시간: ").append(new java.util.Date()).append("\n\n");
				
				if (output != null && !output.trim().isEmpty()) {
					fullOutput.append("=== 표준 출력 ===\n");
					fullOutput.append(output);
				}
				
				if (error != null && !error.trim().isEmpty()) {
					fullOutput.append("\n=== 에러 출력 ===\n");
					fullOutput.append(error);
				}
				
				result.put("output", fullOutput.toString());
			} else {
				result.put("success", false);
				result.put("error", "Shell 실행 실패: " + executionResult.get("error"));
			}
			
		} catch (Exception e) {
			logger.error("Shell 실행 중 오류", e);
			result.put("success", false);
			result.put("error", "Shell 실행 중 오류가 발생했습니다: " + e.getMessage());
		}
		
		return result;
	}
	
	@RequestMapping(path = "/Template/execute")
	public void executeTemplate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String templateId = request.getParameter("templateId");
		
		if (templateId == null || templateId.trim().isEmpty()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "템플릿 ID가 필요합니다.");
			return;
		}
		
		try {
			// DB에서 템플릿 조회
			Map<String, Object> template = sqlTemplateService.getSqlTemplateDetail(templateId);
			
			if (template == null || !(Boolean) template.get("success")) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "템플릿을 찾을 수 없습니다.");
				return;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> templateData = (Map<String, Object>) template.get("data");
			String templateType = (String) templateData.get("templateType");
			if (templateType == null) templateType = "SQL";
			
			// 디버깅 로그 추가
			logger.info("템플릿 실행 요청 - templateId: {}, templateType: {}", templateId, templateType);
			
			// 기존 URL을 그대로 사용하고 templateType만 추가/수정
			String originalQueryString = request.getQueryString();
			String redirectUrl = "/SQLExecute?" + originalQueryString + "&templateType=" + templateType;
			// 타입별 처리
			switch (templateType.toUpperCase()) {
				case "HTML":
					// HTML 직접 출력
					logger.info("HTML 템플릿 처리 시작 - templateId: {}", templateId);
					response.setContentType("text/html; charset=UTF-8");
					PrintWriter out = response.getWriter();
					out.println(templateData.get("sqlContent"));
					out.flush();
					logger.info("HTML 템플릿 처리 완료 - templateId: {}", templateId);
					break;
					
				case "SHELL":
				case "SQL":
				default:
					response.sendRedirect(redirectUrl);
					break; 
			}
			
		} catch (Exception e) {
			logger.error("템플릿 실행 중 오류 발생: " + templateId, e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "템플릿 실행 중 오류가 발생했습니다.");
		}
	}

	@RequestMapping(path = "HTML")
	public void LinkHTML(HttpServletRequest request, HttpServletResponse response, ModelAndView mv) throws IOException {

		File file = new File(request.getParameter("Path"));
		String html = com.FileRead(file);

		java.io.PrintWriter out = response.getWriter();
		out.println(html);
		out.flush();
		out.close();

	}


	@ResponseBody
	@RequestMapping(path = "/SQL/list")
	public List<Map<String, Object>> list(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		String id = (String) session.getAttribute("memberId");

		try {
			// 사용자별 권한 기반 메뉴 트리 조회 (한 번의 쿼리로 권한 있는 것만 조회)
			return sqlTemplateService.getUserMenuTree(id);
			
		} catch (Exception e) {
			logger.error("메뉴 리스트 조회 실패", e);
			return new ArrayList<>();
		}
	}
	



	public enum SqlType {
		CALL, EXECUTE, UPDATE
	}

	// SQL에서 주석 제거
	public static String removeComments(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			throw new IllegalArgumentException("SQL query cannot be null or empty");
		}

		// 정규식을 사용해 단일 줄 및 다중 줄 주석 제거
		String singleLineCommentRegex = "--.*";
		String multiLineCommentRegex = "\\/\\*[\\s\\S]*?\\*\\/";

		sql = sql.replaceAll(singleLineCommentRegex, " ");
		sql = sql.replaceAll(multiLineCommentRegex, " ");
		return sql.trim();
	}

	// SQL 유형 판별
	public static String firstword(String sql) {
		String cleanedSql = removeComments(sql);

		// 첫 번째 단어 추출
		String firstWord = cleanedSql.split("\\s+")[0].toUpperCase();

		return firstWord;
	}

	// SQL 유형 판별
	public static SqlType detectSqlType(String sql) {

		switch (firstword(sql)) {
		case "CALL":
		case "BEGIN":
			return SqlType.CALL;
		case "SELECT":
		case "WITH":
		case "VALUE":
			return SqlType.EXECUTE;
		default:
			return SqlType.UPDATE;
		}
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
