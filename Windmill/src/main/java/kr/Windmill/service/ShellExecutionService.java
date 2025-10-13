package kr.Windmill.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Service
public class ShellExecutionService {
	
	private static final Logger logger = LoggerFactory.getLogger(ShellExecutionService.class);
	
	/**
	 * SSH를 통해 원격 서버에서 쉘 스크립트 실행
	 * 
	 * @param hostIp 호스트 IP
	 * @param port 포트 번호
	 * @param username 사용자명
	 * @param password 비밀번호
	 * @param script 실행할 스크립트
	 * @param parameters 파라미터 (JSON 형태)
	 * @param timeoutSeconds 타임아웃 (초)
	 * @return 실행 결과
	 */
	public Map<String, Object> executeShellScript(String hostIp, int port, String username, 
			String password, String script, String parameters, int timeoutSeconds) {
		
		Map<String, Object> result = new HashMap<>();
		Session session = null;
		Channel channel = null;
		
		try {
			// JSch 객체 생성
			JSch jsch = new JSch();
			
			// SSH 세션 생성
			session = jsch.getSession(username, hostIp, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setConfig("PreferredAuthentications", "password");
			session.setTimeout(timeoutSeconds * 1000);
			
			// 연결
			session.connect();
			logger.info("SSH 연결 성공: {}@{}:{}", username, hostIp, port);
			
			// 실행 채널 생성
			channel = session.openChannel("exec");
			ChannelExec execChannel = (ChannelExec) channel;
			
			// 파라미터 치환된 스크립트 생성
			String processedScript = processScriptParameters(script, parameters);
			logger.info("실행할 스크립트: {}", processedScript.substring(0, Math.min(100, processedScript.length())));
			
			// 스크립트 실행 명령 설정
			execChannel.setCommand(processedScript);
			execChannel.setInputStream(null);
			execChannel.setErrStream(System.err);
			
			// 입력/출력 스트림 설정
			InputStream inputStream = execChannel.getInputStream();
			InputStream errorStream = execChannel.getErrStream();
			
			// 채널 연결
			execChannel.connect();
			logger.info("쉘 스크립트 실행 시작");
			
			// 결과 수집
			StringBuilder output = new StringBuilder();
			StringBuilder error = new StringBuilder();
			
			// 출력 스트림 읽기
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
			
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
			
			// 에러 스트림 읽기
			while ((line = errorReader.readLine()) != null) {
				error.append(line).append("\n");
			}
			
			// 실행 완료 대기
			execChannel.disconnect();
			
			// 결과 처리
			if (execChannel.getExitStatus() == 0) {
				result.put("success", true);
				result.put("output", output.toString());
				result.put("error", error.toString());
				logger.info("쉘 스크립트 실행 성공");
			} else {
				result.put("success", false);
				result.put("output", output.toString());
				result.put("error", "스크립트 실행 실패 (Exit Code: " + execChannel.getExitStatus() + ")\n" + error.toString());
				logger.warn("쉘 스크립트 실행 실패 - Exit Code: {}", execChannel.getExitStatus());
			}
			
		} catch (JSchException e) {
			logger.error("SSH 연결 실패", e);
			result.put("success", false);
			result.put("error", "SSH 연결 실패: " + e.getMessage());
		} catch (IOException e) {
			logger.error("스크립트 실행 중 IO 오류", e);
			result.put("success", false);
			result.put("error", "스크립트 실행 중 IO 오류: " + e.getMessage());
		} catch (Exception e) {
			logger.error("쉘 실행 중 예상치 못한 오류", e);
			result.put("success", false);
			result.put("error", "쉘 실행 중 오류 발생: " + e.getMessage());
		} finally {
			// 리소스 정리
			if (channel != null && channel.isConnected()) {
				channel.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		
		return result;
	}
	
	/**
	 * 스크립트에 파라미터 치환 적용
	 * 
	 * @param script 원본 스크립트
	 * @param parametersJson 파라미터 JSON
	 * @return 파라미터가 치환된 스크립트
	 */
	private String processScriptParameters(String script, String parametersJson) {
		if (parametersJson == null || parametersJson.trim().isEmpty()) {
			return script;
		}
		
		try {
			// JSON 파싱 (간단한 구현)
			// TODO: 실제 JSON 파싱 라이브러리 사용 권장
			String processedScript = script;
			
			// 파라미터 치환 로직
			// 예: ${PARAM1} -> 실제 값으로 치환
			// 현재는 기본적인 치환만 구현
			if (parametersJson.contains("PARAM1")) {
				// 실제 파라미터 값으로 치환하는 로직 구현 필요
				logger.info("파라미터 치환 처리: {}", parametersJson);
			}
			
			return processedScript;
		} catch (Exception e) {
			logger.warn("파라미터 치환 중 오류, 원본 스크립트 사용: {}", e.getMessage());
			return script;
		}
	}
	
	/**
	 * 연결 테스트
	 * 
	 * @param hostIp 호스트 IP
	 * @param port 포트 번호
	 * @param username 사용자명
	 * @param password 비밀번호
	 * @return 연결 테스트 결과
	 */
	public Map<String, Object> testConnection(String hostIp, int port, String username, String password) {
		Map<String, Object> result = new HashMap<>();
		Session session = null;
		
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(username, hostIp, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setConfig("PreferredAuthentications", "password");
			session.setTimeout(10000); // 10초 타임아웃
			
			session.connect();
			
			result.put("success", true);
			result.put("message", "연결 성공");
			logger.info("SSH 연결 테스트 성공: {}@{}:{}", username, hostIp, port);
			
		} catch (JSchException e) {
			logger.error("SSH 연결 테스트 실패", e);
			result.put("success", false);
			result.put("error", "연결 실패: " + e.getMessage());
		} finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		
		return result;
	}
}
