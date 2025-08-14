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
	public Map<String, Object> detail(HttpServletRequest request, Model model, HttpSession session) throws IOException {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			String connectionId = request.getParameter("connectionId");
			String connectionType = request.getParameter("connectionType");

			// DB 기반 연결 상세 조회
			Map<String, Object> connectionDetail = connectionService.getConnectionDetail(connectionId, connectionType);

			if (connectionDetail != null) {
				result.put("success", true);
				result.put("data", connectionDetail);
			} else {
				result.put("success", false);
				result.put("message", "연결을 찾을 수 없습니다.");
			}

		} catch (Exception e) {
			logger.error("연결 상세 조회 실패", e);
			result.put("success", false);
			result.put("message", "연결 상세 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/list")
	public Map<String, Object> Connection_list(HttpServletRequest request, Model model, HttpSession session) throws IOException {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			// TYPE 파라미터 확인 (sql.jsp에서 사용하는 경우)
			String type = request.getParameter("TYPE");

			if ("DB".equals(type)) {
				// sql.jsp에서 사용: 사용자가 권한을 가진 DB 연결만 반환
				List<String> dbConnections = connectionService.getUserDatabaseConnections(userId);
				result.put("success", true);
				result.put("data", dbConnections);
			} else {
				// 관리화면에서 사용: 모든 연결 목록 (페이징 포함)
				String searchKeyword = request.getParameter("searchKeyword");
				String typeFilter = request.getParameter("typeFilter");
				int page = Integer.parseInt(request.getParameter("page") != null ? request.getParameter("page") : "1");
				int pageSize = Integer.parseInt(request.getParameter("pageSize") != null ? request.getParameter("pageSize") : "10");

				Map<String, Object> data = connectionService.getConnectionListWithPagination(userId, searchKeyword, typeFilter, page, pageSize);

				result.put("success", true);
				result.put("data", data.get("connections"));
				result.put("pagination", data.get("pagination"));
			}

		} catch (Exception e) {
			logger.error("연결 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "연결 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/sessionCon")
	public void sessionCon(HttpServletRequest request, HttpSession session) {

		session.setAttribute("connectionId", request.getParameter("connectionId"));

		return;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/save")
	public Map<String, Object> save(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			// 연결 데이터 수집 (새로운 필드명 지원)
			Map<String, Object> connectionData = new HashMap<>();
			connectionData.put("CONNECTION_ID", request.getParameter("CONNECTION_ID"));
			connectionData.put("TYPE", request.getParameter("TYPE"));
			connectionData.put("HOST_IP", request.getParameter("HOST_IP") != null ? request.getParameter("HOST_IP") : request.getParameter("IP"));
			connectionData.put("PORT", request.getParameter("PORT"));
			connectionData.put("DATABASE_NAME", request.getParameter("DATABASE_NAME") != null ? request.getParameter("DATABASE_NAME") : request.getParameter("DB"));
			connectionData.put("USERNAME", request.getParameter("USERNAME") != null ? request.getParameter("USERNAME") : request.getParameter("USER"));
			connectionData.put("PASSWORD", request.getParameter("PASSWORD") != null ? request.getParameter("PASSWORD") : request.getParameter("PW"));
			connectionData.put("DB_TYPE", request.getParameter("DB_TYPE") != null ? request.getParameter("DB_TYPE") : request.getParameter("DBTYPE"));
			connectionData.put("JDBC_DRIVER_FILE", request.getParameter("JDBC_DRIVER_FILE"));
			connectionData.put("TEST_SQL", request.getParameter("TEST_SQL"));

			// SFTP 관련 필드
			connectionData.put("PRIVATE_KEY_PATH", request.getParameter("PRIVATE_KEY_PATH"));
			connectionData.put("REMOTE_PATH", request.getParameter("REMOTE_PATH"));
			connectionData.put("CONNECTION_TIMEOUT", request.getParameter("CONNECTION_TIMEOUT"));

			// DB 기반 저장
			boolean saveResult = connectionService.saveConnection(connectionData, userId);

			if (saveResult) {
				// 커넥션 풀 이벤트 처리
				String connectionId = connectionData.get("CONNECTION_ID").toString();
				if (connectionId != null && !connectionId.trim().isEmpty()) {
					// 기존 연결이 있으면 수정, 없으면 생성
					if (connectionService.isConnectionExists(connectionId)) {
						connectionService.onConnectionUpdated(connectionId);
					} else {
						connectionService.onConnectionCreated(connectionId);
					}
				}

				result.put("success", true);
				result.put("message", "연결이 성공적으로 저장되었습니다.");
				logger.info("연결 설정 저장 완료: {}", connectionId);
			} else {
				result.put("success", false);
				result.put("message", "연결 저장에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("연결 저장 실패", e);
			result.put("success", false);
			result.put("message", "연결 저장 중 오류가 발생했습니다: " + e.getMessage());
		}

		return result;
	}

	/**
	 * 연결을 삭제합니다.
	 * 
	 * @param request HTTP 요청
	 * @param session HTTP 세션
	 */
	@ResponseBody
	@RequestMapping(path = "/Connection/delete")
	public Map<String, Object> deleteConnection(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			String connectionId = request.getParameter("connectionId");
			String connectionType = request.getParameter("connectionType");

			// DB 기반 삭제
			boolean deleteResult = connectionService.deleteConnection(connectionId, connectionType);

			if (deleteResult) {
				// 커넥션 풀 삭제
				connectionService.onConnectionDeleted(connectionId);

				result.put("success", true);
				result.put("message", "연결이 성공적으로 삭제되었습니다.");
				logger.info("연결 삭제 완료: {}", connectionId);
			} else {
				result.put("success", false);
				result.put("message", "연결 삭제에 실패했습니다.");
			}

		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "연결 삭제 중 오류가 발생했습니다: " + e.getMessage());
			logger.error("연결 삭제 실패", e);
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
		String connectionId = request.getParameter("connectionId");
		if (connectionId != null && !connectionId.isEmpty()) {
			connectionService.updateConnectionStatusManually(connectionId);
			return "success";
		}
		return "error";
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/jdbcDrivers")
	public Map<String, Object> getJdbcDrivers(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			List<String> drivers = Common.getJdbcDriverList();
			result.put("success", true);
			result.put("data", drivers);

		} catch (Exception e) {
			logger.error("JDBC 드라이버 목록 조회 실패", e);
			result.put("success", false);
			result.put("message", "JDBC 드라이버 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
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
	@RequestMapping(path = "/Connection/test")
	public Map<String, Object> testConnection(HttpServletRequest request, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
			if (userId == null) {
				result.put("success", false);
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			// 기존 연결 테스트 (connectionId로)
			String connectionId = request.getParameter("connectionId");

			// 새 연결 테스트 (폼 데이터로)
			long startTime = System.currentTimeMillis();

			// 연결 정보를 Map으로 수집 (새로운 필드명 지원)
			Map<String, String> connConfig = new HashMap<>();
			connConfig.put("IP", request.getParameter("HOST_IP") != null ? request.getParameter("HOST_IP") : request.getParameter("IP"));
			connConfig.put("PORT", request.getParameter("PORT"));
			connConfig.put("DB", request.getParameter("DATABASE_NAME") != null ? request.getParameter("DATABASE_NAME") : request.getParameter("DB"));
			connConfig.put("USER", request.getParameter("USERNAME") != null ? request.getParameter("USERNAME") : request.getParameter("USER"));
			connConfig.put("PW", request.getParameter("PASSWORD") != null ? request.getParameter("PASSWORD") : request.getParameter("PW"));
			connConfig.put("DBTYPE", request.getParameter("DB_TYPE") != null ? request.getParameter("DB_TYPE") : request.getParameter("DBTYPE"));
			connConfig.put("JDBC_DRIVER_FILE", request.getParameter("JDBC_DRIVER_FILE"));
			connConfig.put("TEST_SQL", request.getParameter("TEST_SQL"));

			// ConnectionService의 testConnection 메서드 사용 (예외 정보 포함)
			Map<String, Object> testResult = connectionService.testDatabaseConnection(connConfig);

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

				logger.info("연결 테스트 성공 - {}:{} ({}ms)", connConfig.get("IP"), connConfig.get("PORT"), duration);
			} else {
				result.put("success", false);
				result.put("message", "연결 테스트 실패: " + testResult.get("error"));
				logger.error("연결 테스트 실패 - {}:{} (DB: {}, Type: {}, User: {}) - {}", connConfig.get("IP"), connConfig.get("PORT"), connConfig.get("DB"), connConfig.get("DBTYPE"), connConfig.get("USER"), testResult.get("error"));
			}

		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "연결 테스트 중 오류 발생: " + e.getMessage());
			logger.error("연결 테스트 중 예외 발생", e);
		}

		return result;
	}

}
