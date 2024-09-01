package kr.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
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

			String sql = com.FileRead(file);

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
		List<Map<String, ?>> list = getfiles(Common.SrcPath, 0);

		if (!id.equals("admin")) {
			List<String> strList = new ArrayList<>(Arrays.asList(map.get("MENU").split(",")));

			return list.stream().filter(menu -> strList.contains(menu.get("Name"))).collect(Collectors.toList());

		}

		return list;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/excute")
	public Map<String, List> excute(HttpServletRequest request, Model model, HttpSession session, @ModelAttribute LogInfoDTO data) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, SQLException {

		data.setStart(Instant.now());
		data.setId(session.getAttribute("memberId").toString());
		data.setIp(com.getIp(request));

		ConnectionDTO connection = com.getConnection(data.getConnection());
		Properties prop = connection.getProp();

		URL u = new URL("jar:file:" + com.RootPath + "jdbc" + File.separator + connection.getJar() + "!/");
		String classname = connection.getDriver();
		URLClassLoader ucl = new URLClassLoader(new URL[] { u });
		Driver d = (Driver) Class.forName(classname, true, ucl).newInstance();
		DriverManager.registerDriver(new DriverShim(d));
		prop.put("clientProgramName", "DeX");

		String sql = data.getSql();
		String log = "";

		if (data.getLog() != null) {

			for (Entry<String, String> entry : data.getLog().entrySet()) {
				log += "\n" + entry.getKey() + " : " + entry.getValue();
			}
		}

		Map<String, List> result = new HashMap();
		PreparedStatement pstmt = null;

		data.setParamList(com.getJsonObjectFromString(data.getParams()));

		try {

			cLog.log_start(data, log + "\nmenu 실행 시작\n");

			String row = "";

			if (sql.toUpperCase().startsWith("CALL")) {
				cLog.log_line(data, "start============================================\n" + data.getLogsql() + "\nend==============================================");
				result = com.callprocedure(sql, connection.getDbtype(), connection.getJdbc(), prop, data.getParamList());
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);

			} else if (sql.toUpperCase().startsWith("SELECT") || sql.toUpperCase().startsWith("WITH") || sql.toUpperCase().startsWith("VALUE")) {
				cLog.log_line(data, "start============================================\n" + data.getLogsql() + "\nend==============================================");
				result = com.excutequery(sql, connection.getDbtype(), connection.getJdbc(), prop, data.getLimit(), data.getParamList());
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

					sql = singleSql.trim() + ";";
					String logsql = logsqlOrg.split(";")[i].trim() + ";";

					cLog.log_line(data, "start============================================\n" + logsql + "\nend==============================================");
					data.setSql(sql);
					data.setLogsql(logsql);

					Instant singleStart = Instant.now();

					List<List<String>> singleList = new ArrayList<List<String>>();

					if (result.get("rowbody") != null)
						singleList.addAll(result.get("rowbody"));
					singleList.addAll(com.updatequery(sql.trim(), connection.getDbtype(), connection.getJdbc(), prop, null, data.getParamList()));

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
				cLog.log_line(data, log);
			}

			data.setResult(e1.getMessage());
			data.setDuration(0);
			cLog.log_end(data, " sql 실행 종료 : 실패 " + e1.getMessage() + "\n\n");
			cLog.log_DB(data);

			System.out.println("id : " + session.getAttribute("memberId") + " / sql : " + sql);
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return result;
	}

	public static List<Map<String, ?>> getfiles(String root, int depth) {

		List<Map<String, ?>> list = new ArrayList<>();

		File dirFile = new File(root);
		File[] fileList = dirFile.listFiles();
		Arrays.sort(fileList);

		try {
			for (File tempFile : fileList) {
				if (tempFile.isFile()) {
					if (tempFile.getName().contains(".")) {
						if (tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".sql")) {
							Map<String, Object> element = new HashMap<>();
							element.put("Name", tempFile.getName());
							element.put("Path", tempFile.getPath());

							list.add(element);
						} else if (tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".htm")) {
							Map<String, Object> element = new HashMap<>();
							element.put("Name", tempFile.getName());
							element.put("Path", tempFile.getPath());

							list.add(element);
						}
					}

				} else if (tempFile.isDirectory()) {
					Map<String, Object> element = new HashMap<>();

					element.put("Name", tempFile.getName());
					element.put("Path", "Path" + depth);
					element.put("list", getfiles(tempFile.getPath(), depth + 1));

					list.add(element);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;

	}

}
