package kr.Windmill.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.service.LogInfoDTO;

public class Log {
	private static final Logger logger = LoggerFactory.getLogger(Log.class);
	Common com = new Common();

//	public void tablecheck() {
//
//		ConnectionDTO connection;
//		try {
//			connection = com.getConnection(com.LogDB);
//
//			Properties prop = connection.getProp();
//
//			Class.forName(connection.getDriver());
//			prop.put("clientProgramName", "DeX");
//
//			com.excutequery("", connection.getDbtype(), connection.getJdbc(), prop, 1);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

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

			writer.write(strNowDate2 + " id : " + data.getId() + " / ip :  " + data.getIp() + "\nDB : " + data.getConnection() + " / MENU : " + data.getTitle() + msg);
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

			writer.write("start:" + data.getLogId() + ":==============================================\n" + data.getLogsql() + "\nend:" + data.getLogId() + ":==============================================" + "\nDB : " + data.getConnection() + " / MENU : " + data.getTitle() + msg);
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

	public void log_DB(LogInfoDTO data) {

		if (data.getConnection().equals(com.LogDB) || !data.isAudit()) {
			return;
		}

		try {
			ConnectionDTO connection = com.getConnection(com.LogDB);

			com.updatequery("INSERT INTO DEXLOG (USER_ID, IP, CONN_DB, MENU, SQL_TYPE, RESULT_ROWS, SQL_TEXT, RESULT_MSG, DURATION, EXECUTE_DATE, XML_LOG, LOG_ID)" + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", connection.getDbtype(), connection.getJdbc(), connection.getProp(), data, new ArrayList<Map<String, String>>());
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

}
