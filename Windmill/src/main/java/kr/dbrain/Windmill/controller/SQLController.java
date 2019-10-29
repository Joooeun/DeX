package kr.dbrain.Windmill.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
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

		FileReader filereader = new FileReader(file);
		BufferedReader bufReader = new BufferedReader(filereader);
		String line = "";
		String sql = "";
		while ((line = bufReader.readLine()) != null) {
			sql += line + "\r\n";
		}
		bufReader.close();

		file = new File(request.getParameter("Path").replace(".sql", ".properties"));
		List<Map<String, String>> ShortKey = new ArrayList<>();
		List<Map<String, String>> Param = new ArrayList<>();
		if (file.exists()) {
			filereader = new FileReader(file);
			bufReader = new BufferedReader(filereader);
			line = "";

			int num = 0;

			String values[] = null;

			if (request.getParameter("sendvalue") != null) {
				values = request.getParameter("sendvalue").split("\\s*\\&");
			}

			while ((line = bufReader.readLine()) != null) {
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
			bufReader.close();
		}

		mv.addObject("sql", sql);
		mv.addObject("Param", Param);
		mv.addObject("ShortKey", ShortKey);
		mv.addObject("Connection", session.getAttribute("Connection"));

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/list")
	public List<Map<String, ?>> list(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		List<Map<String, ?>> lsit = getfiles(Common.srcPath);

		return lsit;
	}

	@ResponseBody
	@RequestMapping(path = "/SQL/excute")
	public List<List<String>> excute(HttpServletRequest request, Model model, HttpSession session) throws ClassNotFoundException {

		List<List<String>> list = new ArrayList<List<String>>();
		Map<String, String> map = com.ConnectionConf(request.getParameter("Connection"));

		Properties prop = new Properties();
		Class.forName("com.ibm.db2.jcc.DB2Driver");

		prop.put("user", map.get("USER"));
		prop.put("password", map.get("PW"));
		prop.put("clientProgramName", "DeX");
		
		Connection con = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		String sql = request.getParameter("sql");

		try {
			con = DriverManager.getConnection("jdbc:db2://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB"), prop);
			
			con.setAutoCommit(false);

			pstmt = con.prepareStatement(sql);

			// pstmt.setString(1, "");

			rs = pstmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int colcnt = rsmd.getColumnCount();

			List<String> row;
			String column;

			row = new ArrayList<>();
			for (int index = 0; index < colcnt; index++) {

				column = rsmd.getColumnName(index + 1);

				row.add(column);

			}
			list.add(row);

			while (rs.next()) {

				row = new ArrayList<>();
				for (int index = 0; index < colcnt; index++) {
					column = rsmd.getColumnName(index + 1);
					row.add(rs.getString(column));

				}
				list.add(row);

			}

		} catch (SQLException e1) {
			List<String> element = new ArrayList<String>();
			element.add(e1.toString());

			list.add(element);
			e1.printStackTrace();
		} finally {
			if (rs != null)
				try {
					rs.close();
				} catch (SQLException ex) {
				}
			if (pstmt != null)
				try {
					pstmt.close();
				} catch (SQLException ex) {
				}
			if (con != null)
				try {
					con.commit();
					con.close();

				} catch (SQLException ex) {
					logger.error("[ERROR]" + ex.toString());
				}
		}

		return list;
	}

	public List<Map<String, ?>> getfiles(String root) {

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
				element.put("list", getfiles(tempFile.getPath()));
				element.put("Path", "Path");

				list.add(element);
			}
		}

		return list;

	}

}
