package kr.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.service.LogInfoDTO;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
public class SQLController {

	private static final Logger logger = LoggerFactory.getLogger(SQLController.class);
	Common com = new Common();
	Log cLog = new Log();

	@RequestMapping(path = "/SQL")
	public ModelAndView SQLmain(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		try {
			File file = new File(request.getParameter("Path"));

			mv.addObject("Path", file.getParent());
			mv.addObject("title", file.getName().replaceAll("\\..*", ""));

			boolean sql = com.FileRead(file).length() > 0;

			file = new File(request.getParameter("Path").replace(".sql", ".properties"));
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
						mv.addObject("newline", line.split("=")[1]);
					} else if (line.split("=")[0].equals("DB")) {
						mv.addObject("DB", line.split("=")[1]);
					} else if (line.split("=")[0].equals("DESC")) {
						mv.addObject("desc", line.split("=")[1]);
					} else if (line.split("=")[0].equals("SAVE")) {
						mv.addObject("save", line.split("=")[1]);
					} else if (line.split("=")[0].equals("AUDIT")) {
						mv.addObject("audit", line.split("=")[1]);
					}

				}

			}

			boolean DownloadEnable = com.getIp(request).matches(com.DownloadIP);

			mv.addObject("sql", sql);
			mv.addObject("Param", Param);
			mv.addObject("ShortKey", ShortKey);
			mv.addObject("Excute", request.getParameter("excute") == null ? false : request.getParameter("excute"));
			mv.addObject("Connection", session.getAttribute("Connection"));
			mv.addObject("DownloadEnable", DownloadEnable);

		} catch (IOException e) {

			e.printStackTrace();

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

		mv.addObject("Connection", session.getAttribute("Connection"));

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/list")
	public List<Map<String, ?>> list(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		String id = (String) session.getAttribute("memberId");

		Map<String, String> map = com.UserConf(id);
		List<Map<String, ?>> list = com.getfiles(Common.SrcPath, 0);

		if (!id.equals("admin")) {
			List<String> strList = new ArrayList<>(Arrays.asList(map.get("MENU").split(",")));

			return list.stream().filter(menu -> strList.contains(menu.get("Name"))).collect(Collectors.toList());

		}

		return list;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/excute")
	public Map<String, List> excute(HttpServletRequest request, Model model, HttpSession session, @ModelAttribute LogInfoDTO data) throws ClassNotFoundException, IOException {

		data.setStart(Instant.now());
		data.setId(session.getAttribute("memberId").toString());
		data.setIp(com.getIp(request));

		ConnectionDTO connection = com.getConnection(data.getConnection());
		Properties prop = connection.getProp();

		Class.forName(connection.getDriver());
		prop.put("clientProgramName", "DeX");

		String sql = data.getSql().length() > 0 ? data.getSql() : com.FileRead(new File(data.getPath()));
		data.setParamList(com.getJsonObjectFromString(data.getParams()));
		data.setLogsqlA(sql);

		String log = "";

		if (data.getLog() != null) {

			for (Entry<String, String> entry : data.getLog().entrySet()) {
				log += "\n" + entry.getKey() + " : " + entry.getValue();
			}
		}

		Map<String, List> result = new HashMap();
		PreparedStatement pstmt = null;

		try {

			cLog.log_start(data, log + "\nmenu 실행 시작\n");

			List<Map<String, String>> mapping = new ArrayList<Map<String, String>>();

			if (data.getParamList().size() > 0) {

				String patternString = "(?<!:):(";
				for (int i = 0; i < data.getParamList().size(); i++) {

					if (data.getParamList().get(i).get("type").equals("string") || data.getParamList().get(i).get("type").equals("text") || data.getParamList().get(i).get("type").equals("varchar")) {
						if (!patternString.equals("(?<!:):("))
							patternString += "|";
						patternString += data.getParamList().get(i).get("title");
					} else {
						sql = sql.replaceAll(":" + data.getParamList().get(i).get("title"), data.getParamList().get(i).get("value").toString());
					}

				}
				patternString += ")";
				if (!patternString.equals("(?<!:):()")) {
					Pattern pattern = Pattern.compile(patternString);
					Matcher matcher = pattern.matcher(sql);
					int cnt = 0;
					while (matcher.find()) {
						Map temp = new HashMap<>();
						temp.put("value", data.getParamList().stream().filter(p -> p.get("title").equals(matcher.group(1))).findFirst().get().get("value"));
						temp.put("type", data.getParamList().stream().filter(p -> p.get("title").equals(matcher.group(1))).findFirst().get().get("type"));

						mapping.add(temp);
						cnt++;
					}
					matcher.reset();
					sql = matcher.replaceAll("?");
				}

			}

			data.setSql(sql);

			String row = "";

			if (detectSqlType(sql) == SqlType.CALL) {
				data.setLogNo(data.getLogNo() + 1);
				// cLog.log_line(data, "start============================================\n" +
				// data.getLogsql() + "\nend==============================================");
				result = com.callprocedure(sql, connection.getDbtype(), connection.getJdbc(), prop, mapping);
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);

			} else if (detectSqlType(sql) == SqlType.EXECUTE) {
				data.setLogNo(data.getLogNo() + 1);
				// cLog.log_line(data, "start============================================\n" +
				// data.getLogsql() + "\nend==============================================");
				result = com.excutequery(sql, connection.getDbtype(), connection.getJdbc(), prop, data.getLimit(), mapping);
				data.setRows(result.get("rowbody").size() - 1);
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);

			} else {

				List<Map> rowhead = new ArrayList<>();

				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Result");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Updated Rows");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Query");
					}
				});

				result.put("rowhead", rowhead);

				String sqlOrg = sql.trim();
				String logsqlOrg = data.getLogsql().trim();

				for (int i = 0; i < sqlOrg.split(";").length; i++) {

					String singleSql = sqlOrg.split(";")[i];

					if (singleSql.trim().length() == 0) {
						continue;
					}
					data.setLogNo(data.getLogNo() + 1);
					sql = singleSql.trim();
					String logsql = logsqlOrg.split(";")[i].trim() + ";";

					// cLog.log_line(data, "start============================================\n" +
					// logsql + "\nend==============================================");
					data.setSql(sql);
					data.setLogsql(logsql);

					Instant singleStart = Instant.now();

					List<List<String>> singleList = new ArrayList<List<String>>();

					if (result.get("rowbody") != null)
						singleList.addAll(result.get("rowbody"));
					singleList.addAll(com.updatequery(sql.trim(), connection.getDbtype(), connection.getJdbc(), prop, null, mapping));

					result.put("rowbody", singleList);

					Duration timeElapsed = Duration.between(singleStart, Instant.now());
					data.setResult("Success");
					data.setDuration(timeElapsed.toMillis());
					row = " / " + data.getSqlType() + " rows : " + singleList.get(i).get(1).toString();
					data.setRows(Integer.parseInt(singleList.get(i).get(1)));

					cLog.log_end(data, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
					cLog.log_DB(data);
				}
			}

		} catch (SQLException e1) {

			if (result.size() == 0) {
				List<Map> rowhead = new ArrayList<>();

				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Result");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Updated Rows");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Query");
					}
				});

				result.put("rowhead", rowhead);
			}

			List<List<String>> singleList = new ArrayList<List<String>>();
			if (result.get("rowbody") != null)
				singleList.addAll(result.get("rowbody"));

			List<String> element = new ArrayList<String>();
			element.add(e1.toString());
			element.add("0");
			element.add(sql);

			singleList.add(element);

			result.put("rowbody", singleList);

			if (log.length() > 0) {
				// cLog.log_line(data, log);
			}

			data.setResult(e1.getMessage());
			data.setDuration(0);
			cLog.log_end(data, " sql 실 행 종료 : 실패 " + e1.getMessage() + "\n\n");
			cLog.log_DB(data);

			System.out.println("id : " + session.getAttribute("memberId") + " / sql : " + sql);
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return result;
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
}
