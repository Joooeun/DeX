package kr.Windmill.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Struct;
import java.sql.ParameterMetaData;
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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.Windmill.dto.SqlTemplateExecuteDto;
import kr.Windmill.dto.log.LogInfoDto;
import kr.Windmill.util.Common;
import kr.Windmill.util.DynamicJdbcManager;
import kr.Windmill.util.Log;
import kr.Windmill.util.sql.SQLParserUtil;

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

	/**
	 * 연결 ID로 DB 타입을 조회합니다.
	 * @param connectionId 연결 ID
	 * @return DB 타입 (예: "POSTGRESQL", "ORACLE", "DB2" 등), 조회 실패 시 null
	 */
	private String getDbType(String connectionId) {
		if (connectionId == null || connectionId.trim().isEmpty()) {
			return null;
		}
		try {
			String sql = "SELECT DB_TYPE FROM DATABASE_CONNECTION WHERE CONNECTION_ID = ? AND STATUS = 'ACTIVE'";
			return jdbcTemplate.queryForObject(sql, String.class, connectionId);
		} catch (Exception e) {
			logger.debug("DB_TYPE 조회 실패: {} - {}", connectionId, e.getMessage());
			return null;
		}
	}

	/**
	 * 메타데이터 조회를 스킵해야 하는지 판단합니다.
	 * 1. 모니터링 조회인 경우 (skipMetadata 플래그가 true)
	 * 2. PostgreSQL인 경우
	 * @param executeDto SQL 실행 DTO
	 * @return true면 getColumns() 호출을 스킵해야 함
	 */
	private boolean shouldSkipMetadata(SqlTemplateExecuteDto executeDto) {
		if (executeDto == null) {
			return false;
		}
		
		// 모니터링 플래그 확인
		if (executeDto.getSkipMetadata() != null && executeDto.getSkipMetadata()) {
			return true;
		}
		
		// PostgreSQL 체크
		try {
			String dbType = getDbType(executeDto.getConnectionId());
			if (dbType != null && "POSTGRESQL".equalsIgnoreCase(dbType)) {
				return true;
			}
		} catch (Exception e) {
			logger.debug("DB 타입 확인 실패, 기존 동작 유지: {}", e.getMessage());
		}
		
		return false;
	}

	/**
	 * LogInfoDto를 사용하는 경우의 메타데이터 스킵 판단 (레거시 메서드용)
	 * @param connectionId 연결 ID
	 * @return true면 getColumns() 호출을 스킵해야 함
	 */
	private boolean shouldSkipMetadataForLegacy(String connectionId) {
		if (connectionId == null || connectionId.trim().isEmpty()) {
			return false;
		}
		
		// PostgreSQL 체크
		try {
			String dbType = getDbType(connectionId);
			if (dbType != null && "POSTGRESQL".equalsIgnoreCase(dbType)) {
				return true;
			}
		} catch (Exception e) {
			logger.debug("DB 타입 확인 실패, 기존 동작 유지: {}", e.getMessage());
		}
		
		return false;
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

			// 메타데이터 조회 스킵 여부 확인 (PostgreSQL 최적화)
			boolean skipMetadata = shouldSkipMetadataForLegacy(data.getConnectionId());

			for (int index = 0; index < colcnt; index++) {

				Map head = new HashMap();

				String desc = rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")";

				// 메타데이터 조회 스킵하지 않는 경우에만 REMARKS 조회
				if (!skipMetadata) {
					try (ResultSet resultSet = con.getMetaData().getColumns(null, rsmd.getSchemaName(index + 1), rsmd.getTableName(index + 1), rsmd.getColumnName(index + 1))) {
						if (resultSet.next()) {
							String REMARKS = resultSet.getString("REMARKS");
							if (REMARKS != null && !REMARKS.trim().isEmpty()) {
								desc += "\n" + REMARKS;
							}
						}
					} catch (Exception e) {
						logger.debug("컬럼 메타데이터 조회 실패, REMARKS 없이 진행: {}", e.getMessage());
					}
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

		// 컬럼 헤더 생성 (SQLExecute.jsp 방식 참고)
		List<Map<String, Object>> rowhead = new ArrayList<>();
		rowhead.add(new HashMap<String, Object>() {
			{
				put("title", "Result");
				put("type", java.sql.Types.VARCHAR);
				put("desc", "실행 결과");
			}
		});
		rowhead.add(new HashMap<String, Object>() {
			{
				put("title", "Updated Rows");
				put("type", java.sql.Types.INTEGER);
				put("desc", "영향받은 행 수");
			}
		});
		rowhead.add(new HashMap<String, Object>() {
			{
				put("title", "Query");
				put("type", java.sql.Types.VARCHAR);
				put("desc", "실행된 SQL");
			}
		});
		result.put("rowhead", rowhead);

		String sqlOrg = sql.trim();
		String logsqlOrg = data.getLogsql().trim();

		// JSQLParser를 사용하여 여러 SQL 문장을 정확하게 분리
		List<String> sqlStatements;
		List<String> logsqlStatements;
		try {
			sqlStatements = SQLParserUtil.splitSqlStatements(sqlOrg);
			logsqlStatements = SQLParserUtil.splitSqlStatements(logsqlOrg);
		} catch (Exception e) {
			logger.warn("SQL 분리 실패, 기존 방식 사용: {}", e.getMessage());
			// 폴백: 기존 방식으로 분리
			String[] statements = sqlOrg.split(";");
			sqlStatements = new ArrayList<>();
			logsqlStatements = new ArrayList<>();
			String[] logsqls = logsqlOrg.split(";");
			for (int j = 0; j < statements.length; j++) {
				String trimmed = statements[j].trim();
				if (!trimmed.isEmpty()) {
					sqlStatements.add(trimmed);
					if (j < logsqls.length) {
						logsqlStatements.add(logsqls[j].trim() + ";");
					} else {
						logsqlStatements.add("");
					}
				}
			}
		}

		for (int i = 0; i < sqlStatements.size(); i++) {
			String singleSql = sqlStatements.get(i).trim();
			if (singleSql.isEmpty()) {
				continue;
			}

			data.setLogNo(data.getLogNo() + 1);
			sql = singleSql;
			String logsql = i < logsqlStatements.size() ? logsqlStatements.get(i) : "";
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
	 * @throws Exception 
	 */
	public static SqlType detectSqlType(String sql) throws Exception {
		try {
			// JSQLParser를 사용하여 정확한 SQL 타입 감지
			return SQLParserUtil.detectSqlType(sql);
		} catch (Exception e) {
			// JSQLParser 실패 시 기존 방식으로 폴백
			logger.debug("JSQLParser 타입 감지 실패, 기존 방식으로 폴백: {}", e.getMessage());
			return fallbackDetectSqlType(sql);
		}
	}
	
	/**
	 * 기존 방식으로 SQL 타입 감지 (폴백용)
	 */
	private static SqlType fallbackDetectSqlType(String sql) throws Exception {
		switch (firstword(sql)) {
		case "CALL":
		case "BEGIN":
		case "DECLARE":
		case "SET":
		case "EXEC":
		case "DO":			
			return SqlType.CALL;
		case "SELECT":
		case "WITH":
		case "VALUE":
		case "EXPLAIN":
		case "SHOW":
		case "DESC":
		case "PRAGMA":
		case "VALUES":
			return SqlType.EXECUTE;
		case "INSERT":
		case "UPDATE":
		case "DELETE":
		case "MERGE":
		case "TRUNCATE":
			return SqlType.UPDATE;
		default:
			throw new Exception("지원하지 않는 SQL 타입입니다: " + firstword(sql));
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
		Instant start = Instant.now();
		executeDto.setStartTime(start);
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
			

			executeDto.setLogNo(1);
			
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
			}
			

			executeDto.setEndTime(Instant.now());
			executeDto.setExecutionTime(Duration.between(start, executeDto.getEndTime()));
			
			
			if(sqlType != SqlType.UPDATE) {
				// 성공 결과 설정
				executeDto.setResult("Success");
				
				// 성공 로깅
				Duration timeElapsed = Duration.between(executeDto.getStartTime(), executeDto.getEndTime());
				String row = " / rows : " + executeDto.getRows();
				cLog.log_end(executeDto, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
				cLog.log_DB(executeDto);
				
			}
			cLog.logMenuExecutionEnd(executeDto);  // 메뉴 실행 완료 로그 저장
			
			
		} catch (SQLException e1) {
			// SQL 예외 처리
			Map<String, List> errorResult = new HashMap<>();

			if (errorResult.size() == 0) {
				// 컬럼 헤더 생성 (SQLExecute.jsp 방식 참고)
				List<Map<String, Object>> rowhead = new ArrayList<>();
				rowhead.add(new HashMap<String, Object>() {
					{
						put("title", "Result");
						put("type", java.sql.Types.VARCHAR);
						put("desc", "실행 결과");
					}
				});
				rowhead.add(new HashMap<String, Object>() {
					{
						put("title", "Updated Rows");
						put("type", java.sql.Types.INTEGER);
						put("desc", "영향받은 행 수");
					}
				});
				rowhead.add(new HashMap<String, Object>() {
					{
						put("title", "Query");
						put("type", java.sql.Types.VARCHAR);
						put("desc", "실행된 SQL");
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
			executeDto.setExecutionTime(Duration.between(start, executeDto.getEndTime()));
			executeDto.setResult("Failed");
			executeDto.setErrorMessage(e1.getMessage());

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
			executeDto.setExecutionTime(Duration.between(start, executeDto.getEndTime()));
			executeDto.setResult("Failed");
			executeDto.setErrorMessage(e.getMessage());
			
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
			
			List<Integer> outputParamIndexes = new ArrayList<>();
			List<Integer> outputParamTypes = new ArrayList<>();
			List<Integer> rowlength = new ArrayList<>();
			List<Map<String, Object>> rowhead = new ArrayList<>();
			
			// CallableStatement 생성
			callStmt = con.prepareCall(sql);
			
			// 출력 파라미터 등록 (JDBC 메타데이터 기반)
			try {
				ParameterMetaData paramsMeta = callStmt.getParameterMetaData();
				if (paramsMeta != null) {
					int paramCount = paramsMeta.getParameterCount();
					for (int index = 1; index <= paramCount; index++) {
						int mode = paramsMeta.getParameterMode(index);
						if (mode == ParameterMetaData.parameterModeOut || mode == ParameterMetaData.parameterModeInOut) {
							int paramType = paramsMeta.getParameterType(index);
							callStmt.registerOutParameter(index, paramType);
							outputParamIndexes.add(index);
							outputParamTypes.add(paramType);
							
							Map<String, Object> head = new HashMap<>();
							head.put("title", "OUTPUT_" + index);
							head.put("type", paramType);
							head.put("desc", "OUTPUT_PARAMETER");
							rowhead.add(head);
						}
					}
				}
			} catch (SQLException e) {
				logger.warn("프로시저 파라미터 메타데이터 처리 실패: {}", e.getMessage());
			}
			
			result.put("rowhead", rowhead);
			
			// 프로시저 실행
			callStmt.execute();
			
			// 결과 처리
			rs = callStmt.getResultSet();
			
			if (rs != null) {
				// ResultSet이 있는 경우
				ResultSetMetaData rsmd = rs.getMetaData();
				int colcnt = rsmd.getColumnCount();
				
				// ResultSet 컬럼 헤더 추가
				for (int index = 0; index < colcnt; index++) {
					Map<String, Object> head = new HashMap<>();
					head.put("title", rsmd.getColumnLabel(index + 1));
					head.put("type", rsmd.getColumnType(index + 1));
					head.put("desc", rsmd.getColumnTypeName(index + 1) + "(" + rsmd.getColumnDisplaySize(index + 1) + ")");
					rowhead.add(head);
					rowlength.add(0);
				}
				result.put("rowhead", rowhead);
				
				// ResultSet 데이터 처리
				List<List<Object>> rowbody = new ArrayList<>();
				while (rs.next()) {
					List<Object> body = new ArrayList<>();
					for (int index = 0; index < colcnt; index++) {
						try {
							int columnType = rsmd.getColumnType(index + 1);
							Object value;
							if (columnType == Types.CLOB) {
								value = rs.getString(index + 1);
							} else if (columnType == Types.ARRAY) {
								value = normalizeJdbcValue(rs.getArray(index + 1));
							} else {
								value = normalizeJdbcValue(rs.getObject(index + 1));
							}
							body.add(value);
							
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
				// ResultSet이 없는 경우 - 출력 파라미터 값 처리
				List<List<Object>> rowbody = new ArrayList<>();
				List<Object> element = new ArrayList<>();
				
				if (outputParamTypes.size() > 0) {
					// 출력 파라미터 값들 가져오기
					for (int i = 0; i < outputParamTypes.size(); i++) {
						try {
							int paramIndex = outputParamIndexes.get(i);
							int paramType = outputParamTypes.get(i);
							Object value = null;
							switch (paramType) {
								case Types.INTEGER:
									value = callStmt.getInt(paramIndex);
									break;
								case Types.VARCHAR:
								case Types.CHAR:
									value = callStmt.getString(paramIndex);
									break;
								case Types.DECIMAL:
									value = callStmt.getBigDecimal(paramIndex);
									break;
								case Types.BIGINT:
									value = callStmt.getLong(paramIndex);
									break;
								case Types.SMALLINT:
									value = callStmt.getShort(paramIndex);
									break;
								default:
									value = callStmt.getObject(paramIndex);
									break;
							}
							element.add(normalizeJdbcValue(value));
						} catch (SQLException e) {
							logger.warn("출력 파라미터 값 가져오기 실패: index={}, error={}", outputParamIndexes.get(i), e.getMessage());
							element.add(null);
						}
					}
					
					rowlength.add(1);
					rowlength.add(outputParamTypes.size());
				} else {
					// 출력 파라미터가 없는 경우
					Map<String, Object> head = new HashMap<>();
					head.put("title", "Update Rows");
					head.put("type", Types.VARCHAR);
					head.put("desc", "");
					rowhead.add(head);
					result.put("rowhead", rowhead);
					
					element.add("" + callStmt.getUpdateCount());
					rowlength.add(callStmt.getUpdateCount());
					rowlength.add(callStmt.getUpdateCount());
				}
				
				rowbody.add(element);
				result.put("rowbody", rowbody);
				result.put("rowlength", rowlength);
			}
			
			// 실행 결과 설정
			executeDto.setRows(result.get("rowbody") != null ? result.get("rowbody").size() : 0);
			
			return result;
			
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
			
			// 메타데이터 조회 스킵 여부 확인 (모니터링/PostgreSQL 최적화)
			boolean skipMetadata = shouldSkipMetadata(executeDto);
			
			for (int i = 0; i < colCount; i++) {
				Map<String, Object> head = new HashMap<>();
				head.put("title", rsmd.getColumnLabel(i + 1));
				head.put("type", rsmd.getColumnType(i + 1));
				
				// 컬럼 설명 정보 조회
				String desc = rsmd.getColumnTypeName(i + 1) + "(" + rsmd.getColumnDisplaySize(i + 1) + ")";
				
				// 메타데이터 조회 스킵하지 않는 경우에만 REMARKS 조회
				if (!skipMetadata) {
					try (ResultSet colRs = con.getMetaData().getColumns(null, rsmd.getSchemaName(i + 1), 
							rsmd.getTableName(i + 1), rsmd.getColumnName(i + 1))) {
						if (colRs.next()) {
							String remarks = colRs.getString("REMARKS");
							if (remarks != null && !remarks.trim().isEmpty()) {
								desc += "\n" + remarks;
							}
						}
					} catch (Exception e) {
						logger.debug("컬럼 메타데이터 조회 실패, REMARKS 없이 진행: {}", e.getMessage());
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
						case Types.ARRAY:
							value = normalizeJdbcValue(rs.getArray(i + 1));
							break;
							case Types.CLOB:
							case Types.TIMESTAMP:
								value = rs.getString(i + 1);
								break;
							default:
							value = normalizeJdbcValue(rs.getObject(i + 1));
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
		
		// 컬럼 헤더 생성 (SQLExecute.jsp 방식 참고)
		List<Map<String, Object>> rowhead = new ArrayList<>();
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Result");
			put("type", java.sql.Types.VARCHAR);
			put("desc", "실행 결과");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Updated Rows");
			put("type", java.sql.Types.INTEGER);
			put("desc", "영향받은 행 수");
		}});
		rowhead.add(new HashMap<String, Object>() {{
			put("title", "Query");
			put("type", java.sql.Types.VARCHAR);
			put("desc", "실행된 SQL");
		}});
		result.put("rowhead", rowhead);
		
		// JSQLParser를 사용하여 여러 SQL 문장을 정확하게 분리
		// 문자열 내부나 주석 내부의 세미콜론은 무시됨
		List<String> sqlStatements;
		try {
			sqlStatements = SQLParserUtil.splitSqlStatements(sql);
		} catch (Exception e) {
			logger.warn("SQL 분리 실패, 기존 방식 사용: {}", e.getMessage());
			// 폴백: 기존 방식으로 분리
			String[] statements = sql.trim().split(";");
			sqlStatements = new ArrayList<>();
			for (String stmt : statements) {
				String trimmed = stmt.trim();
				if (!trimmed.isEmpty()) {
					sqlStatements.add(trimmed);
				}
			}
		}

		int totalRows = 0;
		
		try {
			
			for (int i = 0; i < sqlStatements.size(); i++) {
				String singleSql = sqlStatements.get(i).trim();
				if (singleSql.isEmpty()) {
					continue;
				}
				
				// 각 쿼리마다 개별 로그 번호 증가
				executeDto.setLogNo(i+1);
				
				executeDto.setSqlContent(singleSql);
				
				Instant singleStart = Instant.now();
				
				// 단일 SQL 실행
				List<List<Object>> singleResult = executeSingleUpdate(executeDto, executeDto.getSqlContent());
				
				// 결과를 전체 결과에 추가
				List<List<Object>> allResults = new ArrayList<>();
				if (result.get("rowbody") != null) {
					allResults.addAll(result.get("rowbody"));
				}
				allResults.addAll(singleResult);
				result.put("rowbody", allResults);
				
				// 각 쿼리마다 개별 로그 기록
				Duration timeElapsed = Duration.between(singleStart, Instant.now());
				executeDto.setResult("Success");
				executeDto.setExecutionTime(timeElapsed);
				
				if (singleResult.size() > 0 && singleResult.get(0).size() > 1) {
					int updatedRows = Integer.parseInt(singleResult.get(0).get(1).toString());
					executeDto.setRows(updatedRows);
					totalRows += updatedRows;
					
					String row = " / " + detectSqlType(executeDto.getSqlContent()) + " rows : " + updatedRows;
					cLog.log_end(executeDto, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
					cLog.log_DB(executeDto);
				}
			}

			
		} catch (Exception e) {

			List<List<Object>> singleList = new ArrayList<List<Object>>();

			List<Object> element = new ArrayList<Object>();
			element.add(e.toString());
			element.add("0");
			element.add(executeDto.getSqlContent());
			
			
			singleList.add(element);
			
			
			// 결과를 전체 결과에 추가
			List<List<Object>> allResults = new ArrayList<>();
			if (result.get("rowbody") != null) {
				allResults.addAll(result.get("rowbody"));
			}
			allResults.addAll(singleList);
			result.put("rowbody", allResults);
			
			List<Boolean> successList = new ArrayList<>();
			successList.add(false);
			result.put("success", successList);

			// 실패 결과 설정
			executeDto.setEndTime(Instant.now());
			executeDto.setResult("Failed");
			executeDto.setErrorMessage(e.getMessage());
			executeDto.setExecutionTime(Duration.between(executeDto.getStartTime(), executeDto.getEndTime()));

			// 실패 로깅
			cLog.log_end(executeDto, " sql 실행 종료 : 실패 " + e.getMessage() + "\n\n");
			cLog.log_DB(executeDto);
			cLog.logMenuExecutionEnd(executeDto);  // 메뉴 실행 완료 로그 저장

			logger.error("SQL 실행 실패 - ConnectionId: {} / templateName: {} / sql: {}", executeDto.getConnectionId(), executeDto.getTemplateName(), executeDto.getSqlContent());

			return result;
		}
		executeDto.setRows(totalRows);
		return result;
	}

	private Object normalizeJdbcValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof java.sql.Array) {
			try {
				Object arrayValue = ((java.sql.Array) value).getArray();
				return normalizeJdbcArray(arrayValue);
			} catch (SQLException e) {
				return value.toString();
			}
		}
		if (value instanceof Struct) {
			return value.toString();
		}
		return value;
	}

	private List<Object> normalizeJdbcArray(Object arrayValue) {
		if (arrayValue == null) {
			return null;
		}
		if (arrayValue instanceof Object[]) {
			return Arrays.asList((Object[]) arrayValue);
		}
		int length = java.lang.reflect.Array.getLength(arrayValue);
		List<Object> result = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			result.add(java.lang.reflect.Array.get(arrayValue, i));
		}
		return result;
	}
	
	/**
	 * 단일 UPDATE/DELETE/INSERT SQL 실행
	 * @throws Exception 
	 */
	private List<List<Object>> executeSingleUpdate(SqlTemplateExecuteDto executeDto, String sql) throws Exception {
		List<List<Object>> result = new ArrayList<>();
		
		Connection con = null;
		PreparedStatement pstmt = null;
		executeDto.setSqlContent(sql);
		Instant singleStart = Instant.now();
		
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
			
			// 성공 결과 설정
			executeDto.setEndTime(Instant.now());
			executeDto.setResult("Success");
			executeDto.setExecutionTime(Duration.between(executeDto.getStartTime(), executeDto.getEndTime()));	
			
		}  finally {
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
			// 템플릿에서 audit 설정 조회 (실행 시에는 이미 권한 확인이 완료된 상태이므로 null 전달)
			try {
				Map<String, Object> templateInfo = sqlTemplateService.getSqlTemplateDetail(executeDto.getTemplateId(), null);
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
		try {
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
					// Matcher.quoteReplacement()를 사용하여 $, \ 등의 특수문자를 리터럴로 처리
					processedSql = processedSql.replaceAll(paramPlaceholder, Matcher.quoteReplacement(paramValue));
				}
			}

			return processedSql;
			
		} catch (Exception e) {
			throw new RuntimeException("파라미터 파싱 중 에러가 발생했습니다. sql을 확인해 주세요.", e);
		}
		
		
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
	
	/**
	 * SQL에서 프로시저명 추출
	 * 
	 * @param sql SQL 문
	 * @return 프로시저명
	 */
	private String extractProcedureName(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return null;
		}
		
		// CALL SP_NAME(...) 형태에서 프로시저명 추출
		String trimmedSql = sql.trim().toUpperCase();
		if (trimmedSql.startsWith("CALL ")) {
			int startIndex = 5; // "CALL ".length()
			int endIndex = trimmedSql.indexOf('(');
			if (endIndex > startIndex) {
				return trimmedSql.substring(startIndex, endIndex).trim();
			}
		}
		
		return null;
	}
	
	/**
	 * 메타데이터를 통한 동적 출력 파라미터 등록
	 * 
	 * @param callStmt CallableStatement
	 * @param sql SQL 문
	 * @throws SQLException
	 */
	private void registerOutParametersDynamic(CallableStatement callStmt, String sql) throws SQLException {
		String procedureName = extractProcedureName(sql);
		if (procedureName == null) {
			return;
		}
		
		try {
			DatabaseMetaData metaData = callStmt.getConnection().getMetaData();
			
			// 프로시저 파라미터 정보 조회
			try (ResultSet rs = metaData.getProcedureColumns(null, null, procedureName, null)) {
				int paramIndex = 1;
				while (rs.next()) {
					String paramName = rs.getString("COLUMN_NAME");
					int paramType = rs.getInt("DATA_TYPE");
					int paramMode = rs.getInt("COLUMN_TYPE");
					
					logger.debug("파라미터 정보: index={}, name={}, type={}, mode={}", paramIndex, paramName, paramType, paramMode);
					
					// IBM DB2 COLUMN_TYPE 값: 1=IN, 2=INOUT, 4=OUT
					// OUT(4) 또는 INOUT(2) 파라미터인 경우 등록
					if (paramMode == 4 || paramMode == 2) {
						try {
							callStmt.registerOutParameter(paramIndex, paramType);
							logger.debug("OUT 파라미터 등록 성공: index={}, type={}, name={}, mode={}", paramIndex, paramType, paramName, paramMode);
						} catch (SQLException e) {
							logger.warn("OUT 파라미터 등록 실패: index={}, type={}, name={}, mode={}, error={}", 
								paramIndex, paramType, paramName, paramMode, e.getMessage());
						}
						
						paramIndex++;
					}
					
				}
			}
		} catch (SQLException e) {
			logger.warn("메타데이터 조회 실패: procedure={}, error={}", procedureName, e.getMessage());
		}
	}
}