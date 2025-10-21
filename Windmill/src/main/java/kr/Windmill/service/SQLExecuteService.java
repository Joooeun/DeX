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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.controller.SQLController.SqlType;
import kr.Windmill.dto.SqlTemplateExecuteDto;
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
	private SqlContentService sqlContentService;
	
	@Autowired
	private SqlTemplateService sqlTemplateService;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

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
		
		// 템플릿 ID가 있는 경우, 해당 DB 연결의 SQL 내용 조회
		if (data.getTemplateId() != null && !data.getTemplateId().trim().isEmpty()) {
			String connectionId = data.getConnectionId();
			
			// connectionId가 null이면 기본 SQL 내용 조회
			if (connectionId == null || connectionId.trim().isEmpty()) {
				Map<String, Object> defaultSqlContent = sqlContentService.getDefaultSqlContent(data.getTemplateId());
				if (defaultSqlContent != null) {
					sql = (String) defaultSqlContent.get("SQL_CONTENT");
					// 기본 템플릿은 connectionId가 null이므로 null로 설정
					connectionId = null;
					logger.info("템플릿 기본 SQL 내용 사용: templateId={}", 
						data.getTemplateId());
				} else {
					logger.warn("템플릿의 기본 SQL 내용을 찾을 수 없음: templateId={}", 
						data.getTemplateId());
				}
			} else {
				// CONNECTION_ID 기반 SQL 내용 조회
				Map<String, Object> sqlContent = sqlContentService.getSqlContentByTemplateAndConnectionId(
					data.getTemplateId(), connectionId);
				
				if (sqlContent != null) {
					sql = (String) sqlContent.get("SQL_CONTENT");
					String contentConnectionId = (String) sqlContent.get("CONNECTION_ID");
					logger.info("템플릿 SQL 내용 사용: templateId={}, requestedConnectionId={}, contentConnectionId={}", 
						data.getTemplateId(), connectionId, contentConnectionId);
				} else {
					logger.warn("템플릿 SQL 내용을 찾을 수 없음: templateId={}, connectionId={}", 
						data.getTemplateId(), connectionId);
				}
			}
		}
		
		data.setParamList(com.getListFromString(data.getParams()));
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
			cLog.logMenuExecutionStart(data);  // 메뉴 실행 시작 로그 저장

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
				cLog.logMenuExecutionEnd(data);  // 메뉴 실행 완료 로그 저장

			} else if (detectSqlType(sql) == SqlType.EXECUTE) {
				data.setLogNo(data.getLogNo() + 1);
				result = excutequery(sql, data, data.getLimit(), mapping);
				data.setRows(result.get("rowbody").size() - 1);
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);
				cLog.logMenuExecutionEnd(data);  // 메뉴 실행 완료 로그 저장

			} else {
				// UPDATE 타입 처리
				result = processUpdateSQL(data, mapping, sql);

				data.setRows(result.size());
				data.setEnd(Instant.now());
				data.setResult("Success");
				Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

				cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(data);
				cLog.logMenuExecutionEnd(data);  // 메뉴 실행 완료 로그 저장
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
			List<Boolean> successList = new ArrayList<>();
			successList.add(false);
			errorResult.put("success", successList);

			data.setResult(e1.getMessage());
			data.setDuration(0);
			cLog.log_end(data, " sql 실행 종료 : 실패 " + e1.getMessage() + "\n\n");
			cLog.log_DB(data);
			cLog.logMenuExecutionEnd(data);  // 메뉴 실행 완료 로그 저장

			logger.error("SQL 실행 실패 - id: {} / sql: {}", data.getId(), data.getSql());
			e1.printStackTrace();

			return errorResult;
		} catch (Exception e) {
			data.setEnd(Instant.now());
			data.setResult("Error");
			Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());
			cLog.log_end(data, " sql 실행 종료 : 실패 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
			cLog.log_DB(data);
			cLog.logMenuExecutionEnd(data);  // 메뉴 실행 완료 로그 저장
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
			
			List<Boolean> successList = new ArrayList<>();
			successList.add(true);
			result.put("success", successList);

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
	public static SqlType detectSqlType(String sql)  {

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
		if (sql == null) return "";
		
		// 블록 주석 제거 (/* ... */)
		sql = sql.replaceAll("/\\*.*?\\*/", "");
		
		// 한 줄 주석 제거 (-- ...)
		String[] lines = sql.split("\n");
		StringBuilder result = new StringBuilder();
		
		for (String line : lines) {
			// -- 이후 부분 제거
			int commentIndex = line.indexOf("--");
			if (commentIndex >= 0) {
				line = line.substring(0, commentIndex);
			}
			result.append(line).append("\n");
		}
		
		return result.toString().trim();
	}

	/**
	 * SQL 실행 핵심 로직
	 * 
	 * @param executeDto SQL 템플릿 실행 DTO
	 * @param sql 실행할 SQL
	 * @return SQL 실행 결과
	 */
	private Map<String, List> executeTemplateSQLCore(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		// SQL 내용을 DTO에 설정
		executeDto.setSqlContent(sql);
		
		// 실행 시작 시간 설정
		executeDto.setStartTime(Instant.now());
		executeDto.setLogNo(0);
		
		Map<String, List> result = new HashMap<>();
		
		String log = "";
		if (executeDto.getLog() != null) {
			
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, String> dataMap = objectMapper.readValue(executeDto.getLog(), HashMap.class);
			
			for (Map.Entry<String, String> entry : dataMap.entrySet()) {
				
				log += "\n" + entry.getKey() + " : " + entry.getValue();
			}
		}
		
		
		try {
			// 로깅 시작
			cLog.log_start(executeDto, log + "\n템플릿 SQL 실행 시작\n"); 
			cLog.logMenuExecutionStart(executeDto);  // 메뉴 실행 시작 로그 저장
			
			// SQL 타입 감지
			SqlType sqlType = detectSqlType(sql);
			
			switch (sqlType) {
				case CALL:
					result = executeCallProcedure(executeDto, sql);
					break;
				case EXECUTE:
					result = executeQuery(executeDto, sql);
					break;
				case UPDATE:
					result = executeUpdate(executeDto, sql);
					break;
				default:
					throw new Exception("지원하지 않는 SQL 타입입니다: " + sqlType);
			}
			
			// 성공 결과 설정
			executeDto.setEndTime(Instant.now());
			executeDto.setResult("Success");
			executeDto.setExecutionTime(Duration.between(executeDto.getStartTime(), executeDto.getEndTime()));
			
			// 성공 로깅
			Duration timeElapsed = Duration.between(executeDto.getStartTime(), executeDto.getEndTime());
			String row = " / rows : " + executeDto.getRows();
			cLog.log_end(executeDto, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
			cLog.log_DB(executeDto);
			cLog.logMenuExecutionEnd(executeDto);  // 메뉴 실행 완료 로그 저장
			
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
			element.add(sql);

			singleList.add(element);
			errorResult.put("rowbody", singleList);
			
			List<Boolean> successList = new ArrayList<>();
			successList.add(false);
			errorResult.put("success", successList);

			// 실패 결과 설정
			executeDto.setEndTime(Instant.now());
			executeDto.setResult("Failed");
			executeDto.setErrorMessage(e1.getMessage());
			executeDto.setExecutionTime(Duration.between(executeDto.getStartTime(), executeDto.getEndTime()));

			// 실패 로깅
			cLog.log_end(executeDto, " sql 실행 종료 : 실패 " + e1.getMessage() + "\n\n");
			cLog.log_DB(executeDto);
			cLog.logMenuExecutionEnd(executeDto);  // 메뉴 실행 완료 로그 저장

			logger.error("SQL 실행 실패 - ConnectionId: {} / templateId: {} / sql: {}", executeDto.getConnectionId(), executeDto.getTemplateId(), sql);
			e1.printStackTrace();

			return errorResult;
			
		} catch (Exception e) {
			// 실패 결과 설정
			executeDto.setEndTime(Instant.now());
			executeDto.setResult("Failed");
			executeDto.setErrorMessage(e.getMessage());
			executeDto.setExecutionTime(Duration.between(executeDto.getStartTime(), executeDto.getEndTime()));
			
			// 실패 로깅
			Duration timeElapsed = Duration.between(executeDto.getStartTime(), executeDto.getEndTime());
			cLog.log_end(executeDto, " sql 실행 종료 : 실패 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
			cLog.log_DB(executeDto);
			cLog.logMenuExecutionEnd(executeDto);  // 메뉴 실행 완료 로그 저장
			
			// 에러 결과 반환
			result = createErrorResult(e.getMessage(), sql);
		}
		
		// 실행 시간 정보를 결과에 추가
		if (result != null) {
			Duration executionTime = Duration.between(executeDto.getStartTime(), executeDto.getEndTime());
			// Map<String, List> 타입이므로 List로 감싸서 추가
			List<Object> executionTimeList = new ArrayList<>();
			executionTimeList.add(executionTime.toMillis());
			result.put("executionTime", executionTimeList);
			
			List<String> executionTimeFormattedList = new ArrayList<>();
			executionTimeFormattedList.add(new DecimalFormat("###,###").format(executionTime.toMillis()) + "ms");
			result.put("executionTimeFormatted", executionTimeFormattedList);
		}
		
		return result;
	}
	
	/**
	 * CALL 프로시저 실행
	 */
	private Map<String, List> executeCallProcedure(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		Connection con = null;
		CallableStatement callStmt = null;
		ResultSet rs = null;
		
		try {
			con = dynamicJdbcManager.getConnection(executeDto.getConnectionId());
			con.setAutoCommit(false);
			
			Map<String, List> result = new HashMap<>();
			
			// CallableStatement 생성 (이미 파라미터가 바인딩된 SQL 사용)
			callStmt = con.prepareCall(sql);
			
			// 프로시저 실행
			callStmt.execute();
			
			// 결과 처리
			rs = callStmt.getResultSet();
			
			if (rs != null) {
				// ResultSet이 있는 경우
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				
				// 컬럼 헤더 생성
				List<Map<String, Object>> rowhead = new ArrayList<>();
				List<Integer> rowlength = new ArrayList<>();
				
				for (int i = 0; i < colCount; i++) {
					Map<String, Object> head = new HashMap<>();
					head.put("title", rsmd.getColumnLabel(i + 1));
					head.put("type", rsmd.getColumnType(i + 1));
					head.put("desc", rsmd.getColumnTypeName(i + 1) + "(" + rsmd.getColumnDisplaySize(i + 1) + ")");
					rowhead.add(head);
					rowlength.add(0);
				}
				
				// 데이터 처리
				List<List<Object>> rowbody = new ArrayList<>();
				while (rs.next()) {
					List<Object> row = new ArrayList<>();
					for (int i = 0; i < colCount; i++) {
						try {
							Object value = rsmd.getColumnTypeName(i + 1).equals("CLOB") ? 
								rs.getString(i + 1) : rs.getObject(i + 1);
							row.add(value);
							
							if (rowlength.get(i) < (value == null ? "" : value.toString()).length()) {
								rowlength.set(i, Math.min(100, (value == null ? "" : value.toString()).length()));
							}
						} catch (Exception e) {
							row.add(null);
						}
					}
					rowbody.add(row);
				}
				
				result.put("rowhead", rowhead);
				result.put("rowbody", rowbody);
				result.put("rowlength", rowlength);
				
			} else {
				// ResultSet이 없는 경우
				List<Map<String, Object>> rowhead = new ArrayList<>();
				List<List<Object>> rowbody = new ArrayList<>();
				List<Object> row = new ArrayList<>();
				
				// 업데이트된 행 수
				Map<String, Object> head = new HashMap<>();
				head.put("title", "Update Rows");
				head.put("type", Types.VARCHAR);
				head.put("desc", "");
				rowhead.add(head);
				
				row.add("" + callStmt.getUpdateCount());
				rowbody.add(row);
				
				result.put("rowhead", rowhead);
				result.put("rowbody", rowbody);
			}
			
			// 실행 결과 설정
			executeDto.setRows(result.get("rowbody") != null ? result.get("rowbody").size() : 0);
			
			return result;
			
		} catch (SQLException e) {
			// SQL 실행 오류 처리
			logger.error("SQL CallableStatement execution error: {}", e.getMessage());
			return createErrorResult("SQL 프로시저 실행 오류: " + e.getMessage(), sql);
			
		} catch (Exception e) {
			// 기타 예외 처리
			logger.error("Unexpected error during callable statement execution: {}", e.getMessage(), e);
			return createErrorResult("예상치 못한 오류: " + e.getMessage(), sql);
			
		} finally {
			if (rs != null) try { rs.close(); } catch (Exception e) {}
			if (callStmt != null) try { callStmt.close(); } catch (Exception e) {}
			if (con != null) {
				try {
					con.commit();
					con.close();
				} catch (Exception e) {
					logger.error("Connection close error: {}", e.getMessage());
				}
			}
		}
	}
	
	/**
	 * SELECT 쿼리 실행
	 */
	private Map<String, List> executeQuery(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			con = dynamicJdbcManager.getConnection(executeDto.getConnectionId());
			con.setAutoCommit(false);
			
			Map<String, List> result = new HashMap<>();
			
			// PreparedStatement 생성 (이미 파라미터가 바인딩된 SQL 사용)
			pstmt = con.prepareStatement(sql);
			
			// 결과 제한 설정
			if (executeDto.getLimit() != null && executeDto.getLimit() > 0) {
				pstmt.setMaxRows(executeDto.getLimit());
			}
			
			// 쿼리 실행
			rs = pstmt.executeQuery();
			
			ResultSetMetaData rsmd = rs.getMetaData();
			int colCount = rsmd.getColumnCount();
			
			// 컬럼 헤더 생성
			List<Map<String, Object>> rowhead = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			
			for (int i = 0; i < colCount; i++) {
				Map<String, Object> head = new HashMap<>();
				head.put("title", rsmd.getColumnLabel(i + 1));
				head.put("type", rsmd.getColumnType(i + 1));
				
				// 컬럼 설명 정보 조회
				String desc = rsmd.getColumnTypeName(i + 1) + "(" + rsmd.getColumnDisplaySize(i + 1) + ")";
				try (ResultSet colRs = con.getMetaData().getColumns(null, rsmd.getSchemaName(i + 1), 
						rsmd.getTableName(i + 1), rsmd.getColumnName(i + 1))) {
					if (colRs.next()) {
						String remarks = colRs.getString("REMARKS");
						if (remarks != null && !remarks.trim().isEmpty()) {
							desc += "\n" + remarks;
						}
					}
				}
				head.put("desc", desc);
				
				rowhead.add(head);
				rowlength.add(0);
			}
			
			result.put("rowhead", rowhead);
			
			// 데이터 처리
			List<List<Object>> rowbody = new ArrayList<>();
			int rowCount = 0;
			
			while (rs.next()) {
				List<Object> row = new ArrayList<>();
				for (int i = 0; i < colCount; i++) {
					try {
						Object value;
						switch (rsmd.getColumnType(i + 1)) {
							case Types.SQLXML:
								value = rs.getSQLXML(i + 1).toString();
								break;
							case Types.DATE:
								value = rs.getDate(i + 1).toString();
								break;
							case Types.BIGINT:
							case Types.DECIMAL:
								value = rs.getBigDecimal(i + 1).toString();
								break;
							case Types.CLOB:
							case Types.TIMESTAMP:
								value = rs.getString(i + 1);
								break;
							default:
								value = rs.getObject(i + 1);
								break;
						}
						
						row.add(value);
						
						// 컬럼 길이 계산
						if (rowlength.get(i) < (value == null ? "" : value.toString()).length()) {
							rowlength.set(i, Math.min(100, (value == null ? "" : value.toString()).length()));
						}
						
					} catch (Exception e) {
						row.add(null);
					}
				}
				rowbody.add(row);
				rowCount++;
			}
			
			result.put("rowbody", rowbody);
			result.put("rowlength", rowlength);
			
			// 실행 결과 설정
			executeDto.setRows(rowCount);
			
			return result;
			
		} finally {
			if (rs != null) try { rs.close(); } catch (Exception e) {}
			if (pstmt != null) try { pstmt.close(); } catch (Exception e) {}
			if (con != null) {
				try {
					con.commit();
					con.close();
				} catch (Exception e) {
					logger.error("Connection close error: {}", e.getMessage());
				}
			}
		}
	}
	
	/**
	 * UPDATE/DELETE/INSERT 쿼리 실행
	 */
	private Map<String, List> executeUpdate(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		Map<String, List> result = new HashMap<>();
		
		// 컬럼 헤더 생성
		List<Map<String, Object>> rowhead = new ArrayList<>();
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Result");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Updated Rows");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Query");
		}});
		result.put("rowhead", rowhead);
		
		// 세미콜론으로 구분된 여러 SQL 처리
		String[] sqlStatements = sql.trim().split(";");
		List<List<Object>> allResults = new ArrayList<>();
		int totalUpdatedRows = 0;
		
		for (String singleSql : sqlStatements) {
			if (singleSql.trim().isEmpty()) {
				continue;
			}
			
			// 단일 SQL 실행
			List<List<Object>> singleResult = executeSingleUpdate(executeDto, singleSql.trim());
			allResults.addAll(singleResult);
			
			// 업데이트된 행 수 누적
			for (List<Object> row : singleResult) {
				if (row.size() > 1 && row.get(1) instanceof String) {
					try {
						totalUpdatedRows += Integer.parseInt((String) row.get(1));
					} catch (NumberFormatException e) {
						// 무시
					}
				}
			}
		}
		
		result.put("rowbody", allResults);
		
		// 실행 결과 설정
		executeDto.setRows(totalUpdatedRows);
		
		return result;
	}
	
	/**
	 * 단일 UPDATE/DELETE/INSERT SQL 실행
	 */
	private List<List<Object>> executeSingleUpdate(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		List<List<Object>> result = new ArrayList<>();
		
		Connection con = null;
		PreparedStatement pstmt = null;
		
		try {
			con = dynamicJdbcManager.getConnection(executeDto.getConnectionId());
			pstmt = con.prepareStatement(sql);
			
			// SQL 실행 (이미 파라미터가 바인딩된 SQL 사용)
			int updatedRows = pstmt.executeUpdate();
			
			// 결과 생성
			List<Object> row = new ArrayList<>();
			row.add("success");
			row.add(Integer.toString(updatedRows));
			row.add(sql);
			result.add(row);
			
		} catch (SQLException e) {
			// SQL 실행 오류 처리
			logger.error("SQL Update execution error: {} {}",executeDto.getTemplateId(), e.getMessage());
			List<Object> errorRow = new ArrayList<>();
			errorRow.add("Error");
			errorRow.add("0");
			errorRow.add(sql);
			result.add(errorRow);
			
		} catch (Exception e) {
			// 기타 예외 처리
			logger.error("Unexpected error during update execution: {}", e.getMessage(), e);
			List<Object> errorRow = new ArrayList<>();
			errorRow.add("Error");
			errorRow.add("0");
			errorRow.add(sql);
			result.add(errorRow);
			
		} finally {
			if (pstmt != null) try { pstmt.close(); } catch (Exception e) {}
			if (con != null) {
				try {
					con.commit();
					con.close();
				} catch (Exception e) {
					logger.error("Connection close error: {}", e.getMessage());
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 에러 결과 생성
	 */
	private Map<String, List> createErrorResult(String errorMessage, String sql) {
		Map<String, List> errorResult = new HashMap<>();
		
		// 컬럼 헤더 생성
		List<Map<String, Object>> rowhead = new ArrayList<>();
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Result");
			put("type", java.sql.Types.VARCHAR);
			put("desc", "실행 결과");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Error Message");
			put("type", java.sql.Types.VARCHAR);
			put("desc", "에러 메시지");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Query");
			put("type", java.sql.Types.VARCHAR);
			put("desc", "실행된 SQL");
		}});
		errorResult.put("rowhead", rowhead);
		
		// 에러 데이터 생성
		List<List<Object>> rowbody = new ArrayList<>();
		List<Object> errorRow = new ArrayList<>();
		errorRow.add("Error");
		errorRow.add(errorMessage != null ? errorMessage : "알 수 없는 오류");
		errorRow.add(sql != null ? sql : "");
		rowbody.add(errorRow);
		errorResult.put("rowbody", rowbody);
		
		// 컬럼 길이 정보 추가
		List<Integer> rowlength = new ArrayList<>();
		rowlength.add(5);  // "Error" 길이
		rowlength.add(Math.min(100, (errorMessage != null ? errorMessage.length() : 0)));  // 에러 메시지 길이
		rowlength.add(Math.min(100, (sql != null ? sql.length() : 0)));  // SQL 길이
		errorResult.put("rowlength", rowlength);
		
		return errorResult;
	}

	/**
	 * 템플릿 기반 SQL 실행
	 * 
	 * @param executeDto SQL 템플릿 실행 DTO
	 * @return SQL 실행 결과
	 */
	public Map<String, List> executeTemplateSQL(SqlTemplateExecuteDto executeDto) throws Exception {
		// 템플릿 SQL 내용 조회
		String sql = getTemplateSqlContent(executeDto);
		
		// 파라미터 바인딩
		String processedSql = bindParameters(sql, executeDto.getParameterList());
		
		// audit 설정 (템플릿에서 가져오거나 DTO에서 설정)
		Boolean audit = executeDto.getAudit();
		if (audit == null) {
			// 템플릿에서 audit 설정 조회
			try {
				Map<String, Object> templateInfo = sqlTemplateService.getSqlTemplateDetail(executeDto.getTemplateId());
				if (templateInfo.get("success").equals(true)) {
					@SuppressWarnings("unchecked")
					Map<String, Object> templateData = (Map<String, Object>) templateInfo.get("data");
					audit = (Boolean) templateData.get("audit");
				}
			} catch (Exception e) {
				logger.warn("템플릿 audit 설정 조회 실패: {}", e.getMessage());
				audit = false;
			}
		}
		executeDto.setAudit(audit != null ? audit : false);
		
		// executeTemplateSQLCore 메서드 사용
		return executeTemplateSQLCore(executeDto, processedSql);
	}
	
	/**
	 * 템플릿 SQL 내용 조회
	 * 
	 * @param executeDto SQL 템플릿 실행 DTO
	 * @return SQL 내용
	 */
	private String getTemplateSqlContent(SqlTemplateExecuteDto executeDto) throws Exception {
		String templateId = executeDto.getTemplateId();
		String connectionId = executeDto.getConnectionId();
		
		// connectionId가 null이면 기본 SQL 내용 조회
		if (connectionId == null || connectionId.trim().isEmpty()) {
			Map<String, Object> defaultSqlContent = sqlContentService.getDefaultSqlContent(templateId);
			if (defaultSqlContent != null) {
				String sql = (String) defaultSqlContent.get("SQL_CONTENT");
				connectionId = (String) defaultSqlContent.get("CONNECTION_ID");
				executeDto.setConnectionId(connectionId);
				return sql;
			} else {
				throw new Exception("템플릿의 기본 SQL 내용을 찾을 수 없습니다: " + templateId);
			}
		} else {
			// CONNECTION_ID 기반 SQL 내용 조회 (복합 키 방식)
			Map<String, Object> sqlContent = sqlContentService.getSqlContentByTemplateAndConnectionId(templateId, connectionId);
			if (sqlContent != null) {
				String sql = (String) sqlContent.get("SQL_CONTENT");
				String contentConnectionId = (String) sqlContent.get("CONNECTION_ID");
				
				return sql;
			} else {
				throw new Exception("템플릿 SQL 내용을 찾을 수 없습니다: templateId=" + templateId + ", connectionId=" + connectionId);
			}
		}
	}
	
	/**
	 * 파라미터 리스트를 SQL에 바인딩
	 * 
	 * @param sql 원본 SQL
	 * @param parameterList 파라미터 리스트 [{"title":"paramName", "value":"paramValue", "type":"STRING"}]
	 * @return 바인딩된 SQL
	 */
	private String bindParameters(String sql, List<Map<String, Object>> parameterList) {
		if (parameterList == null || parameterList.isEmpty()) {
			return sql;
		}
		
		String processedSql = sql;
		for (Map<String, Object> param : parameterList) {
			String title = (String) param.get("title");
			Object value = param.get("value");
			String type = (String) param.get("type");
			
			if (title != null && value != null) {
				// ${paramName} 형태의 파라미터를 실제 값으로 치환
				// 정규식을 사용하여 정확한 매칭 (단어 경계 고려)
				String paramPlaceholder = "\\$\\{" + Pattern.quote(title) + "\\}";
				String paramValue = formatParameterValue(value, type);
				processedSql = processedSql.replaceAll(paramPlaceholder, paramValue);
			}
		}
		
		return processedSql;
	}
	
	/**
	 * 파라미터 값을 SQL에 적합한 형태로 포맷
	 * 템플릿 관리에서 사용되는 5개 타입만 처리
	 */
	private String formatParameterValue(Object value, String type) {
		if (value == null) {
			return "NULL";
		}
		
		switch (type != null ? type.toUpperCase() : "STRING") {
			// 문자열 타입 - 작은따옴표로 감싸기
			case "STRING":
				return "'" + value.toString().replace("'", "''") + "'";
			
			// 숫자 타입 - 따옴표 없이 그대로
			case "NUMBER":
				return value.toString();
			
			// 텍스트 타입 - 작은따옴표로 감싸기 (긴 텍스트용)
			case "TEXT":
				return "'" + value.toString().replace("'", "''") + "'";
			
			// SQL 타입 - SQL 코드 조각 그대로 삽입
			case "SQL":
				return value.toString();
			
			// 로그 타입 - SQL 바인딩에 사용되지 않음 (로깅용)
			case "LOG":
				return "NULL"; // LOG 타입은 SQL 바인딩에 사용되지 않음
			
			// 기본값 - 문자열로 처리
			default:
				return "'" + value.toString().replace("'", "''") + "'";
		}
	}

	/**
	 * 템플릿의 기본 SQL 내용으로 실행
	 * 
	 * @param templateId 템플릿 ID
	 * @param params 파라미터 JSON 문자열
	 * @param limit 실행 제한
	 * @param memberId 사용자 ID
	 * @param ip 사용자 IP
	 * @return SQL 실행 결과
	 */
	public Map<String, List> executeDefaultTemplateSQL(String templateId, String params, 
													  Integer limit, String memberId, String ip) throws Exception {
		// 템플릿의 기본 SQL 내용 조회
		Map<String, Object> defaultSqlContent = sqlContentService.getDefaultSqlContent(templateId);
		
		if (defaultSqlContent == null) {
			throw new Exception("템플릿의 기본 SQL 내용을 찾을 수 없습니다: " + templateId);
		}
		
		// 기본 SQL 내용은 connectionId가 null
		String connectionId = null;
		
		// 무한 재귀 호출 방지를 위해 직접 LogInfoDto를 생성하여 executeSQL 호출
		LogInfoDto data = new LogInfoDto();
		data.setTemplateId(templateId);
		data.setConnectionId(connectionId);
		data.setParams(params);
		data.setLimit(limit != null ? limit : 1000);
		data.setMemberId(memberId);
		data.setIp(ip);
		data.setId(templateId + "_" + System.currentTimeMillis());
		data.setLogNo(0);
		
		return executeSQL(data);
	}

	/**
	 * 템플릿의 SQL 내용 목록 조회
	 * 
	 * @param templateId 템플릿 ID
	 * @return SQL 내용 목록
	 */
	public List<Map<String, Object>> getTemplateSqlContents(String templateId) {
		return sqlContentService.getSqlContentsByTemplate(templateId);
	}


	/**
	 * 템플릿의 특정 연결 ID SQL 내용 조회 (복합 키 방식)
	 * 
	 * @param templateId 템플릿 ID
	 * @param connectionId DB 연결 ID
	 * @return SQL 내용
	 */
	public Map<String, Object> getTemplateSqlContent(String templateId, String connectionId) {
		if (connectionId == null || connectionId.trim().isEmpty()) {
			return sqlContentService.getDefaultSqlContent(templateId);
		}
		return sqlContentService.getSqlContentByTemplateAndConnectionId(templateId, connectionId);
	}
}