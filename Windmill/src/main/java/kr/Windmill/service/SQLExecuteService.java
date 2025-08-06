package kr.Windmill.service;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.service.LogInfoDTO;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Service
public class SQLExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(SQLExecuteService.class);
    private Common com = new Common();
    private Log cLog = new Log();

    public enum SqlType {
        CALL, EXECUTE, UPDATE
    }

    /**
     * SQL 실행 공통 메서드
     * @param data LogInfoDTO - SQL 실행 정보
     * @param memberId 사용자 ID
     * @param ip 사용자 IP
     * @return SQL 실행 결과
     */
    public Map<String, List> executeSQL(LogInfoDTO data) throws Exception {
        data.setStart(Instant.now());
        ConnectionDTO connection = com.getConnection(data.getConnection());
        Properties prop = connection.getProp();

        Class.forName(connection.getDriver());
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
                result = com.callprocedure(sql, connection.getDbtype(), connection.getJdbc(), prop, mapping);

                data.setRows(Integer.parseInt(result.get("rowlength").get(data.isAudit() ? 1 : 0).toString()));
                data.setEnd(Instant.now());
                data.setResult("Success");
                Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

                row = " / rows : " + data.getRows();
                cLog.log_end(data, " sql 실행 종료 : 성공" + row + " / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
                cLog.log_DB(data);

            } else if (detectSqlType(sql) == SqlType.EXECUTE) {
                data.setLogNo(data.getLogNo() + 1);
                result = com.excutequery(sql, connection.getDbtype(), connection.getJdbc(), prop, data.getLimit(), mapping);
                data.setRows(result.get("rowbody").size() - 1);
                data.setEnd(Instant.now());
                data.setResult("Success");
                Duration timeElapsed = Duration.between(data.getStart(), data.getEnd());

                cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
                cLog.log_DB(data);

            } else {
                // UPDATE 타입 처리
                result = processUpdateSQL(data, connection, prop, mapping, sql);
            }

        } catch (SQLException e1) {
            // SQL 예외 처리
            Map<String, List> errorResult = new HashMap<>();
            
            if (errorResult.size() == 0) {
                List<Map<String, String>> rowhead = new ArrayList<>();
                rowhead.add(new HashMap<String, String>() {{ put("title", "Result"); }});
                rowhead.add(new HashMap<String, String>() {{ put("title", "Updated Rows"); }});
                rowhead.add(new HashMap<String, String>() {{ put("title", "Query"); }});
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
    private List<Map<String, String>> processParameterMapping(LogInfoDTO data, String sql) {
        List<Map<String, String>> mapping = new ArrayList<Map<String, String>>();

        String patternString = "(?<!:):(";
        for (int i = 0; i < data.getParamList().size(); i++) {
            if (data.getParamList().get(i).get("type").equals("string") || 
                data.getParamList().get(i).get("type").equals("text") || 
                data.getParamList().get(i).get("type").equals("varchar")) {
                if (!patternString.equals("(?<!:):("))
                    patternString += "|";
                patternString += data.getParamList().get(i).get("title");
            } else {
                sql = sql.replaceAll(":" + data.getParamList().get(i).get("title"), 
                                   data.getParamList().get(i).get("value").toString());
            }
        }
        patternString += ")";
        
        if (!patternString.equals("(?<!:):()")) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(sql);
            int cnt = 0;
            while (matcher.find()) {
                Map<String, String> temp = new HashMap<>();
                temp.put("value", data.getParamList().stream()
                    .filter(p -> p.get("title").equals(matcher.group(1)))
                    .findFirst().get().get("value").toString());
                temp.put("type", data.getParamList().stream()
                    .filter(p -> p.get("title").equals(matcher.group(1)))
                    .findFirst().get().get("type").toString());

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
     * UPDATE SQL 처리
     */
    private Map<String, List> processUpdateSQL(LogInfoDTO data, ConnectionDTO connection, Properties prop, 
                                              List<Map<String, String>> mapping, String sql) throws Exception {
        Map<String, List> result = new HashMap<>();
        
        List<Map<String, String>> rowhead = new ArrayList<>();
        rowhead.add(new HashMap<String, String>() {{ put("title", "Result"); }});
        rowhead.add(new HashMap<String, String>() {{ put("title", "Updated Rows"); }});
        rowhead.add(new HashMap<String, String>() {{ put("title", "Query"); }});
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
            singleList.addAll(com.updatequery(sql.trim(), connection.getDbtype(), connection.getJdbc(), prop, null, mapping));
            result.put("rowbody", singleList);

            data.setRows(singleList.size());
            data.setEnd(Instant.now());
            data.setResult("Success");
            Duration timeElapsed = Duration.between(singleStart, data.getEnd());

            cLog.log_end(data, " sql 실행 종료 : 성공 / 소요시간 : " + new DecimalFormat("###,###").format(timeElapsed.toMillis()) + "\n");
            cLog.log_DB(data);
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