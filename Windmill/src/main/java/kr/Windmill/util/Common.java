package kr.Windmill.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static int Timeout = 15;

	public Common() {
		system_properties = getClass().getResource("").getPath().replaceAll("(WEB-INF).*", "$1") + File.separator
				+ "system.properties";
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
		logger.info("RootPath : " + RootPath + " / Timeout : " + Timeout + " / LogDB : " + LogDB);

	}

	public Map<String, String> ConnectionConf(String ConnectionName) throws IOException {
		Map<String, String> map = new HashMap<>();

		map.put("ConnectionName", ConnectionName);

		String propFile = ConnectionPath + ConnectionName;
		Properties props = new Properties();

		String propStr = FileRead(new File(propFile + ".properties"));

		props.load(new ByteArrayInputStream(propStr.getBytes()));

		map.put("TYPE", props.getProperty("TYPE"));
		map.put("IP", props.getProperty("IP"));
		map.put("PORT", props.getProperty("PORT"));
		map.put("USER", props.getProperty("USER"));
		map.put("PW", props.getProperty("PW"));
		map.put("DB", props.getProperty("DB"));
		map.put("DBTYPE", props.getProperty("DBTYPE"));
		return map;
	}

	public Map<String, String> UserConf(String UserName) {
		Map<String, String> map = new HashMap<>();

		map.put("UserName", UserName);

		try {
			String propFile = UserPath + UserName;
			Properties props = new Properties();

			String propStr = FileRead(new File(propFile));

			props.load(new ByteArrayInputStream(propStr.getBytes()));

			map.put("IP", props.getProperty("IP"));
			map.put("PW", props.getProperty("PW"));
			map.put("MENU", props.getProperty("MENU"));
			map.put("CONNECTION", props.getProperty("CONNECTION"));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			// TODO Auto-generated catch block
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
				if (tempFile.isFile()
						&& tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".properties")) {

					String propStr = FileRead(tempFile);

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

	public List<String> UserList() {

		List<String> userlist = new ArrayList<>();

		File dirFile = new File(UserPath);
		File[] fileList = dirFile.listFiles();
		Arrays.sort(fileList);
		for (File tempFile : fileList) {
			if (tempFile.isFile()) {

				String tempFileName = tempFile.getName();

				if (!tempFileName.contains("."))
					userlist.add(tempFileName);

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

		AES256Cipher a256 = AES256Cipher.getInstance();

//		try {
//			str = a256.AES_Decode(str);
//		} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
//				| BadPaddingException e) {
//			// TODO Auto-generated catch block
//			logger.warn("임호화 에러 "+str);
//		}
		return str;
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

	public void log_file(LogInfoDTO data, String msg) {

		if (data.getConnection().equals(LogDB) || !(data.isAudit() || data.getId().equals("admin"))) {
			return;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = RootPath + "log";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					logger.info("폴더생성여부 : " + folder.mkdirs());
				} catch (Exception e) {
					e.getStackTrace();
				}
			}

			path += File.separator + data.getId() + "_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);
			SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate2 = simpleDateFormat2.format(Date.from(data.getStart()));

			writer.write(strNowDate2 + " id : " + data.getId() + " / ip :  " + data.getIp());
			writer.write("\nDB : " + data.getConnection() + " / MENU : " + data.getPath());
			writer.write(msg);
			writer.newLine();
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void userLog(String user, String ip, String msg) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = RootPath + "log";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					logger.info("폴더생성여부 : " + folder.mkdirs());
				} catch (Exception e) {
					e.getStackTrace();
				}
			}

			path += File.separator + "user_access_log_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);
			SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate2 = simpleDateFormat2.format(nowDate);

			writer.write(strNowDate2 + " id : " + user + " / ip :  " + ip + "\n" + msg);
			writer.newLine();
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log_DB(LogInfoDTO data) {

		if (data.getConnection().equals(LogDB) || !(data.isAudit() || data.getId().equals("admin"))) {
			return;
		}

		try {
			ConnectionDTO connection = getConnection(LogDB);

			updatequery(
					"INSERT INTO DEXLOG (USER, IP, CONNECTION, MENU, TYPE, ROW, SQL, RESULT,DURATION, EXECUTEDATE)"
							+ "VALUES(?,?,?,?,?,?,?,?,?,?);",
					connection.getDbtype(), connection.getJdbc(), connection.getProp(), data);
		} catch (SQLException e) {
			logger.error(e.toString());
		} catch (IOException e) {
			logger.error(e.toString());
		}

	}

	public int mapParams(PreparedStatement ps, Object... args) throws SQLException {
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

	public List<List<String>> updatequery(String sql, String dbtype, String jdbc, Properties prop, LogInfoDTO data)
			throws SQLException {

		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DriverManager.getConnection(jdbc, prop);

			con.setAutoCommit(false);

			List<List<String>> list = new ArrayList<List<String>>();

			int rowcnt = 0;

			pstmt = con.prepareStatement(sql);

			if (data != null) {
				mapParams(pstmt, data.getId(), data.getIp(), data.getConnection(), data.getPath(), data.getSqlType(),
						data.getRows(), data.getSql(), data.getResult(), data.getDuration(), data.getStart());
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
		case "Postgresql":
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
