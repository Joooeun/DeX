package kr.Windmill.util.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.truncate.Truncate;

import java.util.ArrayList;
import java.util.List;

import kr.Windmill.service.SQLExecuteService.SqlType;

/**
 * JSQLParser를 사용한 SQL 파싱 유틸리티
 * 
 * @author Windmill
 */
public class SQLParserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLParserUtil.class);
    
    /**
     * SQL 문법 검증
     * 
     * @param sql 검증할 SQL
     * @return 문법이 올바르면 true, 아니면 false
     */
    public static boolean isValidSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (JSQLParserException e) {
            logger.debug("SQL 파싱 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * SQL 타입 감지 (JSQLParser 사용)
     * 
     * @param sql 감지할 SQL
     * @return SQL 타입 (EXECUTE, UPDATE, CALL)
     * @throws Exception 파싱 실패 또는 알 수 없는 타입인 경우
     */
    public static SqlType detectSqlType(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new Exception("SQL이 비어있습니다.");
        }
        
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            
            // JSQLParser 파싱 성공했더라도, fallbackDetectSqlType에 정의된 타입인지 확인
            // 첫 번째 단어를 추출하여 검증
            String firstWord = firstword(sql);
            
            // fallbackDetectSqlType에 정의된 타입인지 확인
            if (isSupportedSqlType(firstWord)) {
                // 지원하는 타입인 경우 Statement 타입에 따라 반환
                if (statement instanceof Select) {
                    return SqlType.EXECUTE;
                } else if (statement instanceof Insert) {
                    return SqlType.UPDATE;
                } else if (statement instanceof Update) {
                    return SqlType.UPDATE;
                } else if (statement instanceof Delete) {
                    return SqlType.UPDATE;
                } else if (statement instanceof Truncate) {
                    return SqlType.UPDATE;
                } else if (statement instanceof Execute) {
                    // EXEC, EXECUTE 등의 실행문
                    return SqlType.CALL;
                } else {
                    // Statement 타입은 지원하지만 첫 단어로 재확인
                    return fallbackDetectSqlType(sql);
                }
            } else {
                // fallbackDetectSqlType에 정의되지 않은 타입인 경우
                logger.warn("지원하지 않는 SQL 타입이 감지되었습니다: {}", firstWord);
                throw new Exception("지원하지 않는 SQL 타입입니다: " + firstWord);
            }
            
        } catch (JSQLParserException e) {
            // 파싱 실패 시 기존 방식으로 폴백
            logger.debug("JSQLParser 파싱 실패, 기존 방식으로 폴백: {}", e.getMessage());
            return fallbackDetectSqlType(sql);
        }
    }
    
    /**
     * fallbackDetectSqlType에 정의된 타입인지 확인
     * 
     * @param firstWord 첫 번째 단어
     * @return 지원하는 타입이면 true
     */
    private static boolean isSupportedSqlType(String firstWord) {
        switch (firstWord) {
            case "CALL":
            case "BEGIN":
            case "DECLARE":
            case "SET":
            case "EXEC":
            case "DO":
            case "SELECT":
            case "WITH":
            case "VALUE":
            case "EXPLAIN":
            case "SHOW":
            case "DESC":
            case "PRAGMA":
            case "VALUES":
            case "INSERT":
            case "UPDATE":
            case "DELETE":
            case "MERGE":
            case "TRUNCATE":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 기존 방식으로 폴백 (호환성 유지)
     * SQLExecuteService의 기존 로직을 재사용
     * 
     * @param sql 감지할 SQL
     * @return SQL 타입
     * @throws Exception 지원하지 않는 타입인 경우
     */
    private static SqlType fallbackDetectSqlType(String sql) throws Exception {
        String firstWord = firstword(sql);
        
        switch (firstWord) {
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
                throw new Exception("지원하지 않는 SQL 타입입니다: " + firstWord);
        }
    }
    
    /**
     * SQL에서 첫 번째 단어 추출 (기존 로직 재사용)
     * 
     * @param sql SQL 문장
     * @return 첫 번째 단어 (대문자)
     */
    private static String firstword(String sql) {
        String[] words = removeComments(sql).trim().split("\\s+");
        return words.length > 0 ? words[0].toUpperCase() : "";
    }
    
    /**
     * SQL 주석 제거 (기존 로직 재사용)
     * 
     * @param sql 원본 SQL
     * @return 주석이 제거된 SQL
     */
    private static String removeComments(String sql) {
        if (sql == null) return "";
        
        // 블록 주석 제거 (/* ... */)
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        
        // 한 줄 주석 제거 (-- ...)
        String[] lines = sql.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            int commentIndex = line.indexOf("--");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }
            result.append(line).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * 여러 SQL 문장을 정확하게 분리
     * 문자열 내부나 주석 내부의 세미콜론은 무시합니다.
     * 
     * @param sql 여러 SQL 문장이 세미콜론으로 구분된 문자열
     * @return 분리된 각 SQL 문장 리스트
     * @throws Exception 파싱 실패 시
     */
    public static List<String> splitSqlStatements(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // JSQLParser를 사용하여 여러 Statement 분리
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            
            // 각 Statement를 다시 문자열로 변환
            List<String> result = new ArrayList<>();
            for (Statement stmt : statements.getStatements()) {
                String stmtStr = stmt.toString().trim();
                if (!stmtStr.isEmpty()) {
                    result.add(stmtStr);
                }
            }
            
            return result;
            
        } catch (JSQLParserException e) {
            // 파싱 실패 시 기존 방식으로 폴백
            logger.debug("JSQLParser를 사용한 SQL 분리 실패, 기존 방식으로 폴백: {}", e.getMessage());
            return fallbackSplitSqlStatements(sql);
        } catch (Exception e) {
            logger.debug("SQL 분리 중 오류 발생, 기존 방식으로 폴백: {}", e.getMessage());
            return fallbackSplitSqlStatements(sql);
        }
    }
    
    /**
     * 기존 방식으로 SQL 문장 분리 (폴백용)
     * 
     * @param sql 원본 SQL
     * @return 분리된 SQL 문장 리스트
     */
    private static List<String> fallbackSplitSqlStatements(String sql) {
        List<String> result = new ArrayList<>();
        String[] statements = sql.trim().split(";");
        
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        
        return result;
    }
    
    /**
     * SQL 파싱 에러 정보 추출 (향상된 에러 메시지용)
     * 
     * @param sql 원본 SQL
     * @param e 파싱 예외
     * @return 에러 메시지
     */
    public static String extractParseError(String sql, JSQLParserException e) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("SQL 파싱 오류: ").append(e.getMessage());
        
        // 가능하면 더 구체적인 정보 추가
        if (e.getCause() != null) {
            errorMsg.append(" (").append(e.getCause().getMessage()).append(")");
        }
        
        return errorMsg.toString();
    }
}

