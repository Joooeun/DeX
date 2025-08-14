package kr.Windmill.service;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.Windmill.dto.log.LogInfoDto;
import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;
import kr.Windmill.util.Log;

@Service
public class SQLExecuteService {

	private static final Logger logger = LoggerFactory.getLogger(SQLExecuteService.class);
	private final Common com;
	private final Log cLog;
	private final DynamicJdbcManager dynamicJdbcManager;

	@Autowired
	public SQLExecuteService(Common common, Log log, DynamicJdbcManager dynamicJdbcManager) {
		this.com = common;
		this.cLog = log;
		this.dynamicJdbcManager = dynamicJdbcManager;
	}

	public enum SqlType {
		CALL, EXECUTE, UPDATE
	}

	/**
	 * SQL 실행 공통 메서드
	 * 
	 * @param data     LogInfoDto - SQL 실행 정보
	 * @param memberId 사용자 ID
	 * @param ip       사용자 IP
	 * @return SQL 실행 결과
	 */
	public Map<String, List> executeSQL(LogInfoDto data) throws Exception {
		data.setStart(Instant.now());

		Properties prop = new Properties();
		prop.put("clientProgramName", "DeX");

		String sql = data.getSql().length() > 0 ? data.getSql() : com.FileRead(new File(data.getPath()));
		data.setParamList(com.getJsonObjectFromString(data.getParams()));
		data.setLogsqlA(sql);

		String log = "";
		if (data.getLog() != null) {
			for (Entry<String, String> entry : data.getLog().entrySet()) {
				log += "\n" + entry.getKey() + " : " + entry.getValue();
			}
		}

		Map<String, List> result = new HashMap();
		PreparedStatement pstmt = null;

		try {
			cLog.log_start(data, log + "\nmenu 실행 시작\n");

			List<Map<String, String>> mapping = new ArrayList<Map<String, String>>();

			// 파라미터 매핑 처리
			if (data.getParamList().size() > 0) {
				mapping = processParameterMapping(data, sql);
				sql = data.getSql(); // 매핑 후 수정된 SQL
			}

			String row = "";

			if (detectSqlType(sql) == SqlType.CALL) {
				data.setLogNo(data.getLogNo() + 1);
				result = callprocedure(sql, data, mapping);

				data.setRows(Integer.parseInt(result.get("rowlength").get(data.isAudit() ? 1 : 0).toString()));
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				row = " / rows : " + data.getRows();
				cLog.log_end(data, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);

			} else if (detectSqlType(sql) == SqlType.EXECUTE) {
				data.setLogNo(data.getLogNo() + 1);
				result = excutequery(sql, data, data.getLimit(), mapping);
				data.setRows(result.get("rowbody").size() - 1);
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);

			} else {
				// UPDATE 타입 처리
				result = processUpdateSQL(data, mapping, sql);

				data.setRows(result.size());
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);
			}

		} catch (SQLException e1) {
			// SQL 예외 처리
			Map<String, List> errorResult = new HashMap<>();

			if (errorResult.size() == 0) {
				List<Map<String, String>> rowhead = new ArrayList<>();
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Result");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Updated Rows");
					}
				});
				rowhead.add(new HashMap<String, String>() {
					{
						put("title", "Query");
					}
				});
				errorResult.put("rowhead", rowhead);
			}

			List<List<String>> singleList = new ArrayList<List<String>>();
			if (errorResult.get("rowbody") != null)
				singleList.addAll(errorResult.get("rowbody"));

			List<String> element = new ArrayList<String>();
			element.add(e1.toString());
			element.add("0");
			element.add(data.getSql());

			singleList.add(element);
			errorResult.put("rowbody", singleList);

			data.setResult(e1.getMessage());
			data.setDuration(0);
			cLog.log_end(data, " sql 실행 종료 : 실패 " + e1.getMessage() + "\n\n");
			cLog.log_DB(data);

			logger.error("SQL 실행 실패 - id: {} / sql: {}", data.getId(), data.getSql());
			e1.printStackTrace();

			return errorResult;
		} catch (Exception e) {
			data.setEnd(Instant.now());
			data.setResult("Error");
			Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());
			cLog.log_end(data, " sql 실행 종료 : 실패 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
			cLog.log_DB(data);
			throw e;
		}

		return result;
	}

	/**
	 * 파라미터 매핑 처리
	 */
	private List<Map<String, String>> processParameterMapping(LogInfoDto data, String sql) {
		List<Map<String, String>> mapping = new ArrayList<Map<String, String>>();

		String patternString = "(?<!:):(";
		for (int i = 0; i < data.getParamList().size(); i++) {
			if (data.getParamList().get(i).get("type").equals("string") || data.getParamList().get(i).get("type").equals("text") || data.getParamList().get(i).get("type").equals("varchar")) {
				if (!patternString.equals("(?<!:):("))
					patternString += "|";
				patternString += data.getParamList().get(i).get("title");
			} else {
				sql = sql.replaceAll(":" + data.getParamList().get(i).get("title"), data.getParamList().get(i).get("value").toString());
			}
		}
		patternString += ")";

		if (!patternString.equals("(?<!:):()")) {
			Pattern pattern = Pattern.compile(patternString);
			Matcher matcher = pattern.matcher(sql);
			int cnt = 0;
			while (matcher.find()) {
				Map<String, String> temp = new HashMap<>();
				temp.put("value", data.getParamList().stream().filter(p -> p.get("title").equals(matcher.group(1))).findFirst().get().get("value").toString());
				temp.put("type", data.getParamList().stream().filter(p -> p.get("title").equals(matcher.group(1))).findFirst().get().get("type").toString());

				mapping.add(temp);
				cnt++;
			}
			matcher.reset();
			sql = matcher.replaceAll("?");
		}

		data.setSql(sql);
		return mapping;
	}

	public Map<String, List> excutequery(String sql, LogInfoDto data, int limit, List<Map<String, String>> mapping) throws SQLException, Exception {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = dynamicJdbcManager.getConnection(data.getConnectionId());

			con.setAutoCommit(false);

			Map<String, List> result = new HashMap<String, List>();

			pstmt = con.prepareStatement(sql);
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":

					pstmt.setString(i + 1, mapping.get(i).get("value"));
					break;

				default:
					pstmt.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			if (limit > 0) {
				pstmt.setMaxRows(limit);
			}
			rs = pstmt.executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();

			int colcnt = rsmd.getColumnCount();

			List rowhead = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			String column;

			for (int index = 0; index < colcnt; index++) {

				Map head = new HashMap();

				ResultSet resultSet = con.getMetaData().getColumns(null, rsmd.getSchemaName(index + 1), rsmd.getTableName(index + 1), rsmd.getColumnName(index + 1));

				String desc = rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")";

				if (resultSet.next()) {
					String REMARKS = resultSet.getString("REMARKS") == null ? "" : "\n" + resultSet.getString("REMARKS");
					desc += REMARKS;
				}

				head.put("title", rsmd.getColumnLabel(index + 1));
				head.put("type", rsmd.getColumnType(index + 1));
				head.put("desc", desc);

				rowhead.add(head);

				rowlength.add(0);
			}

			result.put("rowhead", rowhead);

			List rowbody = new ArrayList<>();

			while (rs.next()) {

				List body = new ArrayList<>();
				for (int index = 0; index < colcnt; index++) {

					// column = rsmd.getColumnName(index + 1);
					// 타입별 get함수 다르게 변경필
					try {

						switch (rsmd.getColumnType(index + 1)) {
						case Types.SQLXML:
							body.add(rs.getSQLXML(index + 1).toString());
							break;
						case Types.DATE:
							body.add(rs.getDate(index + 1).toString());
							break;
						case Types.BIGINT:
						case Types.DECIMAL:
							body.add(rs.getBigDecimal(index + 1).toString());
							break;

						case Types.CLOB:
						case Types.TIMESTAMP:
							body.add(rs.getString(index + 1));
							break;

						default:
							body.add(rs.getObject(index + 1));
							break;
						}

						if (rowlength.get(index) < (body.get(index) == null ? "" : body.get(index)).toString().length()) {
							rowlength.set(index, body.get(index).toString().length() > 100 ? 100 : body.get(index).toString().length());
						}

					} catch (NullPointerException e) {
						body.add(null);
					} catch (Exception e) {
						body.add(e.toString());
					}

				}
				rowbody.add(body);
			}
			result.put("rowbody", rowbody);
			result.put("rowlength", rowlength);

			return result;

		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

			if (con != null) {

				try {
					con.commit();
					con.close();

				} catch (SQLException ex) {
					logger.error(ex.toString());
				}
			} else {

			}
		}
	}

	public Map<String, List> callprocedure(String sql, LogInfoDto data, List<Map<String, String>> mapping) throws SQLException, Exception {

		Connection con = null;

		CallableStatement callStmt1 = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			con = dynamicJdbcManager.getConnection(data.getConnectionId());

			con.setAutoCommit(false);

			Map<String, List> result = new HashMap<String, List>();

			String callcheckstr = "";
			String prcdname = "";

			List<Integer> typelst = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			List rowhead = new ArrayList<>();

			if (sql.indexOf("CALL") > -1) {
				prcdname = sql.substring(sql.indexOf("CALL") + 5, sql.indexOf("("));
				if (prcdname.contains(".")) {
					prcdname = sql.substring(sql.indexOf(".") + 1, sql.indexOf("("));
				}

				int paramcnt = StringUtils.countMatches(sql, ",") + 1;
				// 임
				switch ("DB2") {
				case "DB2":
					callcheckstr = "SELECT * FROM   syscat.ROUTINEPARMS WHERE  routinename = '" + prcdname.toUpperCase().trim() + "' AND SPECIFICNAME = (SELECT SPECIFICNAME " + " FROM   (SELECT SPECIFICNAME, count(*) AS cnt FROM   syscat.ROUTINEPARMS WHERE  routinename = '" + prcdname.toUpperCase().trim() + "' GROUP  BY SPECIFICNAME) a WHERE  a.cnt = " + paramcnt + ") AND ROWTYPE != 'P' ORDER  BY SPECIFICNAME, ordinal";
					break;
				case "ORACLE":
					callcheckstr = "SELECT DATA_TYPE AS TYPENAME\r\n" + "  FROM sys.user_arguments    \r\n" + " WHERE object_name = '" + prcdname.toUpperCase().trim() + "'";
					break;

				default:
					break;
				}

				pstmt = con.prepareStatement(callcheckstr);
				rs = pstmt.executeQuery();

				while (rs.next()) {
					switch (rs.getString("TYPENAME")) {
					case "VARCHAR2":
						typelst.add(Types.VARCHAR);
						break;
					case "VARCHAR":
						typelst.add(Types.VARCHAR);
						break;
					case "INTEGER":
						typelst.add(Types.INTEGER);
						break;
					case "TIMESTAMP":
						typelst.add(Types.TIMESTAMP);
						break;
					case "DATE":
						typelst.add(Types.DATE);
						break;
					}
				}
			}

			callStmt1 = con.prepareCall(sql);
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":

					callStmt1.setString(i + 1, mapping.get(i).get("value"));
					break;

				default:
					// callStmt1.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			for (int i = 0; i < typelst.size(); i++) {
				callStmt1.registerOutParameter(i + 1 + mapping.size(), typelst.get(i));

				Map head = new HashMap();

				head.put("title", (i + 1) + "");
				head.put("type", typelst.get(i));
				head.put("desc", "");

				rowhead.add(head);
			}

			result.put("rowhead", rowhead);

			callStmt1.execute();

			rs2 = callStmt1.getResultSet();

			if (rs2 != null) {
				ResultSetMetaData rsmd = rs2.getMetaData();
				int colcnt = rsmd.getColumnCount();

				String column;

				for (int index = 0; index < colcnt; index++) {

					Map head = new HashMap();

					head.put("title", rsmd.getColumnLabel(index + 1));
					head.put("type", rsmd.getColumnType(index + 1));
					head.put("desc", rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")");

					rowhead.add(head);
					rowlength.add(0);

				}
				result.put("rowhead", rowhead);

				List rowbody = new ArrayList<>();
				while (rs2.next()) {

					List body = new ArrayList<>();
					for (int index = 0; index < colcnt; index++) {

						// column = rsmd.getColumnName(index + 1);
						// 타입별 get함수 다르게 변경필
						try {

							body.add((rsmd.getColumnTypeName(index + 1).equals("CLOB") ? rs2.getString(index + 1) : rs2.getObject(index + 1)));

							if (rowlength.get(index) < (body.get(index) == null ? "" : body.get(index)).toString().length()) {
								rowlength.set(index, body.get(index).toString().length() > 100 ? 100 : body.get(index).toString().length());
							}

						} catch (NullPointerException e) {
							body.add(null);
						} catch (Exception e) {
							body.add(e.toString());
						}
					}

					rowbody.add(body);

				}

				result.put("rowbody", rowbody);
				result.put("rowlength", rowlength);
			} else {

				List rowbody = new ArrayList<>();

				List<String> element = new ArrayList<String>();

				if (typelst.size() > 0) {

					for (int i = 0; i < typelst.size(); i++) {

						element.add(callStmt1.getString(i + 1 + mapping.size()) + "");

					}

					rowlength.add(1);
					rowlength.add(Integer.parseInt(callStmt1.getString(1 + mapping.size())));

				} else {

					Map head = new HashMap();

					head.put("title", "Update Rows");
					head.put("type", Types.VARCHAR);
					head.put("desc", "");

					rowhead.add(head);

					result.put("rowhead", rowhead);

					element.add("" + callStmt1.getUpdateCount());
					rowlength.add(callStmt1.getUpdateCount());
					rowlength.add(callStmt1.getUpdateCount());

				}

				rowbody.add(element);
				result.put("rowbody", rowbody);
				result.put("rowlength", rowlength);
			}

			return result;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

			if (rs2 != null) {
				try {
					rs2.close();
				} catch (Exception e) {
				}
			}

			if (callStmt1 != null) {
				try {
					callStmt1.close();
				} catch (Exception e) {
				}
			}

			if (con != null) {

				try {
					con.commit();
					con.close();

				} catch (SQLException ex) {
					logger.error(ex.toString());
				}
			} else {

			}
		}
	}

	/**
	 * UPDATE SQL 처리
	 */
	public Map<String, List> processUpdateSQL(LogInfoDto data, List<Map<String, String>> mapping, String sql) throws Exception {
		Map<String, List> result = new HashMap<>();

		List<Map<String, String>> rowhead = new ArrayList<>();
		rowhead.add(new HashMap<String, String>() {
			{
				put("title", "Result");
			}
		});
		rowhead.add(new HashMap<String, String>() {
			{
				put("title", "Updated Rows");
			}
		});
		rowhead.add(new HashMap<String, String>() {
			{
				put("title", "Query");
			}
		});
		result.put("rowhead", rowhead);

		String sqlOrg = sql.trim();
		String logsqlOrg = data.getLogsql().trim();

		for (int i = 0; i < sqlOrg.split(";").length; i++) {
			String singleSql = sqlOrg.split(";")[i];
			if (singleSql.trim().length() == 0) {
				continue;
			}

			data.setLogNo(data.getLogNo() + 1);
			sql = singleSql.trim();
			String logsql = logsqlOrg.split(";")[i].trim() + ";";
			data.setSql(sql);
			data.setLogsql(logsql);

			Instant singleStart = Instant.now();
			List<List<String>> singleList = new ArrayList<List<String>>();

			if (result.get("rowbody") != null)
				singleList.addAll(result.get("rowbody"));

			singleList.addAll(executeUpdateWithConnection(data, sql.trim(), mapping));
			result.put("rowbody", singleList);
		}

		return result;
	}

	/**
	 * Connection을 사용한 UPDATE SQL 실행
	 * 
	 * @throws Exception
	 */
	public List<List<String>> executeUpdateWithConnection(LogInfoDto data, String sql, List<Map<String, String>> mapping) throws Exception {
		List<List<String>> result = new ArrayList<>();

		Connection con = null;
		PreparedStatement pstmt = null;

		try {
			con = dynamicJdbcManager.getConnection(data.getConnectionId());
			pstmt = con.prepareStatement(sql);
			// 파라미터 바인딩
			for (int i = 0; i < mapping.size(); i++) {
				switch (mapping.get(i).get("type")) {
				case "string":
				case "text":
				case "varchar":
					pstmt.setString(i + 1, mapping.get(i).get("value"));
					break;
				default:
					pstmt.setInt(i + 1, Integer.parseInt(mapping.get(i).get("value")));
					break;
				}
			}

			int rowcnt = pstmt.executeUpdate();

			List<String> row = new ArrayList<>();
			row.add("success");
			row.add(Integer.toString(rowcnt));
			row.add(sql);
			result.add(row);
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
			}

			if (con != null) {

				try {
					con.commit();
					con.close();

				} catch (SQLException ex) {
					logger.error(ex.toString());
				}
			} else {

			}
		}

		return result;
	}

	/**
	 * SQL 타입 감지
	 */
	public static SqlType detectSqlType(String sql) {
		String firstWord = firstword(sql);
		if (firstWord.equalsIgnoreCase("CALL")) {
			return SqlType.CALL;
		} else if (firstWord.equalsIgnoreCase("SELECT")) {
			return SqlType.EXECUTE;
		} else {
			return SqlType.UPDATE;
		}
	}

	/**
	 * SQL에서 첫 번째 단어 추출
	 */
	public static String firstword(String sql) {
		String[] words = removeComments(sql).trim().split("\\s+");
		return words.length > 0 ? words[0].toUpperCase() : "";
	}

	/**
	 * SQL 주석 제거
	 */
	public static String removeComments(String sql) {
		// 한 줄 주석 제거
		sql = sql.replaceAll("--.*$", "");
		// 블록 주석 제거
		sql = sql.replaceAll("/\\*.*?\\*/", "");
		return sql;
	}
}