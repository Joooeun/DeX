package kr.Windmill.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import crypt.AES256Cipher;
import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.service.LogInfoDTO;

public class Common {
	private static final Logger logger = LoggerFactory.getLogger(Common.class);

	public static String system_properties = "";
	public static String ConnectionPath = "";
	public static String SrcPath = "";
	public static String UserPath = "";
	public static String tempPath = "";
	public static String RootPath = "";
	public static String LogDB = "";
	public static String DownloadIP = "";
	public static String LogCOL = "";
	public static int Timeout = 15;

	public Common() {
		system_properties = getClass().getResource("").getPath().replaceAll("(WEB-INF).*", "$1") + File.separator + "system.properties";
		Setproperties();
	}

	public static void Setproperties() {

//		logger.debug("[DEBUG : system_properties]" + system_properties);

		Properties props = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(system_properties);
			props.load(new java.io.BufferedInputStream(fis));
		} catch (IOException e) {
			logger.error("system_properties : " + system_properties);
		}

		RootPath = props.getProperty("Root") + File.separator;
		ConnectionPath = props.getProperty("Root") + File.separator + "Connection" + File.separator;
		SrcPath = props.getProperty("Root") + File.separator + "src" + File.separator;
		tempPath = props.getProperty("Root") + File.separator + "temp" + File.separator;
		UserPath = props.getProperty("Root") + File.separator + "user" + File.separator;
		Timeout = Integer.parseInt(props.getProperty("Timeout") == null ? "15" : props.getProperty("Timeout"));
		LogDB = props.getProperty("LogDB");
		DownloadIP = props.getProperty("DownloadIP");
		LogCOL = props.getProperty("LogCOL");
		logger.info("RootPath : " + RootPath + " / Timeout : " + Timeout + " / LogDB : " + LogDB);

	}

	public Map<String, String> ConnectionConf(String ConnectionName) throws IOException {
		Map<String, String> map = new HashMap<>();

		map.put("ConnectionName", ConnectionName);

		String propFile = ConnectionPath + ConnectionName;
		Properties props = new Properties();

		String propStr = FileRead(new File(propFile + ".properties"));

		if (!propStr.startsWith("#")) {
			propStr = FileReadDec(new File(propFile + ".properties"));
		}

		props.load(new ByteArrayInputStream(propStr.getBytes()));

		map.put("TYPE", bytetostr(props.getProperty("TYPE")));
		map.put("IP", bytetostr(props.getProperty("IP")));
		map.put("PORT", bytetostr(props.getProperty("PORT")));
		map.put("USER", bytetostr(props.getProperty("USER")));
		map.put("PW", bytetostr(props.getProperty("PW")));
		map.put("DB", bytetostr(props.getProperty("DB")));
		map.put("DBTYPE", bytetostr(props.getProperty("DBTYPE")));
		return map;
	}

	public Map<String, String> UserConf(String UserName) throws IOException {
		Map<String, String> map = new HashMap<>();

		map.put("UserName", UserName);

		String propFile = UserPath + UserName;
		Properties props = new Properties();
		String propStr = FileRead(new File(propFile));

		if (!propStr.startsWith("#")) {
			propStr = FileReadDec(new File(propFile));
		}

		props.load(new ByteArrayInputStream(propStr.getBytes()));

		map.put("ID", UserName);
		map.put("NAME", bytetostr(props.getProperty("NAME")));
		map.put("IP", bytetostr(props.getProperty("IP")));
		map.put("PW", bytetostr(props.getProperty("PW")));
		map.put("TEMPPW", bytetostr(props.getProperty("TEMPPW")));
		map.put("MENU", bytetostr(props.getProperty("MENU")));
		map.put("CONNECTION", bytetostr(props.getProperty("CONNECTION")));

		return map;
	}

	public Map<String, String> SqlConf(String sqlPath) {
		Map<String, String> map = new HashMap<>();

		map.put("sql", sqlPath);

		try {
			String propFile = SrcPath + sqlPath + ".properties";
			Properties props = new Properties();

			String propStr = FileRead(new File(propFile));

			props.load(new ByteArrayInputStream(propStr.getBytes()));

			map.put("SHORTKEY", props.getProperty("SHORTKEY"));
			map.put("LIMIT", props.getProperty("LIMIT"));
			map.put("REFRESHTIMEOUT", props.getProperty("REFRESHTIMEOUT"));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public void sqlConfSave(String sqlPath, Map<String, String> conf) {

		String propFile = SrcPath + sqlPath + ".properties";
		File file = new File(propFile);

		try {
			String str = "#" + sqlPath + "\n";
			FileWriter fw = new FileWriter(file);

			for (Map.Entry<String, String> entry : conf.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				str += key + "=" + val + "\n";
			}

			fw.write(str);
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<String> ConnectionnList(String type) {

		List<String> dblist = new ArrayList<>();

		try {
			// System.out.println("[debug]" + ConnectionPath);
			File dirFile = new File(ConnectionPath);
			File[] fileList = dirFile.listFiles();
			Arrays.sort(fileList);
			for (File tempFile : fileList) {
				if (tempFile.isFile() && tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".properties")) {

					String propStr = FileRead(tempFile);
					if (!propStr.startsWith("#")) {
						propStr = FileReadDec(tempFile);
					}

					Properties props = new Properties();

					props.load(new ByteArrayInputStream(propStr.getBytes()));

					if (props.getProperty("TYPE").equals(type) || type.equals("")) {
						String tempFileName = tempFile.getName();
						dblist.add(tempFileName);
					}

				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dblist;
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
					} else {
						System.out.println("파일 확인 필요 : " + tempFile.getPath());
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

	public List<Map<String, String>> UserList() throws IOException {

		List<Map<String, String>> userlist = new ArrayList<>();

		File dirFile = new File(UserPath);
		File[] fileList = dirFile.listFiles();
		Arrays.sort(fileList);
		for (File tempFile : fileList) {

			Map user = new HashMap<String, String>();
			if (tempFile.isFile()) {

				String tempFileName = tempFile.getName();

				if (!tempFileName.contains(".")) {
					user.put("id", tempFileName);
					user.put("name", UserConf(tempFileName).get("NAME"));
					userlist.add(user);
				}

			}
		}

		return userlist;
	}

	public String FileRead(File file) throws IOException {
		String str = "";

		BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		String line = "";

		while ((line = bufReader.readLine()) != null) {
			str += line + "\r\n";
		}
		bufReader.close();

		return str;
	}

	public String FileReadDec(File file) throws IOException {
		String str = "";

		BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		String line = "";

		while ((line = bufReader.readLine()) != null) {
			str += line + "\r\n";
		}
		bufReader.close();

		AES256Cipher a256 = AES256Cipher.getInstance();

		try {
			str = a256.AES_Decode(str);

		} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {

			System.out.println("file : " + file.getPath());

			e.printStackTrace();
		}
		return str;
	}

	public String cryptStr(String str) throws IOException {

		String crtStr = "";

		AES256Cipher a256 = AES256Cipher.getInstance();

		try {
			crtStr = a256.AES_Encode(str);

		} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return crtStr;
	}

	// 사용자에게 메시지를 전달하고, 페이지를 리다이렉트 한다.
	public Map<String, String> showMessageAndRedirect(String str1, String str2, String str3) {
		Map<String, String> map = new HashMap<>();
		map.put("message", str1);
		map.put("redirectUri", str2);
		map.put("method", str3);
		return map;
	}

	public String getIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		logger.debug(">>>> X-FORWARDED-FOR : " + ip);
		if (ip == null) {
			ip = request.getHeader("Proxy-Client-IP");
			logger.debug(">>>> Proxy-Client-IP : " + ip);
		}
		if (ip == null) {
			ip = request.getHeader("WL-Proxy-Client-IP"); // 웹로직
			logger.debug(">>>> WL-Proxy-Client-IP : " + ip);
		}
		if (ip == null) {
			ip = request.getHeader("HTTP_CLIENT_IP");
			logger.debug(">>>> HTTP_CLIENT_IP : " + ip);
		}
		if (ip == null) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
			logger.debug(">>>> HTTP_X_FORWARDED_FOR : " + ip);
		}
		if (ip == null) {
			ip = request.getRemoteAddr();
		}
		logger.debug(">>>> Result : IP Address : " + ip);
		return ip;
	}

	public int mapParams(PreparedStatement ps, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {

			if (arg instanceof Date) {
				ps.setTimestamp(i++, new Timestamp(((Date) arg).getTime()));
			} else if (arg instanceof Instant) {
				ps.setTimestamp(i++, new Timestamp((Date.from((Instant) arg)).getTime()));
			} else if (arg instanceof Integer) {
				ps.setInt(i++, (Integer) arg);
			} else if (arg instanceof Duration) {
				ps.setInt(i++, (Integer) arg);
			} else if (arg instanceof Long) {
				ps.setLong(i++, (Long) arg);
			} else if (arg instanceof Double) {
				ps.setDouble(i++, (Double) arg);
			} else if (arg instanceof Float) {
				ps.setFloat(i++, (Float) arg);
			} else {
				ps.setString(i++, (String) arg);

			}
		}
		return i;
	}

	public List<List<String>> updatequery(String sql, String dbtype, String jdbc, Properties prop, LogInfoDTO data, List<Map<String, String>> mapping) throws SQLException {

		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DriverManager.getConnection(jdbc, prop);

			con.setAutoCommit(false);

			List<List<String>> list = new ArrayList<List<String>>();

			int rowcnt = 0;

			pstmt = con.prepareStatement(sql);
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":

					pstmt.setString(i + 1, mapping.get(i).get("value"));
					break;

				default:
					pstmt.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			if (data != null) {

				List<Object> logparams = Arrays.asList(data.getId(), data.getIp(), data.getConnection(), data.getTitle(), data.getSqlType(), data.getRows(), data.getLogsql(), data.getResult(), data.getDuration(), data.getStart(), data.getXmlLog(), data.getLogId());

				mapParams(pstmt, logparams);
			}

			rowcnt = pstmt.executeUpdate();

			List<String> row;

			row = new ArrayList<>();
			row.add("success");
			row.add(Integer.toString(rowcnt));
			row.add(sql);

			list.add(row);

			return list;

		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

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

	}

	public ConnectionDTO getConnection(String connectionId) throws IOException {
		Map<String, String> map = ConnectionConf(connectionId);

		ConnectionDTO connection = new ConnectionDTO();

		Properties prop = new Properties();

		String dbtype = map.get("DBTYPE") == null ? "DB2" : map.get("DBTYPE");
		String driver = "";
		String jdbc = "";

		switch (dbtype) {
		case "DB2":
			driver = "com.ibm.db2.jcc.DB2Driver";
			jdbc = "jdbc:db2://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		case "ORACLE":
			driver = "oracle.jdbc.driver.OracleDriver";
			jdbc = "jdbc:oracle:thin:@" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		case "PostgreSQL":
			driver = "org.postgresql.Driver";
			jdbc = "jdbc:postgresql://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		case "Tibero":
			driver = "com.tmax.tibero.jdbc.TbDriver";
			jdbc = "jdbc:tibero:thin:@" + map.get("IP") + ":" + map.get("PORT") + ":" + map.get("DB");
			break;

		default:
			driver = "com.ibm.db2.jcc.DB2Driver";
			jdbc = "jdbc:db2://" + map.get("IP") + ":" + map.get("PORT") + "/" + map.get("DB");
			break;
		}

		prop.put("user", map.get("USER"));
		prop.put("password", map.get("PW"));

		connection.setDbtype(dbtype);
		connection.setProp(prop);
		connection.setDriver(driver);
		connection.setJdbc(jdbc);
		connection.setDbName(connectionId);

		return connection;
	}

	public Map<String, List> excutequery(String sql, String dbtype, String jdbc, Properties prop, int limit, List<Map<String, String>> mapping) throws SQLException {

		Connection con = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DriverManager.getConnection(jdbc, prop);

			con.setAutoCommit(false);

			Map<String, List> result = new HashMap<String, List>();

			pstmt = con.prepareStatement(sql);
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":

					pstmt.setString(i + 1, mapping.get(i).get("value"));
					break;

				default:
					pstmt.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			if (limit > 0) {
				pstmt.setMaxRows(limit);
			}
			rs = pstmt.executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();

			int colcnt = rsmd.getColumnCount();

			List rowhead = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			String column;

			for (int index = 0; index < colcnt; index++) {

				Map head = new HashMap();

				ResultSet resultSet = con.getMetaData().getColumns(null, rsmd.getSchemaName(index + 1), rsmd.getTableName(index + 1), rsmd.getColumnName(index + 1));

				String desc = rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")";

				if (resultSet.next()) {
					String REMARKS = resultSet.getString("REMARKS") == null ? "" : "\n" + resultSet.getString("REMARKS");
					desc += REMARKS;
				}

				head.put("title", rsmd.getColumnLabel(index + 1));
				head.put("type", rsmd.getColumnType(index + 1));
				head.put("desc", desc);

				rowhead.add(head);

				rowlength.add(0);
			}

			result.put("rowhead", rowhead);

			List rowbody = new ArrayList<>();

			while (rs.next()) {

				List body = new ArrayList<>();
				for (int index = 0; index < colcnt; index++) {

					// column = rsmd.getColumnName(index + 1);
					// 타입별 get함수 다르게 변경필
					try {

						switch (rsmd.getColumnType(index + 1)) {
						case Types.SQLXML:
							body.add(rs.getSQLXML(index + 1).toString());
							break;
						case Types.DATE:
							body.add(rs.getDate(index + 1).toString());
							break;
						case Types.BIGINT:
						case Types.DECIMAL:
							body.add(rs.getBigDecimal(index + 1).toString());
							break;

						case Types.CLOB:
						case Types.TIMESTAMP:
							body.add(rs.getString(index + 1));
							break;

						default:
							body.add(rs.getObject(index + 1));
							break;
						}

						if (rowlength.get(index) < (body.get(index) == null ? "" : body.get(index)).toString().length()) {
							rowlength.set(index, body.get(index).toString().length() > 100 ? 100 : body.get(index).toString().length());
						}

					} catch (NullPointerException e) {
						body.add(null);
					} catch (Exception e) {
						body.add(e.toString());
					}

				}
				rowbody.add(body);
			}
			result.put("rowbody", rowbody);
			result.put("rowlength", rowlength);

			return result;

		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

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
	}

	public Map<String, List> callprocedure(String sql, String dbtype, String jdbc, Properties prop, List<Map<String, String>> mapping) throws SQLException {

		Connection con = null;

		CallableStatement callStmt1 = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			con = DriverManager.getConnection(jdbc, prop);

			con.setAutoCommit(false);

			Map<String, List> result = new HashMap<String, List>();

			String callcheckstr = "";
			String prcdname = "";
			
			List<Integer> typelst = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			
			if (sql.indexOf("CALL") > -1) {
				prcdname = sql.substring(sql.indexOf("CALL") + 6, sql.indexOf("("));
				if (prcdname.contains(".")) {
					prcdname = sql.substring(sql.indexOf(".") + 1, sql.indexOf("("));
				}

				int paramcnt = StringUtils.countMatches(sql, ",") + 1;
				switch (dbtype) {
				case "DB2":
					callcheckstr = "SELECT * FROM   syscat.ROUTINEPARMS WHERE  routinename = '" + prcdname.toUpperCase().trim() + "' AND SPECIFICNAME = (SELECT SPECIFICNAME " + " FROM   (SELECT SPECIFICNAME, count(*) AS cnt FROM   syscat.ROUTINEPARMS WHERE  routinename = '" + prcdname.toUpperCase().trim() + "' GROUP  BY SPECIFICNAME) a WHERE  a.cnt = " + paramcnt + ") AND ROWTYPE != 'P' ORDER  BY SPECIFICNAME, ordinal";
					break;
				case "ORACLE":
					callcheckstr = "SELECT DATA_TYPE AS TYPENAME\r\n" + "  FROM sys.user_arguments    \r\n" + " WHERE object_name = '" + prcdname.toUpperCase().trim() + "'";
					break;

				default:
					break;
				}

				
				pstmt = con.prepareStatement(callcheckstr);
				rs = pstmt.executeQuery();

				

				while (rs.next()) {
					switch (rs.getString("TYPENAME")) {
					case "VARCHAR2":
						typelst.add(Types.VARCHAR);
						break;
					case "VARCHAR":
						typelst.add(Types.VARCHAR);
						break;
					case "INTEGER":
						typelst.add(Types.INTEGER);
						break;
					case "TIMESTAMP":
						typelst.add(Types.TIMESTAMP);
						break;
					case "DATE":
						typelst.add(Types.DATE);
						break;
					}
				}
			}

			callStmt1 = con.prepareCall(sql);
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":

					callStmt1.setString(i + 1, mapping.get(i).get("value"));
					break;

				default:
					// callStmt1.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			for (int i = 0; i < typelst.size(); i++) {
				callStmt1.registerOutParameter(i + 1, typelst.get(i));
			}

			callStmt1.execute();

			rs2 = callStmt1.getResultSet();

			if (rs2 != null) {
				ResultSetMetaData rsmd = rs2.getMetaData();
				int colcnt = rsmd.getColumnCount();

				List rowhead = new ArrayList<>();
				String column;

				for (int index = 0; index < colcnt; index++) {

					Map head = new HashMap();

					head.put("title", rsmd.getColumnLabel(index + 1));
					head.put("type", rsmd.getColumnType(index + 1));
					head.put("desc", rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")");

					rowhead.add(head);
					rowlength.add(0);

				}
				result.put("rowhead", rowhead);

				List rowbody = new ArrayList<>();
				while (rs2.next()) {

					List body = new ArrayList<>();
					for (int index = 0; index < colcnt; index++) {

						// column = rsmd.getColumnName(index + 1);
						// 타입별 get함수 다르게 변경필
						try {

							body.add((rsmd.getColumnTypeName(index + 1).equals("CLOB") ? rs2.getString(index + 1) : rs2.getObject(index + 1)));

							if (rowlength.get(index) < (body.get(index) == null ? "" : body.get(index)).toString().length()) {
								rowlength.set(index, body.get(index).toString().length() > 100 ? 100 : body.get(index).toString().length());
							}

						} catch (NullPointerException e) {
							body.add(null);
						} catch (Exception e) {
							body.add(e.toString());
						}
					}

					rowbody.add(body);

				}

				result.put("rowbody", rowbody);
				result.put("rowlength", rowlength);
			} else {

				List<String> element = new ArrayList<String>();
				for (int i = 0; i < typelst.size(); i++) {

					element.add(callStmt1.getString(i + 1) + "");

				}

				result.put("rowbody", element);
			}

			return result;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

			if (rs2 != null) {
				try {
					rs2.close();
				} catch (Exception e) {
				}
			}

			if (callStmt1 != null) {
				try {
					callStmt1.close();
				} catch (Exception e) {
				}
			}

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
	}

	public List<Map<String, Object>> getJsonObjectFromString(String jsonStr) {

		JSONArray jsonArray = new JSONArray();

		JSONParser jsonParser = new JSONParser();

		try {

			jsonArray = (JSONArray) jsonParser.parse(jsonStr);

		} catch (ParseException e) {
			e.printStackTrace();
		}

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		if (jsonArray != null) {

			int jsonSize = jsonArray.size();

			for (int i = 0; i < jsonSize; i++) {

				Map<String, Object> map = getMapFromJsonObject((JSONObject) jsonArray.get(i));
				list.add(map);
			}
		}

		return list;
	}

	public static Map<String, Object> getMapFromJsonObject(JSONObject jsonObject) {

		Map<String, Object> map = null;

		try {

			map = new ObjectMapper().readValue(jsonObject.toJSONString(), Map.class);

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return map;
	}

	public String bytetostr(String str) throws UnsupportedEncodingException {

		String resultstr = "";
		if (str != null) {
			resultstr = new String(str.getBytes("ISO-8859-1"), "utf-8");
		}

		return resultstr;
	}

	public String getSystem_properties() {
		return system_properties;
	}

	public void setSystem_properties(String system_properties) {
		this.system_properties = system_properties;
	}

	public String getConnectionPath() {
		return ConnectionPath;
	}

	public void setConnectionPath(String connectionPath) {
		ConnectionPath = connectionPath;
	}

	public void setUserPath(String userPath) {
		UserPath = userPath;
	}

	public String getUserPath() {
		return UserPath;
	}

	public String getSrcPath() {
		return SrcPath;
	}

	public static void setSrcPath(String srcPath) {
		SrcPath = srcPath;
	}

	public static String getRootPath() {
		return RootPath;
	}

	public static void setRootPath(String rootPath) {
		RootPath = rootPath;
	}

}
