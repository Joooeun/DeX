package kr.dbrain.Windmill.controller;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.dbrain.Windmill.util.Common;

@Controller
public class SQLController {

	private static final Logger logger = LoggerFactory.getLogger(SQLController.class);
	Common com = new Common();

	@RequestMapping(path = "/SQL")
	public ModelAndView SQLmain(HttpServletRequest request, ModelAndView mv, HttpSession session) throws IOException {

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
					ShortKey.add(map);
				} else if (line.split("=")[0].equals("REFRESHTIMEOUT")) {
					mv.addObject("refreshtimeout", line.split("=")[1]);
				}

			}

		}

		mv.addObject("sql", sql);
		mv.addObject("Param", Param);
		mv.addObject("ShortKey", ShortKey);
		mv.addObject("Connection", session.getAttribute("Connection"));

		return mv;
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
		List<Map<String, ?>> list = getfiles(Common.srcPath, 0);

		if (!id.equals("admin")) {
			List<String> strList = new ArrayList<>(Arrays.asList(map.get("MENU").split(",")));

			return list.stream().filter(menu -> strList.contains(menu.get("Name"))).collect(Collectors.toList());

		}

		return list;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/excute")
	public List<List<String>> excute(HttpServletRequest request, Model model, HttpSession session)
			throws ClassNotFoundException {

		Instant start = Instant.now();

		List<List<String>> list = new ArrayList<List<String>>();
		Map<String, String> map = com.ConnectionConf(request.getParameter("Connection"));

		Properties prop = new Properties();

		String dbtype = map.get("DBTYPE") == null ? "DB2" : map.get("DBTYPE");
		String driver = "com.ibm.db2.jcc.DB2Driver";
		String jdbc = "jdbc:db2://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");

		switch (dbtype) {
		case "DB2":
			driver = "com.ibm.db2.jcc.DB2Driver";
			jdbc = "jdbc:db2://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		case "ORACLE":
			driver = "oracle.jdbc.driver.OracleDriver";
			jdbc = "jdbc:oracle:thin:@" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		case "Postgresql":
			driver = "org.postgresql.Driver";
			jdbc = "jdbc:postgresql://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;

		default:
			break;
		}

		Class.forName(driver);

		prop.put("user", map.get("USER"));
		prop.put("password", map.get("PW"));
		prop.put("clientProgramName", "DeX");

		Connection con = null;

		String sql = request.getParameter("sql");

		try {

			con = DriverManager.getConnection(jdbc, prop);
			con.setAutoCommit(false);
			

			if (sql.startsWith("CALL")) {
				list = callprocedure(sql, dbtype, con);
			} else if (sql.toUpperCase().startsWith("INSERT") || sql.toUpperCase().startsWith("UPDATE")
					|| sql.toUpperCase().startsWith("DELETE")) {
				list = updatequery(sql, dbtype, con);

			} else {
				list = excutequery(sql, dbtype, con);
			}

			Instant end = Instant.now();
			Duration timeElapsed = Duration.between(start, end);

			com.userLog(session.getAttribute("memberId").toString(), com.getIp(request),
					" sql 실행 성공 / rows : " + (list.size() - 1) + " / 소요시간 : "
							+ new DecimalFormat("###,###").format(timeElapsed.toMillis())
							+ "\nstart============================================\n" + sql
							+ "\nend==============================================");

		} catch (SQLException e1) {
			List<String> element = new ArrayList<String>();
			element.add(e1.toString());

			com.userLog(session.getAttribute("memberId").toString(), com.getIp(request),
					" sql 실행 실패\nstart============================================\n" + sql
							+ "\nend==============================================");

			list.add(element);
			e1.printStackTrace();
		} finally {

			if (con != null) {

				try {
					con.commit();
					con.close();

				} catch (SQLException ex) {
					logger.error(ex.toString());
				}
			} else {

			}
		}

		return list;
	}

	public List<List<String>> excutequery(String sql, String dbtype, Connection con) throws SQLException {

		List<List<String>> list = new ArrayList<List<String>>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		pstmt = con.prepareStatement(sql);
		pstmt.setMaxRows(200);
		rs = pstmt.executeQuery();

		ResultSetMetaData rsmd = rs.getMetaData();
		int colcnt = rsmd.getColumnCount();

		List<String> row;
		String column;

		row = new ArrayList<>();
		for (int index = 0; index < colcnt; index++) {

			column = rsmd.getColumnLabel(index + 1);

			row.add(column);

		}
		list.add(row);

		while (rs.next()) {

			row = new ArrayList<>();
			for (int index = 0; index < colcnt; index++) {
				// column = rsmd.getColumnName(index + 1);
				// 타입별 get함수 다르게 변경필
				try {
					row.add(rs.getObject(index + 1) == null ? "NULL"
							: (rsmd.getColumnTypeName(index + 1).equals("CLOB") ? rs.getString(index + 1)
									: rs.getObject(index + 1).toString()));
					// System.out.println(rs.getObject(index+1));
				} catch (Exception e) {
					row.add(e.toString());
				}

			}
			list.add(row);

		}

		return list;

	}

	public List<List<String>> updatequery(String sql, String dbtype, Connection con) throws SQLException {

		List<List<String>> list = new ArrayList<List<String>>();

		PreparedStatement pstmt = null;
		int rowcnt = 0;

		pstmt = con.prepareStatement(sql);
		rowcnt = pstmt.executeUpdate();

		List<String> row;
		String column;

		row = new ArrayList<>();
		row.add("Updated Rows");
		row.add(rowcnt + "");

		list.add(row);
		row = new ArrayList<>();
		row.add("Query");
		row.add(sql);

		list.add(row);

		return list;

	}

	public List<List<String>> callprocedure(String sql, String dbtype, Connection con) throws SQLException {

		List<List<String>> list = new ArrayList<List<String>>();

		CallableStatement callStmt1 = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String callcheckstr = "";

		String prcdname = "";
		prcdname = sql.substring(sql.indexOf("CALL") + 4, sql.indexOf("("));
		if (prcdname.contains(".")) {
			prcdname = sql.substring(sql.indexOf(".") + 1, sql.indexOf("("));
		}

		int paramcnt = StringUtils.countMatches(sql, ",") + 1;
		switch (dbtype) {
		case "DB2":
			callcheckstr = "SELECT * FROM   syscat.ROUTINEPARMS WHERE  routinename = '" + prcdname.toUpperCase().trim()
					+ "' AND SPECIFICNAME = (SELECT SPECIFICNAME "
					+ " FROM   (SELECT SPECIFICNAME, count(*) AS cnt FROM   syscat.ROUTINEPARMS WHERE  routinename = '"
					+ prcdname.toUpperCase().trim() + "' GROUP  BY SPECIFICNAME) a WHERE  a.cnt = " + paramcnt
					+ ") AND ROWTYPE != 'P' ORDER  BY SPECIFICNAME, ordinal";
			break;
		case "ORACLE":
			callcheckstr = "SELECT DATA_TYPE AS TYPENAME\r\n" + "  FROM sys.user_arguments    \r\n"
					+ " WHERE object_name = '" + prcdname.toUpperCase().trim() + "'";
			break;

		default:
			break;
		}

		List<Integer> typelst = new ArrayList<>();
		pstmt = con.prepareStatement(callcheckstr);
		rs = pstmt.executeQuery();
		System.out.println(pstmt);

		while (rs.next()) {
			switch (rs.getString("TYPENAME")) {
			case "VARCHAR2":
				typelst.add(java.sql.Types.VARCHAR);
				break;
			case "VARCHAR":
				typelst.add(java.sql.Types.VARCHAR);
				break;
			case "INTEGER":
				typelst.add(java.sql.Types.INTEGER);
				break;
			case "TIMESTAMP":
				typelst.add(java.sql.Types.TIMESTAMP);
				break;
			case "DATE":
				typelst.add(java.sql.Types.DATE);
				break;
			}
		}

		callStmt1 = con.prepareCall(sql);
		for (int i = 0; i < typelst.size(); i++) {
			callStmt1.registerOutParameter(i + 1, typelst.get(i));
		}

		callStmt1.execute();
		for (int i = 0; i < typelst.size(); i++) {
			List<String> element = new ArrayList<String>();
			element.add(callStmt1.getString(i + 1) + "");
			list.add(element);
		}
		return list;
	}

	public static List<Map<String, ?>> getfiles(String root, int depth) {

		List<Map<String, ?>> list = new ArrayList<>();

		File dirFile = new File(root);
		File[] fileList = dirFile.listFiles();
		Arrays.sort(fileList);
		for (File tempFile : fileList) {
			if (tempFile.isFile()) {

				if (tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".sql")) {
					Map<String, Object> element = new HashMap<>();
					element.put("Name", tempFile.getName());
					element.put("Path", tempFile.getPath());

					list.add(element);
				}

			} else if (tempFile.isDirectory()) {
				Map<String, Object> element = new HashMap<>();

				element.put("Name", tempFile.getName());
				element.put("Path", "Path" + depth);
				element.put("list", getfiles(tempFile.getPath(), depth + 1));

				list.add(element);
			}
		}

		return list;

	}

}
