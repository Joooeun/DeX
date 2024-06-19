package kr.Windmill.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogInfoDTO {
	private String Connection;
	private String id;
	private String ip;

	private String sql;
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
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
		setSqlType(sql.toUpperCase().split("\\s")[0]);
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
				xmlLog += entry.getValue();
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

}
