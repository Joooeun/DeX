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
import kr.Windmill.service.ConnectionService;
import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.util.Common;

@Controller
public class ConnectionController {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

	@Autowired
	private ConnectionService connectionService;
	
	@Autowired
	private Common com;

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

		String connectionName = request.getParameter("file");
		String propFile = com.ConnectionPath + connectionName;
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
			
			// 캐시 업데이트
			updateConnectionCache(connectionName);
			
			logger.info("연결 설정 저장 및 캐시 업데이트 완료: {}", connectionName);
			
		} catch (IOException e) {
			logger.error("연결 설정 저장 실패: {}", connectionName, e);
			e.printStackTrace();
		}
	}
	
	/**
	 * 연결 캐시를 업데이트합니다.
	 * @param connectionName 연결 이름
	 */
	private void updateConnectionCache(String connectionName) {
		try {
			// 기존 캐시 제거
			connectionService.removeConnectionFromCache(connectionName);
			
			// 새로운 연결 설정으로 캐시 생성
			ConnectionDTO newConnection = connectionService.createConnectionDTO(connectionName);
			
			// DataSource도 새로 생성
			connectionService.getConnectionPoolManager().removeDataSource(connectionName);
			connectionService.getConnectionPoolManager().getDataSource(connectionName);
			
			logger.info("연결 캐시 업데이트 완료: {}", connectionName);
			
		} catch (Exception e) {
			logger.error("연결 캐시 업데이트 실패: {}", connectionName, e);
		}
	}
	
	/**
	 * 연결을 삭제합니다.
	 * @param request HTTP 요청
	 * @param session HTTP 세션
	 */
	@ResponseBody
	@RequestMapping(path = "/Connection/delete")
	public Map<String, Object> deleteConnection(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();
		String connectionName = request.getParameter("connectionName");
		
		try {
			// 파일 삭제
			String propFile = com.ConnectionPath + connectionName + ".properties";
			File file = new File(propFile);
			
			if (file.exists()) {
				boolean deleted = file.delete();
				if (deleted) {
					// 캐시에서도 제거
					connectionService.removeConnectionFromCache(connectionName);
					
					result.put("success", true);
					result.put("message", "연결이 성공적으로 삭제되었습니다.");
					logger.info("연결 삭제 완료: {}", connectionName);
				} else {
					result.put("success", false);
					result.put("error", "파일 삭제에 실패했습니다.");
				}
			} else {
				result.put("success", false);
				result.put("error", "연결 파일을 찾을 수 없습니다.");
			}
			
		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "연결 삭제 중 오류 발생: " + e.getMessage());
			logger.error("연결 삭제 실패: {}", connectionName, e);
		}
		
		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/status")
	public List<ConnectionStatusDTO> getConnectionStatus(HttpServletRequest request, HttpSession session) {
		String id = (String) session.getAttribute("memberId");
		return connectionService.getConnectionStatusesForUser(id);
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/status/refresh")
	public String refreshConnectionStatus(HttpServletRequest request, HttpSession session) {
		String connectionName = request.getParameter("connectionName");
		if (connectionName != null && !connectionName.isEmpty()) {
			connectionService.updateConnectionStatusManually(connectionName);
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

			// 연결 정보를 Map으로 수집
			Map<String, String> connConfig = new HashMap<>();
			connConfig.put("IP", request.getParameter("IP"));
			connConfig.put("PORT", request.getParameter("PORT"));
			connConfig.put("DB", request.getParameter("DB"));
			connConfig.put("USER", request.getParameter("USER"));
			connConfig.put("PW", request.getParameter("PW"));
			connConfig.put("DBTYPE", request.getParameter("DBTYPE"));
			connConfig.put("JDBC_DRIVER_FILE", request.getParameter("JDBC_DRIVER_FILE"));

			// ConnectionService의 testConnection 메서드 사용 (예외 정보 포함)
			Map<String, Object> testResult = connectionService.testConnectionWithDetails(connConfig);

			// 결과 처리
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;

			if ((Boolean) testResult.get("success")) {
				result.put("success", true);
				result.put("duration", duration);
				result.put("message", "연결이 성공적으로 완료되었습니다.");
				
				// 드라이버 정보 추가
				String jdbcDriverFile = request.getParameter("JDBC_DRIVER_FILE");
				if (jdbcDriverFile != null && !jdbcDriverFile.trim().isEmpty()) {
					Map<String, String> driverInfo = connectionService.extractDriverInfo(jdbcDriverFile);
					result.put("driverClass", driverInfo.get("driverClass"));
					result.put("version", driverInfo.get("version"));
				}

				logger.info("연결 테스트 성공 - {}:{} ({}ms)", 
					request.getParameter("IP"), request.getParameter("PORT"), duration);
			} else {
				result.put("success", false);
				result.put("error", (String) testResult.get("error"));
				result.put("errorType", (String) testResult.get("errorType"));
				logger.error("연결 테스트 실패 - {}:{} (DB: {}, Type: {}, User: {}) - {}", 
					request.getParameter("IP"), 
					request.getParameter("PORT"),
					request.getParameter("DB"),
					request.getParameter("DBTYPE"),
					request.getParameter("USER"),
					testResult.get("error"));
			}

		} catch (Exception e) {
			result.put("success", false);
			result.put("error", "연결 테스트 중 오류 발생: " + e.getMessage());
			result.put("errorType", "SYSTEM_ERROR");
			logger.error("연결 테스트 중 예외 발생", e);
		}

		return result;
	}
}
