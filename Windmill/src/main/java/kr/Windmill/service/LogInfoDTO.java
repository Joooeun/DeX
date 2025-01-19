package kr.Windmill.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.controller.SQLController.SqlType;

public class LogInfoDTO {
	private String Connection;
	private String id;
	private String logId;
	private int logNo = 0;
	private String ip;

	private String sql;
	private String logsql;
	private String log;
	private String Path;
	private boolean autocommit;
	private int limit;

	private Instant start;
	private Instant end;

	private String sqlType;
	private int rows;
	private String result;

	private boolean audit = false;

	private long duration = 0;

	private String xmlLog;
	private Map mapLog;
	private String params;
	private List<Map<String, Object>> paramList;

	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getConnection() {
		return Connection;
	}

	public void setConnection(String connection) {
		Connection = connection;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSql() {
		return (sql == null ? "" : sql).trim();
	}

	public void setSql(String sql) {
		this.sql = sql == null ? "" : sql;
		setSqlType(firstword(sql));

	}

	public Map<String, String> getLog() {
		return mapLog;
	}

	public void setLog(String log) {
		this.log = log;

		ObjectMapper objectMapper = new ObjectMapper();
		HashMap<String, String> dataMap;
		try {
			dataMap = objectMapper.readValue(log, HashMap.class);

			this.mapLog = dataMap;

			String xmlLog = "<xml>";
			for (Map.Entry<String, String> entry : dataMap.entrySet()) {
				xmlLog += "<" + entry.getKey() + ">";
				xmlLog += encodeXml(entry.getValue());
				xmlLog += "</" + entry.getKey() + ">";
			}
			xmlLog += "</xml>";
			this.xmlLog = xmlLog;

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 특수문자 인코딩 메서드
	public static String encodeXml(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
	}

	public String getXmlLog() {
		return xmlLog;
	}

	public void setXmlLog(String xmlLog) {
		this.xmlLog = xmlLog;
	}

	public String getPath() {
		return Path;
	}

	public void setPath(String path) {
		Path = path;
	}

	public boolean isAutocommit() {
		return autocommit;
	}

	public void setAutocommit(boolean autocommit) {
		this.autocommit = autocommit;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public Instant getStart() {
		return start;
	}

	public void setStart(Instant start) {
		this.start = start;
	}

	public Instant getEnd() {
		return end;
	}

	public void setEnd(Instant end) {
		this.end = end;

		setDuration((Duration.between(getStart(), getEnd())).toMillis());

	}

	public String getSqlType() {
		return sqlType;
	}

	private void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getResult() {
		return result;
	}

	public boolean isAudit() {
		return audit;
	}

	public void setAudit(boolean audit) {
		this.audit = audit;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public List<Map<String, Object>> getParamList() {
		return paramList;
	}

	public void setParamList(List<Map<String, Object>> paramList) {
		this.paramList = paramList;
	}

	public String getLogsql() {
		return logsql;
	}

	public void setLogsql(String logsql) {
		this.logsql = logsql;
	}

	public void setLogsqlA(String sql) {

		this.logsql = sql;

		for (int i = 0; i < paramList.size(); i++) {

			if (paramList.get(i).get("type").equals("string") || paramList.get(i).get("type").equals("text") || paramList.get(i).get("type").equals("varchar")) {

				this.logsql = logsql.replaceAll(":" + paramList.get(i).get("title"), "\'" + paramList.get(i).get("value").toString() + "\'");
			} else
				this.logsql = logsql.replaceAll(":" + paramList.get(i).get("title"), paramList.get(i).get("value").toString());

		}

	}

	public String getLogId() {
		return logId;
	}

	public void setLogId(String logId) {
		this.logId = logId;
	}

	public int getLogNo() {
		return logNo;
	}

	public void setLogNo(int logNo) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddHHmmssSSS");
		String strNowDate = simpleDateFormat.format(Date.from(this.start));

		this.logId = this.id + "_" + this.title + "_" + strNowDate + "_" + logNo;
		this.logNo = logNo;
	}

	// SQL에서 주석 제거
	public static String removeComments(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			throw new IllegalArgumentException("SQL query cannot be null or empty");
		}

		// 정규식을 사용해 단일 줄 및 다중 줄 주석 제거
		String singleLineCommentRegex = "--.*";
		String multiLineCommentRegex = "\\/\\*[\\s\\S]*?\\*\\/";

		sql = sql.replaceAll(singleLineCommentRegex, " ");
		sql = sql.replaceAll(multiLineCommentRegex, " ");
		return sql.trim();
	}

	// SQL 유형 판별
	public static String firstword(String sql) {
		String cleanedSql = removeComments(sql);

		// 첫 번째 단어 추출
		String firstWord = cleanedSql.split("\\s+")[0].toUpperCase();

		return firstWord;
	}

	// SQL 유형 판별
	public static SqlType detectSqlType(String sql) {

		switch (firstword(sql)) {
		case "CALL":
		case "BEGIN":
			return SqlType.CALL;
		case "SELECT":
		case "WITH":
		case "VALUE":
			return SqlType.EXECUTE;
		default:
			return SqlType.UPDATE;
		}
	}

}
