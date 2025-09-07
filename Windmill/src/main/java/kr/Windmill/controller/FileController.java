package kr.Windmill.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import kr.Windmill.util.Common;
import kr.Windmill.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Controller
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);
	private final Common com;
	private final Log cLog;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public FileController(Common common, Log log, JdbcTemplate jdbcTemplate) {
		this.com = common;
		this.cLog = log;
		this.jdbcTemplate = jdbcTemplate;
	}

	@RequestMapping(path = "/FileRead")
	public ModelAndView FileRead(HttpServletRequest request, ModelAndView mv, HttpSession session) throws IOException {
		
		logger.info("PATH: {}", request.getParameter("Path"));
		mv.addObject("Path", request.getParameter("Path"));

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/FILE/readfile")
	public Map<String, String> readfile(HttpServletRequest request, Model model, HttpSession session1) throws ClassNotFoundException, JSchException, IOException {

		Map<String, String> map = new HashMap<>();
		String connectionId = request.getParameter("connectionId");
		String filePath = request.getParameter("FilePath");
		
		logger.info("SFTP 파일 읽기 요청 - connectionId: {}, filePath: {}", connectionId, filePath);
		
		// 입력 파라미터 검증
		if (connectionId == null || connectionId.trim().isEmpty()) {
			logger.error("SFTP 연결 ID가 지정되지 않음");
			map.put("error", "연결 ID가 지정되지 않았습니다.");
			return map;
		}
		
		if (filePath == null || filePath.trim().isEmpty()) {
			logger.error("파일 경로가 지정되지 않음");
			map.put("error", "파일 경로가 지정되지 않았습니다.");
			return map;
		}
		
		try {
			String sql = "SELECT * FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			Map<String, Object> connInfo = jdbcTemplate.queryForMap(sql, connectionId);
			
			map.put("USER", (String) connInfo.get("USERNAME"));
			map.put("IP", (String) connInfo.get("HOST_IP"));
			map.put("PORT", String.valueOf(connInfo.get("PORT")));
			map.put("PW", (String) connInfo.get("PASSWORD"));
			
			logger.debug("SFTP 연결 정보 조회 성공 - IP: {}, PORT: {}", map.get("IP"), map.get("PORT"));
		} catch (Exception e) {
			logger.error("SFTP 연결 정보 조회 실패: {} - {}", connectionId, e.getMessage(), e);
			map.put("error", "SFTP 연결 정보를 찾을 수 없습니다: " + connectionId);
			return map;
		}

		Session session = null;
		Channel channel = null;
		ChannelSftp sftpChannel = null;
		BufferedReader br = null;
		String filestr = "";

		try {
			// 1. JSch 객체를 생성한다.
			JSch jsch = new JSch();
			// 2. 세션 객체를 생성한다(사용자 이름, 접속할 호스트, 포트를 인자로 전달한다.)
			session = jsch.getSession(map.get("USER"), map.get("IP"), Integer.parseInt(map.get("PORT")));
			// 3. 세션과 관련된 정보를 설정한다.
			session.setConfig("StrictHostKeyChecking", "no");
			// 4. 패스워드를 설정한다.
			session.setPassword(map.get("PW"));
			// 5. 접속한다.
			session.connect();
			logger.debug("SFTP 세션 연결 성공");

			// 6. sftp 채널을 연다.
			channel = session.openChannel("sftp");
			// 7. 채널에 연결한다.
			channel.connect();
			// 8. 채널을 FTP용 채널 객체로 캐스팅한다.
			sftpChannel = (ChannelSftp) channel;
			logger.debug("SFTP 채널 연결 성공");

			// 파일 경로 처리
			String fileName = filePath;
			String cdDir = fileName.substring(0, fileName.lastIndexOf("/"));
			String fileNameOnly = fileName.substring(fileName.lastIndexOf("/") + 1);
			
			logger.debug("파일 디렉토리: {}, 파일명: {}", cdDir, fileNameOnly);
			
			// 디렉토리 변경
			sftpChannel.cd("/");
			sftpChannel.cd(cdDir);

			// 파일 읽기
			br = new BufferedReader(new InputStreamReader(sftpChannel.get(fileNameOnly)));
			String line = "";
			while ((line = br.readLine()) != null) {
				filestr += line + "\r\n";
			}
			
			logger.info("SFTP 파일 읽기 성공 - 파일 크기: {} bytes", filestr.length());
			map.clear();
			map.put("result", filestr);

		} catch (Exception e) {
			logger.error("SFTP 파일 읽기 실패 - connectionId: {}, filePath: {}", connectionId, filePath, e);
			map.clear();
			map.put("error", "파일 읽기 실패: " + e.getMessage());
		} finally {
			// 리소스 정리
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("BufferedReader 닫기 실패", e);
				}
			}
			if (sftpChannel != null) {
				sftpChannel.disconnect();
			}
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}

		return map;

	}

	@RequestMapping(path = "/FileUpload", method = RequestMethod.GET)
	public ModelAndView FileUpload(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		return mv;
	}
	
	@ResponseBody
	@RequestMapping(path = "/log-error")
	public String ViewError(@RequestBody Map<String, Object> errorData) throws JsonProcessingException {
		String errorJson = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(errorData);
		logger.debug("에러 로그 데이터: {}", errorJson);
		
		cLog.errorLog(errorJson);
		
		return "done";
	}

	@ResponseBody
	@RequestMapping(path = "/FILE/uploadfile")
	public String uploadfile(HttpServletRequest request, Model model, HttpSession session1) throws ClassNotFoundException, JSchException, IOException {

		String connectionId = request.getParameter("connectionId");
		String filePath = request.getParameter("FilePath");
		String content = request.getParameter("Content");
		
		logger.info("SFTP 파일 업로드 요청 - connectionId: {}, filePath: {}", connectionId, filePath);
		
		// 입력 파라미터 검증
		if (connectionId == null || connectionId.trim().isEmpty()) {
			logger.error("SFTP 연결 ID가 지정되지 않음");
			return "error: 연결 ID가 지정되지 않았습니다.";
		}
		
		if (filePath == null || filePath.trim().isEmpty()) {
			logger.error("파일 경로가 지정되지 않음");
			return "error: 파일 경로가 지정되지 않았습니다.";
		}
		
		if (content == null) {
			logger.error("파일 내용이 지정되지 않음");
			return "error: 파일 내용이 지정되지 않았습니다.";
		}

		// temp 폴더 사용하지 않음 - 메모리에서 직접 처리

		String result = "";
		Map<String, String> map = new HashMap<>();
		try {
			String sql = "SELECT * FROM SFTP_CONNECTION WHERE SFTP_CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			Map<String, Object> connInfo = jdbcTemplate.queryForMap(sql, connectionId);
			
			map.put("USER", (String) connInfo.get("USERNAME"));
			map.put("IP", (String) connInfo.get("HOST_IP"));
			map.put("PORT", String.valueOf(connInfo.get("PORT")));
			map.put("PW", (String) connInfo.get("PASSWORD"));
			
			logger.debug("SFTP 연결 정보 조회 성공 - IP: {}, PORT: {}", map.get("IP"), map.get("PORT"));
		} catch (Exception e) {
			logger.error("SFTP 연결 정보 조회 실패: {} - {}", connectionId, e.getMessage(), e);
			return "error: SFTP 연결 정보를 찾을 수 없습니다: " + connectionId;
		}

		Session session = null;
		Channel channel = null;
		ChannelSftp sftpChannel = null;
		InputStream in = null;
		
		try {
			// 1. JSch 객체를 생성한다.
			JSch jsch = new JSch();
			// 2. 세션 객체를 생성한다(사용자 이름, 접속할 호스트, 포트를 인자로 전달한다.)
			session = jsch.getSession(map.get("USER"), map.get("IP"), Integer.parseInt(map.get("PORT")));
			// 3. 세션과 관련된 정보를 설정한다.
			session.setConfig("StrictHostKeyChecking", "no");
			// 4. 패스워드를 설정한다.
			session.setPassword(map.get("PW"));
			// 5. 접속한다.
			session.connect();
			logger.debug("SFTP 세션 연결 성공");

			// 6. sftp 채널을 연다.
			channel = session.openChannel("sftp");
			// 7. 채널에 연결한다.
			channel.connect();
			// 8. 채널을 FTP용 채널 객체로 캐스팅한다.
			sftpChannel = (ChannelSftp) channel;
			logger.debug("SFTP 채널 연결 성공");

			// 메모리에서 직접 파일 업로드 (temp 폴더 사용하지 않음)
			String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			byte[] contentBytes = content.getBytes("UTF-8");
			in = new ByteArrayInputStream(contentBytes);
			logger.debug("메모리에서 직접 파일 업로드 준비 완료 - 파일명: {}, 크기: {} bytes", fileName, contentBytes.length);
			String targetDir = filePath.substring(0, filePath.lastIndexOf("/"));
			sftpChannel.cd("/");
			sftpChannel.cd(targetDir);
			sftpChannel.put(in, fileName);
			
			logger.info("SFTP 파일 업로드 성공 - 파일 크기: {} bytes (temp 폴더 사용하지 않음)", content.length());
			result = "success";
			
		} catch (Exception e) {
			logger.error("SFTP 파일 업로드 실패 - connectionId: {}, filePath: {}", connectionId, filePath, e);
			result = "error: " + e.getMessage();
		} finally {
			// 리소스 정리
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("InputStream 닫기 실패", e);
				}
			}
			if (sftpChannel != null) {
				sftpChannel.exit();
				sftpChannel.disconnect();
			}
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		return result;

	}

}
