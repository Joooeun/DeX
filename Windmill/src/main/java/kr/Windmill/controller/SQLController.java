package kr.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.dto.log.LogInfoDto;
import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
public class SQLController {

	private static final Logger logger = LoggerFactory.getLogger(SQLController.class);
	private final Common com;
	private final Log cLog;
	private final SQLExecuteService sqlExecuteService;
	private final SqlTemplateService sqlTemplateService;
	private final SystemConfigService systemConfigService;
	
	@Autowired
	public SQLController(Common common, Log log, SQLExecuteService sqlExecuteService, SqlTemplateService sqlTemplateService, SystemConfigService systemConfigService) {
		this.com = common;
		this.cLog = log;
		this.sqlExecuteService = sqlExecuteService;
		this.sqlTemplateService = sqlTemplateService;
		this.systemConfigService = systemConfigService;
	}

	@RequestMapping(path = "/SQL")
	public ModelAndView SQLmain(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		// templateId 파라미터가 있으면 SQLTemplateController로 리다이렉트
		String templateId = request.getParameter("templateId");
		if (templateId != null && !templateId.trim().isEmpty()) {
			mv.addObject("sendvalue", request.getParameter("sendvalue"));
			String excuteParam = request.getParameter("Excute");
			String redirectUrl = "/SQLTemplate?templateId=" + templateId;
			if (excuteParam != null) {
				redirectUrl += "&Excute=" + excuteParam;
			}
			mv.setViewName("redirect:" + redirectUrl);
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
					} else if (line.split("=")[0].equals("AUDIT")) {
						mv.addObject("audit", line.split("=")[1].toLowerCase());
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

	@RequestMapping(path = "HTML")
	public void LinkHTML(HttpServletRequest request, HttpServletResponse response, ModelAndView mv) throws IOException {

		File file = new File(request.getParameter("Path"));
		String html = com.FileRead(file);

		java.io.PrintWriter out = response.getWriter();
		out.println(html);
		out.flush();
		out.close();

	}

	@RequestMapping(path = "/search_all_data", method = RequestMethod.GET)
	public ModelAndView test(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		mv.addObject("connectionId", session.getAttribute("connectionId"));

		return mv;
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
	


	@ResponseBody
	@RequestMapping(path = "/SQL/excute")
	public Map<String, List> excute(HttpServletRequest request, Model model, HttpSession session, @ModelAttribute LogInfoDto data) throws ClassNotFoundException, IOException, Exception {

		data.setId(session.getAttribute("memberId").toString());
		data.setIp(com.getIp(request));

		try {
			// 공통 SQL 실행 서비스 사용
			return sqlExecuteService.executeSQL(data);
		} catch (Exception e) {
			logger.error("SQL 실행 오류", e);
			throw e;
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
