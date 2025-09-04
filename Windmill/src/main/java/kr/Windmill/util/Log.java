package kr.Windmill.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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

		// 파일은 모두 저장으로 변경 20240619
//		if (data.getConnection().equals(LogDB) || !(data.isAudit() || data.getId().equals("admin"))) {
//			return;
//		}

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
		
		// DEXLOG 테이블에 삽입 (기존 순서 유지)
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
			data.getStart(),                                // EXECUTE_DATE
			data.getXmlLog(),                               // XML_LOG (파라미터 정보)
			data.getLogId()                                 // LOG_ID
		);
		
		logger.debug("DEXLOG 저장 완료: {} - {}", data.getId(), data.getConnectionId());
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

	// ==================== SqlTemplateExecuteDto 로깅 메서드들 ====================
	
	/**
	 * SqlTemplateExecuteDto를 사용한 로그 시작
	 */
	public void log_start(SqlTemplateExecuteDto executeDto, String msg) {
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
		
		// 파라미터 정보를 JSON 형태로 변환
		String paramJson = "{}";
		if (executeDto.getParameterList() != null && !executeDto.getParameterList().isEmpty()) {
			try {
				// 간단한 JSON 형태로 변환 (실제로는 Jackson 등을 사용하는 것이 좋음)
				StringBuilder json = new StringBuilder("{");
				for (int i = 0; i < executeDto.getParameterList().size(); i++) {
					java.util.Map<String, Object> param = executeDto.getParameterList().get(i);
					if (i > 0) json.append(",");
					json.append("\"").append(param.get("title")).append("\":\"").append(param.get("value")).append("\"");
				}
				json.append("}");
				paramJson = json.toString();
			} catch (Exception e) {
				logger.warn("파라미터 JSON 변환 실패: {}", e.getMessage());
				paramJson = "{}";
			}
		}
		
		// DEXLOG 테이블에 삽입 (기존 순서 유지)
		String sql = "INSERT INTO DEXLOG (" +
			"USER_ID, IP, CONN_DB, MENU, SQL_TYPE, RESULT_ROWS, " +
			"SQL_TEXT, RESULT_MSG, DURATION, EXECUTE_DATE, XML_LOG, LOG_ID" +
			") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		jdbcTemplate.update(sql,
			executeDto.getMemberId(),                        // USER_ID
			executeDto.getIp(),                              // IP
			executeDto.getConnectionId(),                     // CONN_DB
			"SQL_TEMPLATE",                                  // MENU (템플릿 실행임을 표시)
			getExecutionType(executeDto.getSqlContent()),    // SQL_TYPE
			executeDto.getRows(),                           // RESULT_ROWS
			executeDto.getSqlContent(),                     // SQL_TEXT
			executeDto.getResult(),                         // RESULT_MSG
			duration,                                       // DURATION
			executeDto.getStartTime(),                      // EXECUTE_DATE
			paramJson,                                      // XML_LOG (파라미터 정보)
			executeDto.getTemplateId() + "_" + executeDto.getStartTime().toEpochMilli()  // LOG_ID
		);
		
		logger.debug("DEXLOG 저장 완료 (Template): {} - {}", executeDto.getMemberId(), executeDto.getConnectionId());
	}

}
