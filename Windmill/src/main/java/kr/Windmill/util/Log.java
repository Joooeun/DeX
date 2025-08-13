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

import kr.Windmill.service.LogInfoDTO;

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


	public void log_start(LogInfoDTO data, String msg) {

		// 파일은 모두 저장으로 변경 20240619
//		if (data.getConnection().equals(LogDB) || !(data.isAudit() || data.getId().equals("admin"))) {
//			return;
//		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
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

			writer.write(strNowDate2 + " id : " + data.getId() + " / ip :  " + data.getIp() + "\nDB : " + data.getConnectionId() + " / MENU : " + data.getTitle() + msg);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log_end(LogInfoDTO data, String msg) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
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

			writer.write("start:" + data.getLogId() + ":==============================================\n" + data.getLogsql() + "\nend:" + data.getLogId() + ":==============================================" + "\nDB : " + data.getConnectionId() + " / MENU : " + data.getTitle() + msg);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log_line(LogInfoDTO data, String msg) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(Date.from(data.getStart()));

		try {

			String path = com.RootPath + "log";
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

			writer.write(data.getLogId() + "\n" + msg);
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

			String path = com.RootPath + "log";
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

	public void errorLog(String msg) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = com.RootPath + "log";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					logger.info("폴더생성여부 : " + folder.mkdirs());
				} catch (Exception e) {
					e.getStackTrace();
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
			e.printStackTrace();
		}
	}

	public void monitoringLog(String component, String message) {

		Date nowDate = new Date();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd");
		String strNowDate = simpleDateFormat.format(nowDate);

		try {

			String path = com.RootPath + "log" + File.separator + "monitoring";
			File folder = new File(path);

			if (!folder.exists()) {
				try {
					logger.info("monitoring 폴더생성여부 : " + folder.mkdirs());
				} catch (Exception e) {
					e.getStackTrace();
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
			e.printStackTrace();
		}
	}

	public void log_DB(LogInfoDTO data) {

		if (data.getConnectionId().equals(com.LogDB) || !data.isAudit()) {
			return;
		}

		// JdbcTemplate을 사용하여 DEXLOG 테이블에 직접 저장 (톰캣 DB 설정 사용)
		insertDexLog(data);

	}
	
	/**
	 * DEXLOG 테이블에 로그를 저장합니다 (톰캣 DB 설정 사용).
	 */
	private void insertDexLog(LogInfoDTO data) {
		// 실행 시간 계산
		long duration = 0;
		if (data.getStart() != null && data.getEnd() != null) {
			duration = java.time.Duration.between(data.getStart(), data.getEnd()).toMillis();
		}
		
		// DEXLOG 테이블에 삽입 (기존 Log.java와 동일한 구조)
		String sql = "INSERT INTO DEXLOG (" +
			"USER_ID, IP, CONN_DB, MENU, SQL_TYPE, RESULT_ROWS, " +
			"SQL_TEXT, RESULT_MSG, DURATION, EXECUTE_DATE, XML_LOG, LOG_ID_REF" +
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
			data.getParams(),                               // XML_LOG (파라미터 정보)
			data.getLogId()                                 // LOG_ID_REF
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

}
