package kr.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.ConnectionStatusDTO;
import kr.Windmill.service.ConnectionStatusService;
import kr.Windmill.util.Common;

@Controller
public class ConnectionController {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

	@Autowired
	private ConnectionStatusService connectionStatusService;
	
	Common com = new Common();

	@RequestMapping(path = "/Connection", method = RequestMethod.GET)
	public ModelAndView Connection(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/detail")
	public Map<String, String> detail(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		Map<String, String> map = com.ConnectionConf(request.getParameter("DB"));

		return map;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/list")
	public List<String> Connection_list(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		List<String> dblist = com.ConnectionnList(request.getParameter("TYPE"));
		String id = (String) session.getAttribute("memberId");

		if (!id.equals("admin")) {
			Map<String, String> map = com.UserConf(id);
			List<String> strList = new ArrayList<>(Arrays.asList(map.get("CONNECTION").split(",")));

			
			return dblist.stream().filter(con ->
			strList.contains(con.split("\\.")[0])).collect(Collectors.toList());

		}

		return dblist;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/sessionCon")
	public void sessionCon(HttpServletRequest request, HttpSession session) {

		session.setAttribute("Connection", request.getParameter("Connection"));

		return;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/save")
	public void save(HttpServletRequest request, HttpSession session) {

		String propFile = com.ConnectionPath + request.getParameter("file");
		File file = new File(propFile + ".properties");

		try {
			String str = "#" + request.getParameter("TYPE") + "\n";
			FileWriter fw = new FileWriter(file);
			str += "TYPE=" + request.getParameter("TYPE") + "\n";
			str += "IP=" + request.getParameter("IP") + "\n";
			str += "PORT=" + request.getParameter("PORT") + "\n";
			if (request.getParameter("TYPE").equals("DB")) {
				str += "DB=" + request.getParameter("DB") + "\n";
			}
			str += "USER=" + request.getParameter("USER") + "\n";
			str += "PW=" + request.getParameter("PW") + "\n";
			str += "DBTYPE=" + request.getParameter("DBTYPE");

			// JDBC 드라이버 파일 정보 추가
			if (request.getParameter("JDBC_DRIVER_FILE") != null && !request.getParameter("JDBC_DRIVER_FILE").trim().isEmpty()) {
				str += "\nJDBC_DRIVER_FILE=" + request.getParameter("JDBC_DRIVER_FILE").trim();
			}

			fw.write(com.cryptStr(str));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/status")
	public List<ConnectionStatusDTO> getConnectionStatus(HttpServletRequest request, HttpSession session) {
		String id = (String) session.getAttribute("memberId");
		return connectionStatusService.getConnectionStatusesForUser(id);
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/status/refresh")
	public String refreshConnectionStatus(HttpServletRequest request, HttpSession session) {
		String connectionName = request.getParameter("connectionName");
		if (connectionName != null && !connectionName.isEmpty()) {
			connectionStatusService.updateConnectionStatusManually(connectionName);
			return "success";
		}
		return "error";
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/jdbcDrivers")
	public List<String> getJdbcDrivers(HttpServletRequest request, HttpSession session) {
		return Common.getJdbcDriverList();
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/driverInfo")
	public Map<String, String> getDriverInfo(HttpServletRequest request, HttpSession session) {
		String driverFile = request.getParameter("driverFile");
		Map<String, String> result = new HashMap<>();

		try {
			// JDBC 드라이버 파일에서 정보 추출
			Map<String, String> driverInfo = com.extractDriverInfo(driverFile);
			result.put("driverClass", driverInfo.get("driverClass"));
			result.put("version", driverInfo.get("version"));
			result.put("description", driverInfo.get("description"));
		} catch (Exception e) {
			logger.error("드라이버 정보 추출 실패: " + driverFile, e);
			result.put("error", "드라이버 정보를 추출할 수 없습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/testConnection")
	public Map<String, Object> testConnection(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			// 테스트 시작 시간
			long startTime = System.currentTimeMillis();

			// 연결 정보 수집
			String ip = request.getParameter("IP");
			String port = request.getParameter("PORT");
			String db = request.getParameter("DB");
			String user = request.getParameter("USER");
			String pw = request.getParameter("PW");
			String dbtype = request.getParameter("DBTYPE");
			String jdbcDriverFile = request.getParameter("JDBC_DRIVER_FILE");

			// 임시 ConnectionDTO 생성
			kr.Windmill.service.ConnectionDTO testConnection = new kr.Windmill.service.ConnectionDTO();

			// Properties 설정
			java.util.Properties prop = new java.util.Properties();
			prop.put("user", user);
			prop.put("password", pw);
			testConnection.setProp(prop);
			testConnection.setDbtype(dbtype);
			testConnection.setJdbcDriverFile(jdbcDriverFile);

			// JDBC URL 생성
			String jdbcUrl = "";
			String driverClass = "";

			if (jdbcDriverFile != null && !jdbcDriverFile.trim().isEmpty()) {
				// 사용자 지정 드라이버 파일 사용
				Map<String, String> driverInfo = com.extractDriverInfo(jdbcDriverFile.trim());
				driverClass = driverInfo.get("driverClass");
			} else {
				// 기본 드라이버는 DB2 사용
				driverClass = "com.ibm.db2.jcc.DB2Driver";
			}

			// JDBC URL 생성 (Common의 공통 메서드 사용)
			jdbcUrl = com.createJdbcUrl(dbtype, ip, port, db);

			testConnection.setDriver(driverClass);
			testConnection.setJdbc(jdbcUrl);

			// 실제 연결 테스트 (Common의 공통 메서드 사용)
			java.sql.Connection conn = null;
			try {
				// Common의 동적 드라이버 연결 메서드 사용
				conn = com.createConnectionWithDynamicDriver(jdbcUrl, prop, jdbcDriverFile, driverClass);

				// 연결 성공
				long endTime = System.currentTimeMillis();
				long duration = endTime - startTime;

				result.put("success", true);
				result.put("driverClass", driverClass);
				result.put("version", com.extractDriverInfo(jdbcDriverFile != null ? jdbcDriverFile : "").get("version"));
				result.put("duration", duration);
				result.put("message", "연결이 성공적으로 완료되었습니다.");

				logger.info("연결 테스트 성공 - {}:{} ({}ms)", ip, port, duration);

			} catch (ClassNotFoundException e) {
				result.put("success", false);
				result.put("error", "드라이버 클래스를 찾을 수 없습니다: " + driverClass);
				logger.error("연결 테스트 실패 - 드라이버 클래스 없음: {}", driverClass, e);
			} catch (java.sql.SQLException e) {
				result.put("success", false);
				result.put("error", "데이터베이스 연결 실패: " + e.getMessage());
				logger.error("연결 테스트 실패 - SQL 오류: {}:{}", ip, port, e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (java.sql.SQLException e) {
						logger.warn("테스트 연결 종료 실패", e);
					}
				}
			}

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "연결 테스트 중 오류 발생: " + e.getMessage());
			logger.error("연결 테스트 중 예외 발생", e);
		}

		return result;
	}
}
