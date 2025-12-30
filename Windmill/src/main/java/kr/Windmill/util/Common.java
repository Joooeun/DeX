package kr.Windmill.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Common {
	private static final Logger logger = LoggerFactory.getLogger(Common.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static String system_properties = "";
	public static String tempPath = "";
	public static String JdbcPath = ""; // JDBC 드라이버 폴더 경로
	public static String RootPath = "";
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

		// RootPath 먼저 설정
		RootPath = props.getProperty("Root") + File.separator;
		
		// RootPath 유효성 확인
		if (!isRootPathValid()) {
			logger.error("RootPath가 유효하지 않아 애플리케이션 초기화를 중단합니다: {}", RootPath);
			return; // 여기서 메서드 종료
		}
		
		// RootPath가 유효한 경우에만 다른 경로들 설정
		tempPath = props.getProperty("Root") + File.separator + "temp" + File.separator;
		JdbcPath = props.getProperty("Root") + File.separator + "jdbc" + File.separator;
		LogCOL = props.getProperty("LogCOL");
		logger.info("RootPath : " + RootPath);

		// RootPath 유효성 확인 후 JDBC 폴더 생성
		if (isRootPathValid()) {
			createJdbcDirectory();
		} else {
			logger.error("RootPath가 유효하지 않아 JDBC 폴더 생성을 건너뜁니다: {}", RootPath);
		}

	}





	public List<String> ConnectionnList() {
		List<String> dblist = new ArrayList<>();

		try {
			// DATABASE_CONNECTION 테이블에서만 조회 (type 구분 없이)
			String sql = "SELECT CONNECTION_ID FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY CONNECTION_ID";
			dblist = jdbcTemplate.queryForList(sql, String.class);

		} catch (Exception e) {
			logger.error("DB 기반 연결 목록 조회 실패: {}", e.getMessage(), e);
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
						logger.warn("파일 확인 필요: {}", tempFile.getPath());
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


	public List<Map<String, Object>> getListFromString(String jsonStr) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		// null 또는 빈 문자열 체크
		if (jsonStr == null || jsonStr.trim().isEmpty()) {
			logger.debug("JSON 문자열이 null이거나 비어있습니다.");
			return list;
		}

		try {
			// Jackson ObjectMapper를 사용한 안전한 JSON 파싱
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
			
			// JSON 배열로 파싱 시도
			JsonNode jsonNode = objectMapper.readTree(jsonStr);
			
			if (jsonNode.isArray()) {
				for (JsonNode node : jsonNode) {
					if (node.isObject()) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = objectMapper.convertValue(node, Map.class);
						if (map != null) {
							list.add(map);
						}
					}
				}
			} else if (jsonNode.isObject()) {
				// 단일 객체인 경우 배열로 변환
				@SuppressWarnings("unchecked")
				Map<String, Object> map = objectMapper.convertValue(jsonNode, Map.class);
				if (map != null) {
					list.add(map);
				}
			}
			
			logger.debug("JSON 파싱 성공 - 파싱된 객체 수: {}", list.size());
			
		} catch (Exception e) {
			logger.error("JSON 파싱 실패 - jsonStr: {}, 오류: {}", jsonStr, e.getMessage(), e);
			
			// 기존 방식으로 폴백 시도
			try {
				JSONArray jsonArray = new JSONArray();
				JSONParser jsonParser = new JSONParser();
				jsonArray = (JSONArray) jsonParser.parse(jsonStr);
				
				if (jsonArray != null) {
					int jsonSize = jsonArray.size();
					for (int i = 0; i < jsonSize; i++) {
						try {
							Map<String, Object> map = getMapFromJsonObject((JSONObject) jsonArray.get(i));
							if (map != null) {
								list.add(map);
							}
						} catch (Exception ex) {
							logger.error("JSON 객체 처리 중 오류 (인덱스 {}): {}", i, ex.getMessage());
						}
					}
				}
				logger.debug("기존 방식으로 JSON 파싱 성공 - 파싱된 객체 수: {}", list.size());
			} catch (Exception fallbackEx) {
				logger.error("기존 방식 JSON 파싱도 실패: {}", fallbackEx.getMessage());
			}
		}

		return list;
	}

	public static Map<String, Object> getMapFromJsonObject(JSONObject jsonObject) {
		// null 체크
		if (jsonObject == null) {
			logger.debug("JSONObject가 null입니다.");
			return null;
		}

		Map<String, Object> map = null;

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> tempMap = new ObjectMapper().readValue(jsonObject.toJSONString(), Map.class);
			map = tempMap;
		} catch (JsonParseException e) {
			logger.error("JSON 파싱 오류: {}", e.getMessage());
		} catch (JsonMappingException e) {
			logger.error("JSON 매핑 오류: {}", e.getMessage());
		} catch (IOException e) {
			logger.error("JSON 처리 중 I/O 오류: {}", e.getMessage());
		} catch (Exception e) {
			logger.error("JSON 객체 처리 중 예상치 못한 오류: {}", e.getMessage());
		}

		return map;
	}




	public static String getRootPath() {
		return RootPath;
	}

	public static void setRootPath(String rootPath) {
		RootPath = rootPath;
	}

	public String getDriverByDbType(String dbType) {
		switch (dbType.toUpperCase()) {
		case "ORACLE":
			return "oracle.jdbc.driver.OracleDriver";
		case "POSTGRESQL":
			return "org.postgresql.Driver";
		case "TIBERO":
			return "com.tmax.tibero.jdbc.TbDriver";
		case "DB2":
			return "com.ibm.db2.jcc.DB2Driver";
		case "MYSQL":
			return "com.mysql.jdbc.Driver";
		default:
			return "oracle.jdbc.driver.OracleDriver";
		}
	}

	public String getTestQueryByDbType(String dbType) {
		switch (dbType.toUpperCase()) {
		case "ORACLE":
			return "SELECT 1 FROM DUAL";
		case "POSTGRESQL":
			return "SELECT current_database(), current_user, version()"; // 더 엄격한 테스트
		case "TIBERO":
			return "SELECT 1 FROM DUAL";
		case "DB2":
			return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
		case "MYSQL":
			return "SELECT DATABASE(), USER(), VERSION()"; // 더 엄격한 테스트
		default:
			return "SELECT 1 FROM DUAL";
		}
	}

	/**
	 * DB2 연결 정리를 위한 메서드
	 */
	public static void cleanupDB2Connections() {
		try {
			logger.info("DB2 연결 정리 시작...");

			// DB2 드라이버의 정적 리소스 정리
			System.gc();

			// 잠시 대기하여 정리 완료 확인
			Thread.sleep(2000);

			logger.info("DB2 연결 정리 완료");

		} catch (Exception e) {
			logger.error("DB2 연결 정리 중 오류 발생", e);
		}
	}

	/**
	 * 애플리케이션 종료 시 전체 정리 작업
	 */
	public static void cleanupOnShutdown() {
		try {
			logger.info("애플리케이션 종료 시 정리 작업 시작...");
			
			// DB2 연결 정리
			logger.info("DB2 연결 정리 시작...");
			cleanupDB2Connections();
			logger.info("DB2 연결 정리 완료");

			// 정적 변수 정리
			logger.info("정적 변수 정리 시작...");
			system_properties = "";
			tempPath = "";
			JdbcPath = "";
			RootPath = "";
			LogCOL = "";
			logger.info("정적 변수 정리 완료");

			logger.info("애플리케이션 종료 시 정리 작업 완료");

		} catch (Exception e) {
			logger.error("애플리케이션 종료 시 정리 작업 중 오류 발생", e);
		}
	}

	/**
	 * JDBC 드라이버 목록을 반환합니다.
	 */
	public static List<String> getJdbcDriverList() {
		List<String> driverList = new ArrayList<>();
		File jdbcDir = new File(JdbcPath);

		if (jdbcDir.exists() && jdbcDir.isDirectory()) {
			File[] files = jdbcDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

			if (files != null) {
				for (File file : files) {
					driverList.add(file.getName());
				}
			}
		}

		return driverList;
	}

	/**
	 * JDBC 드라이버 파일의 경로를 반환합니다.
	 */
	public static String getJdbcDriverPath(String driverFileName) {
		return JdbcPath + driverFileName;
	}

	/**
	 * JDBC 드라이버 파일이 존재하는지 확인합니다.
	 */
	public static boolean isJdbcDriverExists(String driverFileName) {
		if (driverFileName == null || driverFileName.trim().isEmpty()) {
			return false;
		}
		File driverFile = new File(getJdbcDriverPath(driverFileName));
		return driverFile.exists() && driverFile.isFile();
	}

	/**
	 * JDBC 드라이버 파일에서 정보를 추출합니다.
	 */
	public Map<String, String> extractDriverInfo(String driverFileName) {
		Map<String, String> driverInfo = new HashMap<>();

		if (driverFileName == null || driverFileName.trim().isEmpty()) {
			driverInfo.put("error", "드라이버 파일명이 지정되지 않았습니다.");
			return driverInfo;
		}

		try {
			String driverClass = getDriverClassFromFileName(driverFileName);
			String version = getVersionFromFileName(driverFileName);
			String description = getDescriptionFromFileName(driverFileName);

			driverInfo.put("driverClass", driverClass);
			driverInfo.put("version", version);
			driverInfo.put("description", description);

		} catch (Exception e) {
			logger.error("드라이버 정보 추출 실패: " + driverFileName, e);
			driverInfo.put("error", "드라이버 정보를 추출할 수 없습니다: " + e.getMessage());
		}

		return driverInfo;
	}

	/**
	 * 파일명에서 드라이버 클래스명을 추출합니다.
	 */
	private String getDriverClassFromFileName(String fileName) {
		String lowerFileName = fileName.toLowerCase();

		if (lowerFileName.contains("db2")) {
			return "com.ibm.db2.jcc.DB2Driver";
		} else if (lowerFileName.contains("oracle") || lowerFileName.contains("ojdbc")) {
			return "oracle.jdbc.driver.OracleDriver";
		} else if (lowerFileName.contains("postgresql") || lowerFileName.contains("postgres")) {
			return "org.postgresql.Driver";
		} else if (lowerFileName.contains("tibero")) {
			return "com.tmax.tibero.jdbc.TbDriver";
		} else if (lowerFileName.contains("mysql")) {
			return "com.mysql.cj.jdbc.Driver";
		} else if (lowerFileName.contains("mariadb")) {
			return "org.mariadb.jdbc.Driver";
		} else if (lowerFileName.contains("sqlserver") || lowerFileName.contains("mssql")) {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		} else {
			// 기본값
			return "com.ibm.db2.jcc.DB2Driver";
		}
	}

	/**
	 * 파일명에서 버전 정보를 추출합니다.
	 */
	private String getVersionFromFileName(String fileName) {
		// 파일명에서 버전 패턴 찾기 (예: ojdbc8-12.2.0.1.jar -> 12.2.0.1)
		java.util.regex.Pattern versionPattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+\\.\\d+(\\.\\d+)?)");
		java.util.regex.Matcher matcher = versionPattern.matcher(fileName);

		if (matcher.find()) {
			return matcher.group(1);
		}

		return "Unknown";
	}

	/**
	 * 파일명에서 설명을 추출합니다.
	 */
	private String getDescriptionFromFileName(String fileName) {
		String lowerFileName = fileName.toLowerCase();

		if (lowerFileName.contains("db2")) {
			return "IBM DB2 JDBC Driver";
		} else if (lowerFileName.contains("oracle") || lowerFileName.contains("ojdbc")) {
			return "Oracle JDBC Driver";
		} else if (lowerFileName.contains("postgresql") || lowerFileName.contains("postgres")) {
			return "PostgreSQL JDBC Driver";
		} else if (lowerFileName.contains("tibero")) {
			return "Tibero JDBC Driver";
		} else if (lowerFileName.contains("mysql")) {
			return "MySQL JDBC Driver";
		} else if (lowerFileName.contains("mariadb")) {
			return "MariaDB JDBC Driver";
		} else if (lowerFileName.contains("sqlserver") || lowerFileName.contains("mssql")) {
			return "SQL Server JDBC Driver";
		} else {
			return "Unknown JDBC Driver";
		}
	}

	// RootPath 유효성 검증 로그 중복 방지를 위한 플래그
	private static String lastInvalidRootPath = null;
	
	/**
	 * RootPath가 유효한지 확인합니다.
	 */
	public static boolean isRootPathValid() {
		if (RootPath == null || RootPath.isEmpty() || RootPath.contains("${system.root.path}")) {
			// 이전에 로그한 경로와 다른 경우에만 로그
			if (!RootPath.equals(lastInvalidRootPath)) {
				logger.error("RootPath가 유효하지 않습니다: {}", RootPath);
				lastInvalidRootPath = RootPath;
			}
			return false;
		}
		
		File rootDir = new File(RootPath);
		if (!rootDir.exists()) {
			// 이전에 로그한 경로와 다른 경우에만 로그
			if (!RootPath.equals(lastInvalidRootPath)) {
				logger.error("Root 디렉토리가 존재하지 않습니다: {}", RootPath);
				lastInvalidRootPath = RootPath;
			}
			return false;
		}
		
		if (!rootDir.isDirectory()) {
			// 이전에 로그한 경로와 다른 경우에만 로그
			if (!RootPath.equals(lastInvalidRootPath)) {
				logger.error("Root 경로가 디렉토리가 아닙니다: {}", RootPath);
				lastInvalidRootPath = RootPath;
			}
			return false;
		}
		
		if (!rootDir.canRead()) {
			// 이전에 로그한 경로와 다른 경우에만 로그
			if (!RootPath.equals(lastInvalidRootPath)) {
				logger.error("Root 디렉토리에 읽기 권한이 없습니다: {}", RootPath);
				lastInvalidRootPath = RootPath;
			}
			return false;
		}
		
		// 유효한 경로인 경우 플래그 초기화
		if (lastInvalidRootPath != null) {
			logger.info("RootPath가 유효해졌습니다: {}", RootPath);
			lastInvalidRootPath = null;
		}
		
		return true;
	}

	private static void createJdbcDirectory() {
		// RootPath 유효성 검증
		if (!isRootPathValid()) {
			logger.error("RootPath가 유효하지 않아 JDBC 폴더를 생성할 수 없습니다: {}", RootPath);
			return;
		}
		
		File jdbcDir = new File(JdbcPath);
		if (!jdbcDir.exists()) {
			try {
				boolean created = jdbcDir.mkdirs();
				logger.info("JDBC 폴더 생성 여부: " + created + " - " + JdbcPath);
			} catch (Exception e) {
				logger.error("JDBC 폴더 생성 실패: " + JdbcPath, e);
			}
		}
	}

}
