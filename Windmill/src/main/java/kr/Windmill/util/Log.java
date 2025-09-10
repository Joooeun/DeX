package kr.Windmill.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.log.LogInfoDto;
import kr.Windmill.dto.SqlTemplateExecuteDto;

@Component
public class Log {
	private static final Logger logger = LoggerFactory.getLogger(Log.class);
	private final Common com;
	private final JdbcTemplate jdbcTemplate;
	
	
	@Autowired
	public Log(Common common, JdbcTemplate jdbcTemplate) {
		this.com = common;
		this.jdbcTemplate = jdbcTemplate;
	}


	public void log_start(LogInfoDto data, String msg) {

		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (data.getId() == null || data.getId().trim().isEmpty()) {
			return;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
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

			writer.write(strNowDate2 + " id : " + data.getId() + " / ip :  " + data.getIp() + "\nDB : " + data.getConnectionId() + " / MENU : " + data.getTitle() + msg);
			writer.close();
		} catch (IOException e) {
			logger.error("로그 파일 쓰기 실패", e);
		}
	}

	public void log_end(LogInfoDto data, String msg) {

		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (data.getId() == null || data.getId().trim().isEmpty()) {
			return;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
				}
			}

			path += File.separator + data.getId() + "_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);

			writer.write("start:" + data.getLogId() + ":==============================================\n" + data.getLogsql() + "\nend:" + data.getLogId() + ":==============================================" + "\nDB : " + data.getConnectionId() + " / MENU : " + data.getTitle() + msg);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			logger.error("로그 파일 쓰기 실패", e);
		}
	}

	public void log_line(LogInfoDto data, String msg) {

		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (data.getId() == null || data.getId().trim().isEmpty()) {
			return;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
				}
			}

			path += File.separator + data.getId() + "_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);

			writer.write(data.getLogId() + "\n" + msg);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			logger.error("로그 파일 쓰기 실패", e);
		}
	}

	public void userLog(String user, String ip, String msg) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
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
			logger.error("사용자 로그 파일 쓰기 실패", e);
		}
	}

	public void errorLog(String msg) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
				}
			}

			path += File.separator + "error_log_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);
			SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate2 = simpleDateFormat2.format(nowDate);

			writer.write(strNowDate2 + " " + msg);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			logger.error("에러 로그 파일 쓰기 실패", e);
		}
	}

	public void monitoringLog(String component, String message) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = com.RootPath + "log" + File.separator + "monitoring";
			File folder = new File(path);

			// RootPath 유효성 확인 후 폴더 생성
			if (!Common.isRootPathValid()) {
				logger.error("RootPath가 유효하지 않아 모니터링 로그 폴더를 생성할 수 없습니다: {}", com.RootPath);
				return;
			}

		if (!folder.exists()) {
			try {
				folder.mkdirs();
			} catch (Exception e) {
				logger.error("모니터링 로그 폴더 생성 실패", e);
			}
		}

			path += File.separator + "monitoring_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);
			SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate2 = simpleDateFormat2.format(nowDate);

			writer.write(strNowDate2 + " [" + component + "] " + message);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			logger.error("모니터링 로그 파일 쓰기 실패", e);
		}
	}

	public void log_DB(LogInfoDto data) {

		if (!data.isAudit()) {
			return;
		}

		// JdbcTemplate을 사용하여 DEXLOG 테이블에 직접 저장 (톰캣 DB 설정 사용)
		insertDexLog(data);

	}
	
	/**
	 * DEXLOG 테이블에 로그를 저장합니다 (톰캣 DB 설정 사용).
	 */
	private void insertDexLog(LogInfoDto data) {
		// 실행 시간 계산
		long duration = 0;
		if (data.getStart() != null && data.getEnd() != null) {
			duration = java.time.Duration.between(data.getStart(), data.getEnd()).toMillis();
		}
		
		// XML_LOG 필드 처리 (null이거나 빈 문자열인 경우 기본 XML 제공)
		String xmlLog = data.getXmlLog();
		if (xmlLog == null || xmlLog.trim().isEmpty()) {
			xmlLog = "<xml></xml>";
		}
		
		// DEXLOG 테이블에 삽입 (기존 순서 유지)
		try {
			String sql = "INSERT INTO DEXLOG (" +
				"USER_ID, IP, CONN_DB, MENU, SQL_TYPE, RESULT_ROWS, " +
				"SQL_TEXT, RESULT_MSG, DURATION, EXECUTE_DATE, XML_LOG, LOG_ID" +
				") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			jdbcTemplate.update(sql,
			data.getId(),                                    // USER_ID
			data.getIp(),                                    // IP
			data.getConnectionId(),                           // CONN_DB
			data.getTitle(),                                // MENU
			getExecutionType(data.getLogsql()),             // SQL_TYPE
				data.getRows(),                                 // RESULT_ROWS
			data.getLogsql(),                               // SQL_TEXT
			data.getResult(),                               // RESULT_MSG
				duration,                                       // DURATION
				data.getStart() != null ? java.sql.Timestamp.from(data.getStart()) : null,  // EXECUTE_DATE
				xmlLog,                                         // XML_LOG (파라미터 정보)
			data.getLogId()                                 // LOG_ID
			);
			
			logger.debug("DEXLOG 저장 완료: {} - {}", data.getId(), data.getConnectionId());
		} catch (Exception e) {
			logger.error("DEXLOG 저장 실패 (LogInfoDto): {} - {}", data.getId(), data.getConnectionId(), e);
			// 오류가 발생해도 흐름을 중단하지 않음
		}
	}
	

	
	/**
	 * XML 특수문자 인코딩 메서드 (LogInfoDto와 동일)
	 */
	private String encodeXml(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
	}

	/**
	 * SQL 텍스트에서 실행 타입을 추출합니다.
	 */
	private String getExecutionType(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return "UNKNOWN";
		}
		
		String firstWord = sql.trim().split("\\s+")[0].toUpperCase();
		
		switch (firstWord) {
			case "SELECT":
				return "SELECT";
			case "INSERT":
				return "INSERT";
			case "UPDATE":
				return "UPDATE";
			case "DELETE":
				return "DELETE";
			case "CALL":
				return "CALL";
			default:
				return "OTHER";
		}
	}

	/**
	 * 메뉴 실행 시작 로그를 EXECUTION_LOG 테이블에 INSERT합니다 (PENDING 상태).
	 */
	public void logMenuExecutionStart(LogInfoDto data) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (data.getId() == null || data.getId().trim().isEmpty()) {
			return;
		}

		try {
			// EXECUTION_LOG 테이블에 PENDING 상태로 삽입
			String sql = "INSERT INTO EXECUTION_LOG (" +
				"LOG_ID, USER_ID, TEMPLATE_ID, CONNECTION_ID, SQL_TYPE, EXECUTION_STATUS, " +
				"SQL_CONTENT, EXECUTION_START_TIME" +
				") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			
			jdbcTemplate.update(sql,
				data.getLogId(),                                // LOG_ID
				data.getId(),                                   // USER_ID
				data.getTitle(),                                // TEMPLATE_ID
				data.getConnectionId(),                         // CONNECTION_ID
				getExecutionType(data.getLogsql()),            // SQL_TYPE
				"PENDING",                                      // EXECUTION_STATUS
				data.getLogsql(),                              // SQL_CONTENT
				data.getStart() != null ? java.sql.Timestamp.from(data.getStart()) : null  // EXECUTION_START_TIME
			);
			
			logger.debug("EXECUTION_LOG 시작 저장 완료: {} - {}", data.getId(), data.getTitle());
			
		} catch (Exception e) {
			logger.error("EXECUTION_LOG 시작 저장 실패", e);
		}
	}

	/**
	 * 메뉴 실행 완료 로그를 EXECUTION_LOG 테이블에서 UPDATE합니다.
	 */
	public void logMenuExecutionEnd(LogInfoDto data) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (data.getId() == null || data.getId().trim().isEmpty()) {
			return;
		}

		try {
			// 실행 시간 계산
			long duration = 0;
			if (data.getStart() != null && data.getEnd() != null) {
				duration = java.time.Duration.between(data.getStart(), data.getEnd()).toMillis();
			}
			
			// 실행 상태 판단
			String executionStatus = "SUCCESS";
			String errorMessage = null;
			
			if (data.getResult() != null) {
				if ("Success".equals(data.getResult())) {
					executionStatus = "SUCCESS";
				} else {
					executionStatus = "FAIL";
					errorMessage = data.getResult();
				}
			}
			
			// PENDING 상태 판단 (30초 이상 걸린 경우)
			if (duration > 30000 && "SUCCESS".equals(executionStatus)) {
				executionStatus = "PENDING";
			}
			
			// EXECUTION_LOG 테이블에서 업데이트
			String sql = "UPDATE EXECUTION_LOG SET " +
				"EXECUTION_STATUS = ?, DURATION = ?, AFFECTED_ROWS = ?, " +
				"ERROR_MESSAGE = ?, EXECUTION_END_TIME = ? " +
				"WHERE LOG_ID = ?";
			
			jdbcTemplate.update(sql,
				executionStatus,                                // EXECUTION_STATUS
				duration,                                       // DURATION
				data.getRows(),                                 // AFFECTED_ROWS
				errorMessage,                                   // ERROR_MESSAGE
				data.getEnd() != null ? java.sql.Timestamp.from(data.getEnd()) : null,  // EXECUTION_END_TIME
				data.getLogId()                                 // LOG_ID
			);
			
			logger.debug("EXECUTION_LOG 완료 업데이트 완료: {} - {}", data.getId(), data.getTitle());
			
		} catch (Exception e) {
			logger.error("EXECUTION_LOG 완료 업데이트 실패", e);
		}
	}

	/**
	 * SqlTemplateExecuteDto를 사용한 메뉴 실행 시작 로그를 EXECUTION_LOG 테이블에 INSERT합니다 (PENDING 상태).
	 */
	public void logMenuExecutionStart(SqlTemplateExecuteDto executeDto) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (executeDto.getMemberId() == null || executeDto.getMemberId().trim().isEmpty()) {
			return;
		}

		try {
			// LOG_ID 생성 (템플릿 ID + 타임스탬프)
			String logId = executeDto.getTemplateId() + "_" + executeDto.getStartTime().toEpochMilli();
			
			// EXECUTION_LOG 테이블에 PENDING 상태로 삽입
			String sql = "INSERT INTO EXECUTION_LOG (" +
				"LOG_ID, USER_ID, TEMPLATE_ID, CONNECTION_ID, SQL_TYPE, EXECUTION_STATUS, " +
				"SQL_CONTENT, EXECUTION_START_TIME" +
				") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			
			jdbcTemplate.update(sql,
				logId,                                           // LOG_ID
				executeDto.getMemberId(),                        // USER_ID
				executeDto.getTemplateId(),                      // TEMPLATE_ID
				executeDto.getConnectionId(),                     // CONNECTION_ID
				getExecutionType(executeDto.getSqlContent()),    // SQL_TYPE
				"PENDING",                                       // EXECUTION_STATUS
				executeDto.getSqlContent(),                      // SQL_CONTENT
				java.sql.Timestamp.from(executeDto.getStartTime())  // EXECUTION_START_TIME
			);
			
			logger.debug("EXECUTION_LOG 시작 저장 완료 (Template): {} - {}", executeDto.getMemberId(), executeDto.getTemplateId());
			
		} catch (Exception e) {
			logger.error("EXECUTION_LOG 시작 저장 실패 (Template)", e);
		}
	}

	/**
	 * SqlTemplateExecuteDto를 사용한 메뉴 실행 완료 로그를 EXECUTION_LOG 테이블에서 UPDATE합니다.
	 */
	public void logMenuExecutionEnd(SqlTemplateExecuteDto executeDto) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (executeDto.getMemberId() == null || executeDto.getMemberId().trim().isEmpty()) {
			return;
		}

		try {
			// LOG_ID 생성 (템플릿 ID + 타임스탬프)
			String logId = executeDto.getTemplateId() + "_" + executeDto.getStartTime().toEpochMilli();
			
			// 실행 시간 계산
			long duration = 0;
			if (executeDto.getStartTime() != null && executeDto.getEndTime() != null) {
				duration = java.time.Duration.between(executeDto.getStartTime(), executeDto.getEndTime()).toMillis();
			}
			
			// 실행 상태 판단
			String executionStatus = "SUCCESS";
			String errorMessage = null;
			
			if (executeDto.getResult() != null) {
				if ("Success".equals(executeDto.getResult())) {
					executionStatus = "SUCCESS";
				} else {
					executionStatus = "FAIL";
					errorMessage = executeDto.getResult();
				}
			}
			
			// PENDING 상태 판단 (30초 이상 걸린 경우)
			if (duration > 30000 && "SUCCESS".equals(executionStatus)) {
				executionStatus = "PENDING";
			}
			
			// EXECUTION_LOG 테이블에서 업데이트
			String sql = "UPDATE EXECUTION_LOG SET " +
				"EXECUTION_STATUS = ?, DURATION = ?, AFFECTED_ROWS = ?, " +
				"ERROR_MESSAGE = ?, EXECUTION_END_TIME = ? " +
				"WHERE LOG_ID = ?";
			
			jdbcTemplate.update(sql,
				executionStatus,                                 // EXECUTION_STATUS
				duration,                                        // DURATION
				executeDto.getRows(),                           // AFFECTED_ROWS
				errorMessage,                                    // ERROR_MESSAGE
				java.sql.Timestamp.from(executeDto.getEndTime()),  // EXECUTION_END_TIME
				logId                                            // LOG_ID
			);
			
			logger.debug("EXECUTION_LOG 완료 업데이트 완료 (Template): {} - {}", executeDto.getMemberId(), executeDto.getTemplateId());
			
		} catch (Exception e) {
			logger.error("EXECUTION_LOG 완료 업데이트 실패 (Template)", e);
		}
	}

	// ==================== SqlTemplateExecuteDto 로깅 메서드들 ====================
	
	/**
	 * SqlTemplateExecuteDto를 사용한 로그 시작
	 */
	public void log_start(SqlTemplateExecuteDto executeDto, String msg) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (executeDto.getMemberId() == null || executeDto.getMemberId().trim().isEmpty()) {
			return;
		}

		// RootPath 유효성 확인
		if (!Common.isRootPathValid()) {
			logger.error("RootPath가 유효하지 않아 템플릿 로그를 기록할 수 없습니다: {}", com.RootPath);
			return;
		}

		// 파일은 모두 저장으로 변경 20240619
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(java.util.Date.from(executeDto.getStartTime()));

		try {
			String path = com.RootPath + "log";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
				}
			}

			path += File.separator + executeDto.getMemberId() + "_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);
			SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate2 = simpleDateFormat2.format(java.util.Date.from(executeDto.getStartTime()));

			writer.write(strNowDate2 + " id : " + executeDto.getMemberId() + " / ip :  " + executeDto.getIp() + 
				"\nDB : " + executeDto.getConnectionId() + " / TEMPLATE : " + executeDto.getTemplateId() + msg);
			writer.close();
		} catch (IOException e) {
			logger.error("템플릿 로그 시작 파일 쓰기 실패", e);
		}
	}

	/**
	 * SqlTemplateExecuteDto를 사용한 로그 종료
	 */
	public void log_end(SqlTemplateExecuteDto executeDto, String msg) {
		// 사용자 ID가 null이거나 빈 문자열인 경우 로그를 남기지 않음
		if (executeDto.getMemberId() == null || executeDto.getMemberId().trim().isEmpty()) {
			return;
		}

		// RootPath 유효성 확인
		if (!Common.isRootPathValid()) {
			logger.error("RootPath가 유효하지 않아 템플릿 로그를 기록할 수 없습니다: {}", com.RootPath);
			return;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(java.util.Date.from(executeDto.getStartTime()));

		try {
			String path = com.RootPath + "log";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					folder.mkdirs();
				} catch (Exception e) {
					logger.error("로그 폴더 생성 실패", e);
				}
			}

			path += File.separator + executeDto.getMemberId() + "_" + strNowDate + ".log";

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter writer = new BufferedWriter(fw);

			// SQL 내용을 가져오기 위해 파라미터 정보 포함
			String sqlContent = executeDto.getSqlContent() != null ? executeDto.getSqlContent() : "SQL Template: " + executeDto.getTemplateId();
			String logId = executeDto.getTemplateId() + "_" + executeDto.getStartTime().toEpochMilli();

			writer.write("start:" + logId + ":==============================================\n" + sqlContent + 
				"\nend:" + logId + ":==============================================" + 
				"\nDB : " + executeDto.getConnectionId() + " / TEMPLATE : " + executeDto.getTemplateId() + msg);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			logger.error("템플릿 로그 종료 파일 쓰기 실패", e);
		}
	}

	/**
	 * SqlTemplateExecuteDto를 사용한 DB 로그 저장
	 */
	public void log_DB(SqlTemplateExecuteDto executeDto) {
		if (!executeDto.getAudit()) {
			return;
		}

		// JdbcTemplate을 사용하여 DEXLOG 테이블에 직접 저장 (톰캣 DB 설정 사용)
		insertDexLog(executeDto);
	}
	
	/**
	 * SqlTemplateExecuteDto를 DEXLOG 테이블에 저장합니다 (톰캣 DB 설정 사용).
	 */
	private void insertDexLog(SqlTemplateExecuteDto executeDto) {
		// 실행 시간 계산
		long duration = 0;
		if (executeDto.getStartTime() != null && executeDto.getEndTime() != null) {
			duration = java.time.Duration.between(executeDto.getStartTime(), executeDto.getEndTime()).toMillis();
		}
		
		// XML_LOG는 LOG 파라미터만 저장 (예전 소스와 동일한 방식)
		String paramXml = "<xml></xml>";
		if (executeDto.getLog() != null && !executeDto.getLog().trim().isEmpty()) {
			try {
				// LOG 파라미터 JSON을 파싱하여 XML로 변환 (예전 LogInfoDTO.setLog()와 동일)
				ObjectMapper objectMapper = new ObjectMapper();
				HashMap<String, String> dataMap = objectMapper.readValue(executeDto.getLog(), HashMap.class);
				
				StringBuilder xml = new StringBuilder("<xml>");
				for (Map.Entry<String, String> entry : dataMap.entrySet()) {
					xml.append("<").append(entry.getKey()).append(">");
					xml.append(encodeXml(entry.getValue()));
					xml.append("</").append(entry.getKey()).append(">");
				}
				xml.append("</xml>");
				paramXml = xml.toString();
			} catch (Exception e) {
				logger.warn("LOG 파라미터 XML 변환 실패: {}", e.getMessage());
				paramXml = "<xml></xml>";
			}
		}
		
		
		// DEXLOG 테이블에 삽입 (기존 순서 유지)
		try {
			String sql = "INSERT INTO DEXLOG (" +
				"USER_ID, IP, CONN_DB, MENU, SQL_TYPE, RESULT_ROWS, " +
				"SQL_TEXT, RESULT_MSG, DURATION, EXECUTE_DATE, XML_LOG, LOG_ID" +
				") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			jdbcTemplate.update(sql,
			executeDto.getMemberId(),                        // USER_ID
			executeDto.getIp(),                              // IP
			executeDto.getConnectionId(),                     // CONN_DB
			executeDto.getTemplateId(),                      // MENU (템플릿 ID)
			getExecutionType(executeDto.getSqlContent()),    // SQL_TYPE
				executeDto.getRows(),                           // RESULT_ROWS
			executeDto.getSqlContent(),                     // SQL_TEXT
			executeDto.getResult(),                         // RESULT_MSG
				duration,                                       // DURATION
				java.sql.Timestamp.from(executeDto.getStartTime()) ,  // EXECUTE_DATE
				paramXml,                                       // XML_LOG (파라미터 정보)
			executeDto.getTemplateId() + "_" + executeDto.getStartTime().toEpochMilli()  // LOG_ID
			);
			
			logger.debug("DEXLOG 저장 완료 (Template): {} - {}", executeDto.getMemberId(), executeDto.getConnectionId());
		} catch (Exception e) {
			logger.error("DEXLOG 저장 실패 (SqlTemplateExecuteDto): {} - {}", executeDto.getMemberId(), executeDto.getConnectionId(), e);
			// 오류가 발생해도 흐름을 중단하지 않음
		}
	}


}
