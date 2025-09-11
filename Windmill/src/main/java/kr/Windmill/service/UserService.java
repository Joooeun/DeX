package kr.Windmill.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.Windmill.util.Crypto;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 사용자 목록 조회 (페이징 포함)
    public Map<String, Object> getUserList(String searchKeyword, String groupFilter, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 전체 개수 조회
        StringBuilder countSqlBuilder = new StringBuilder();
        countSqlBuilder.append("SELECT COUNT(*) FROM USERS u ");
        countSqlBuilder.append("LEFT JOIN USER_GROUP_MAPPING ugm ON u.USER_ID = ugm.USER_ID ");
        countSqlBuilder.append("LEFT JOIN USER_GROUPS ug ON ugm.GROUP_ID = ug.GROUP_ID ");
        
        List<Object> countParams = new ArrayList<>();
        List<String> whereConditions = new ArrayList<>();
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            whereConditions.add("(u.USER_ID LIKE ? OR u.USER_NAME LIKE ?)");
            String likePattern = "%" + searchKeyword.trim() + "%";
            countParams.add(likePattern);
            countParams.add(likePattern);
        }
        
        if (groupFilter != null && !groupFilter.trim().isEmpty()) {
            whereConditions.add("ugm.GROUP_ID = ?");
            countParams.add(groupFilter);
        }
        
        if (!whereConditions.isEmpty()) {
            countSqlBuilder.append("WHERE ").append(String.join(" AND ", whereConditions));
        }
        
        int totalCount;
        if (countParams.isEmpty()) {
            totalCount = jdbcTemplate.queryForObject(countSqlBuilder.toString(), Integer.class);
        } else {
            totalCount = jdbcTemplate.queryForObject(countSqlBuilder.toString(), countParams.toArray(), Integer.class);
        }
        
        // 페이징된 데이터 조회 - DB2 표준 문법 사용
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM (");
        sqlBuilder.append("SELECT ROW_NUMBER() OVER (ORDER BY u.CREATED_TIMESTAMP DESC) AS RN, ");
        sqlBuilder.append("u.USER_ID, u.USER_NAME, u.STATUS, u.IP_RESTRICTION, u.LAST_LOGIN_TIMESTAMP, u.LOGIN_FAIL_COUNT, u.CREATED_TIMESTAMP, ");
        sqlBuilder.append("ug.GROUP_NAME ");
        sqlBuilder.append("FROM USERS u ");
        sqlBuilder.append("LEFT JOIN USER_GROUP_MAPPING ugm ON u.USER_ID = ugm.USER_ID ");
        sqlBuilder.append("LEFT JOIN USER_GROUPS ug ON ugm.GROUP_ID = ug.GROUP_ID ");
        
        List<Object> params = new ArrayList<>();
        List<String> dataWhereConditions = new ArrayList<>();
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            dataWhereConditions.add("(u.USER_ID LIKE ? OR u.USER_NAME LIKE ?)");
            String likePattern = "%" + searchKeyword.trim() + "%";
            params.add(likePattern);
            params.add(likePattern);
        }
        
        if (groupFilter != null && !groupFilter.trim().isEmpty()) {
            dataWhereConditions.add("ugm.GROUP_ID = ?");
            params.add(groupFilter);
        }
        
        if (!dataWhereConditions.isEmpty()) {
            sqlBuilder.append("WHERE ").append(String.join(" AND ", dataWhereConditions));
        }
        
        sqlBuilder.append(") AS PAGED_DATA ");
        sqlBuilder.append("WHERE RN BETWEEN ? AND ?");
        
        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;
        params.add(startRow);
        params.add(endRow);
        
        List<Map<String, Object>> userList = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
        
        // 페이징 정보 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        result.put("userList", userList);
        result.put("totalCount", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        return result;
    }
    
    // 기존 메서드 호환성을 위한 오버로드
    public Map<String, Object> getUserList(String searchKeyword, int page, int pageSize) {
        return getUserList(searchKeyword, null, page, pageSize);
    }
    
    public List<Map<String, Object>> getUserList(String searchKeyword) {
        Map<String, Object> result = getUserList(searchKeyword, null, 1, 5);
        return (List<Map<String, Object>>) result.get("userList");
    }
    
    // 사용자 상세 조회
    public Map<String, Object> getUserDetail(String userId) {
        String sql = "SELECT * FROM USERS WHERE USER_ID = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
        return result.isEmpty() ? null : result.get(0);
    }
    
    // 사용자 생성
    @Transactional
    public boolean createUser(Map<String, Object> userData) {
        try {
            // 비밀번호 암호화
            String plainPassword = (String) userData.get("password");
            String encryptedPassword = Crypto.crypt(plainPassword);
            
            String sql = "INSERT INTO USERS (USER_ID, USER_NAME, PASSWORD, STATUS, IP_RESTRICTION, CREATED_BY) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                userData.get("userId"),
                userData.get("userName"),
                encryptedPassword,  // 암호화된 비밀번호 저장
                userData.get("status"),
                userData.get("ipRestriction"),
                userData.get("createdBy")
            );
            return true;
        } catch (Exception e) {
            logger.error("사용자 생성 실패", e);
            return false;
        }
    }
    
    // 사용자 수정
    @Transactional
    public boolean updateUser(String userId, Map<String, Object> userData) {
        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("UPDATE USERS SET USER_NAME = ?, STATUS = ?, IP_RESTRICTION = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP");
            
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(userData.get("userName"));
            params.add(userData.get("status"));
            params.add(userData.get("ipRestriction"));
            params.add(userData.get("modifiedBy"));
            
            // 비밀번호가 제공된 경우 관리자가 입력한 비밀번호를 임시 비밀번호로 설정
            String password = (String) userData.get("password");
            if (password != null && !password.trim().isEmpty()) {
                // 관리자가 입력한 비밀번호를 임시 비밀번호로 설정
                String encryptedPassword = Crypto.crypt(password);
                sqlBuilder.append(", PASSWORD = ?, TEMP_PASSWORD = ?, PASSWORD_CHANGE_DATE = CURRENT DATE");
                params.add(encryptedPassword);
                params.add(encryptedPassword); // TEMP_PASSWORD에도 동일한 값 저장
                
                // 감사 로그 기록 (비밀번호가 임시 비밀번호로 변경됨)
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, STATUS, ERROR_MESSAGE) " +
                                "VALUES (?, 'CHANGE_PASSWORD', 'USER', ?, 'SUCCESS', ?)";
                jdbcTemplate.update(auditSql, userId, userId, 
                    "비밀번호가 임시 비밀번호로 변경됨 (관리자: " + userData.get("modifiedBy") + ")");
            }
            
            sqlBuilder.append(" WHERE USER_ID = ?");
            params.add(userId);
            
            jdbcTemplate.update(sqlBuilder.toString(), params.toArray());
            return true;
        } catch (Exception e) {
            logger.error("사용자 수정 실패: {}", userId, e);
            return false;
        }
    }
    
    // 사용자 삭제
    @Transactional
    public boolean deleteUser(String userId) {
        try {
            String sql = "DELETE FROM USERS WHERE USER_ID = ?";
            jdbcTemplate.update(sql, userId);
            return true;
        } catch (Exception e) {
            logger.error("사용자 삭제 실패: {}", userId, e);
            return false;
        }
    }
    
    // 로그인 처리
    @Transactional
    public Map<String, Object> login(String userId, String password, String ipAddress, String userAgent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 사용자 존재 여부 및 상태 확인
            String checkSql = "SELECT STATUS, LOGIN_FAIL_COUNT FROM USERS WHERE USER_ID = ?";
            List<Map<String, Object>> userInfo = jdbcTemplate.queryForList(checkSql, userId);
            
            if (userInfo.isEmpty()) {
                // 존재하지 않는 사용자 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', '계정정보가 일치하지 않습니다')";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent);
                
                result.put("success", false);
                result.put("message", "계정정보가 일치하지 않습니다.");
                return result;
            }
            
            Map<String, Object> user = userInfo.get(0);
            String status = (String) user.get("STATUS");
            Integer failCount = (Integer) user.get("LOGIN_FAIL_COUNT");
            
            // 비밀번호 확인 (PASSWORD 또는 TEMP_PASSWORD와 비교)
            String passwordSql = "SELECT PASSWORD, TEMP_PASSWORD FROM USERS WHERE USER_ID = ?";
            List<Map<String, Object>> passwordResult = jdbcTemplate.queryForList(passwordSql, userId);
            
            if (passwordResult.isEmpty()) {
                result.put("success", false);
                result.put("message", "계정정보가 일치하지 않습니다.");
                return result;
            }
            
            String storedPassword = (String) passwordResult.get(0).get("PASSWORD");
            String tempPassword = (String) passwordResult.get(0).get("TEMP_PASSWORD");
            String encryptedPassword = Crypto.crypt(password);
            
            // 비밀번호가 틀린 경우 (PASSWORD 또는 TEMP_PASSWORD와 비교)
            boolean passwordMatch = storedPassword.equals(encryptedPassword) || 
                                  (tempPassword != null && tempPassword.equals(encryptedPassword));
            
            if (!passwordMatch) {
                // 로그인 실패 횟수 증가
                String failSql = "UPDATE USERS SET LOGIN_FAIL_COUNT = LOGIN_FAIL_COUNT + 1 WHERE USER_ID = ?";
                jdbcTemplate.update(failSql, userId);
                
                // 현재 실패 횟수 확인
                String currentFailSql = "SELECT LOGIN_FAIL_COUNT FROM USERS WHERE USER_ID = ?";
                Integer currentFailCount = jdbcTemplate.queryForObject(currentFailSql, Integer.class, userId);
                
                String errorMessage = "계정정보가 일치하지 않습니다.";
                
                // 5번 이상 실패하면 계정 잠금
                if (currentFailCount != null && currentFailCount >= 5) {
                    String lockSql = "UPDATE USERS SET STATUS = 'LOCKED' WHERE USER_ID = ?";
                    jdbcTemplate.update(lockSql, userId);
                    errorMessage = "로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의하세요.";
                }
                
                // 비밀번호 불일치 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, errorMessage);
                
                result.put("success", false);
                result.put("message", errorMessage);
                return result;
            }
            
            // 비밀번호가 맞은 경우 - 계정 상태 확인
            if (!"ACTIVE".equals(status)) {
                String statusMessage = "LOCKED".equals(status) ? "계정이 잠겨있습니다. 관리자에게 문의하세요." : "비활성화된 계정입니다.";
                
                // 계정 상태 문제 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, statusMessage);
                
                result.put("success", false);
                result.put("message", statusMessage);
                return result;
            }
            
            // 로그인 성공 처리
            String sessionId = "SESS_" + System.currentTimeMillis() + "_" + userId;
            
            // 임시 비밀번호로 로그인했는지 확인
            boolean isTempPasswordLogin = (tempPassword != null && tempPassword.equals(encryptedPassword));
            
            // 로그인 실패 횟수 초기화 및 마지막 로그인 시간 업데이트
            String successSql = "UPDATE USERS SET LOGIN_FAIL_COUNT = 0, LAST_LOGIN_TIMESTAMP = CURRENT TIMESTAMP WHERE USER_ID = ?";
            jdbcTemplate.update(successSql, userId);
            
            // 세션 생성
            String sessionSql = "INSERT INTO USER_SESSIONS (SESSION_ID, USER_ID, IP_ADDRESS, USER_AGENT) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sessionSql, sessionId, userId, ipAddress, userAgent);
            
            // 감사 로그 기록
            String auditMessage = isTempPasswordLogin ? "임시 비밀번호로 로그인 성공" : "로그인 성공";
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, ?, 'SUCCESS', ?)";
            jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, sessionId, auditMessage);
            
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("message", "로그인 성공");
            result.put("isTempPassword", isTempPasswordLogin);
            
        } catch (Exception e) {
            logger.error("로그인 처리 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "로그인 처리 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 로그아웃 처리
    @Transactional
    public boolean logout(String sessionId, String userId) {
        try {
            // 세션 상태 업데이트
            String sessionSql = "UPDATE USER_SESSIONS SET STATUS = 'LOGOUT' WHERE SESSION_ID = ? AND USER_ID = ?";
            jdbcTemplate.update(sessionSql, sessionId, userId);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, SESSION_ID, STATUS) VALUES (?, 'LOGOUT', 'USER', ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, sessionId);
            
            return true;
        } catch (Exception e) {
            logger.error("로그아웃 처리 중 오류 발생", e);
            return false;
        }
    }
    
    // 그룹 목록 조회
    public List<Map<String, Object>> getGroupList() {
        String sql = "SELECT * FROM USER_GROUPS WHERE STATUS = 'ACTIVE' ORDER BY GROUP_NAME";
        return jdbcTemplate.queryForList(sql);
    }
    
    // 사용자 그룹 매핑 (단일 그룹)
    @Transactional
    public boolean assignUserToGroup(String userId, String groupId, String assignedBy) {
        try {
            // 기존 그룹 매핑 삭제 (단일 그룹 정책)
            String deleteSql = "DELETE FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
            jdbcTemplate.update(deleteSql, userId);
            
            // 새로운 그룹 매핑 추가
            String insertSql = "INSERT INTO USER_GROUP_MAPPING (USER_ID, GROUP_ID, ASSIGNED_BY, ASSIGNED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
            jdbcTemplate.update(insertSql, userId, groupId, assignedBy);
            
            return true;
        } catch (Exception e) {
            logger.error("사용자 그룹 매핑 중 오류 발생", e);
            return false;
        }
    }
    
    // SQL 템플릿 카테고리 권한 조회
    public List<Map<String, Object>> getSqlTemplateCategoryPermissions(String userId) {
        String sql = "SELECT gcm.CATEGORY_ID, stc.CATEGORY_NAME, stc.CATEGORY_DESCRIPTION " +
                    "FROM GROUP_CATEGORY_MAPPING gcm " +
                    "INNER JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " +
                    "LEFT JOIN SQL_TEMPLATE_CATEGORY stc ON gcm.CATEGORY_ID = stc.CATEGORY_ID " +
                    "WHERE ugm.USER_ID = ? " +
                    "ORDER BY stc.CATEGORY_ORDER, stc.CATEGORY_NAME";
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    // 연결 정보 권한 조회
    public List<Map<String, Object>> getConnectionPermissions(String userId) {
        String sql = "SELECT gcm.CONNECTION_ID, dc.DB_TYPE " +
                    "FROM GROUP_CONNECTION_MAPPING gcm " +
                    "INNER JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " +
                    "LEFT JOIN DATABASE_CONNECTION dc ON gcm.CONNECTION_ID = dc.CONNECTION_ID " +
                    "WHERE ugm.USER_ID = ? " +
                    "ORDER BY gcm.CONNECTION_ID";
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    // 사용자 권한 저장 (단순화)
    @Transactional
    public boolean saveUserPermissions(String userId, Map<String, Object> permissions, String savedBy) {
        try {
            // 사용자의 그룹 ID 조회
            String groupSql = "SELECT GROUP_ID FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
            List<Map<String, Object>> groupResult = jdbcTemplate.queryForList(groupSql, userId);
            
            if (groupResult.isEmpty()) {
                return false; // 사용자가 그룹에 할당되지 않음
            }
            
            String groupId = (String) groupResult.get(0).get("GROUP_ID");
            
            // 기존 권한 삭제
            String deleteCategorySql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ?";
            String deleteConnectionSql = "DELETE FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ?";
            jdbcTemplate.update(deleteCategorySql, groupId);
            jdbcTemplate.update(deleteConnectionSql, groupId);
            
            // SQL 템플릿 카테고리 권한 저장
            @SuppressWarnings("unchecked")
            List<String> categoryIds = (List<String>) permissions.get("categories");
            if (categoryIds != null) {
                for (String categoryId : categoryIds) {
                    String insertSql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                    jdbcTemplate.update(insertSql, groupId, categoryId, savedBy);
                }
            }
            
            // 연결 정보 권한 저장
            @SuppressWarnings("unchecked")
            List<String> connectionIds = (List<String>) permissions.get("connections");
            if (connectionIds != null) {
                for (String connectionId : connectionIds) {
                    String insertSql = "INSERT INTO GROUP_CONNECTION_MAPPING (GROUP_ID, CONNECTION_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                    jdbcTemplate.update(insertSql, groupId, connectionId, savedBy);
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("사용자 권한 저장 중 오류 발생", e);
            return false;
        }
    }
    
    // 사용자 활동 로그 조회
    public List<Map<String, Object>> getUserActivityLogs(String userId, String dateRange) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, CREATED_TIMESTAMP, STATUS, ERROR_MESSAGE ");
        sql.append("FROM AUDIT_LOGS WHERE USER_ID = ? ");
        
        List<Object> params = new ArrayList<>();
        params.add(userId);
        
        if (dateRange != null && !dateRange.trim().isEmpty()) {
            // 날짜 범위 조건 추가 (예: "7days", "30days")
            if ("7days".equals(dateRange)) {
                sql.append("AND CREATED_TIMESTAMP >= CURRENT DATE - 7 DAYS ");
            } else if ("30days".equals(dateRange)) {
                sql.append("AND CREATED_TIMESTAMP >= CURRENT DATE - 30 DAYS ");
            }
        }
        
        sql.append("ORDER BY CREATED_TIMESTAMP DESC");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }
    
    // 비밀번호 변경
    @Transactional
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        try {
            // 현재 비밀번호 확인
            String currentPasswordSql = "SELECT PASSWORD FROM USERS WHERE USER_ID = ?";
            String currentPassword = jdbcTemplate.queryForObject(currentPasswordSql, String.class, userId);
            
            if (!currentPassword.equals(Crypto.crypt(oldPassword))) {
                return false; // 현재 비밀번호가 틀림
            }
            
            // 새 비밀번호로 업데이트
            String updateSql = "UPDATE USERS SET PASSWORD = ?, PASSWORD_CHANGE_DATE = CURRENT DATE WHERE USER_ID = ?";
            jdbcTemplate.update(updateSql, Crypto.crypt(newPassword), userId);
            
            return true;
        } catch (Exception e) {
            logger.error("비밀번호 변경 중 오류 발생", e);
            return false;
        }
    }
    
    // 임시 비밀번호 설정
    @Transactional
    public boolean setTemporaryPassword(String userId, String tempPassword) {
        try {
            String sql = "UPDATE USERS SET PASSWORD = ?, PASSWORD_CHANGE_DATE = CURRENT DATE WHERE USER_ID = ?";
            jdbcTemplate.update(sql, Crypto.crypt(tempPassword), userId);
            return true;
        } catch (Exception e) {
            logger.error("임시 비밀번호 설정 중 오류 발생", e);
            return false;
        }
    }
    
    // 임시 비밀번호 검증
    public boolean validateTemporaryPassword(String userId, String tempPassword) {
        try {
            String sql = "SELECT PASSWORD FROM USERS WHERE USER_ID = ?";
            String storedPassword = jdbcTemplate.queryForObject(sql, String.class, userId);
            return storedPassword.equals(Crypto.crypt(tempPassword));
        } catch (Exception e) {
            logger.error("임시 비밀번호 검증 중 오류 발생", e);
            return false;
        }
    }
    
    // 임시 비밀번호 여부 확인 (TEMP_PASSWORD가 존재하고 PASSWORD와 동일한지 확인)
    public boolean isTemporaryPassword(String userId) {
        try {
            String sql = "SELECT CASE WHEN TEMP_PASSWORD IS NOT NULL AND TEMP_PASSWORD = PASSWORD THEN 'Y' ELSE 'N' END FROM USERS WHERE USER_ID = ?";
            String isTempPassword = jdbcTemplate.queryForObject(sql, String.class, userId);
            return "Y".equals(isTempPassword);
        } catch (Exception e) {
            logger.error("임시 비밀번호 여부 확인 중 오류 발생", e);
            return false;
        }
    }
    
    // 임시 비밀번호에서 새 비밀번호로 변경
    @Transactional
    public boolean changePasswordFromTemp(String userId, String newPassword) {
        try {
            String sql = "UPDATE USERS SET PASSWORD = ?, TEMP_PASSWORD = NULL, PASSWORD_CHANGE_DATE = CURRENT DATE WHERE USER_ID = ?";
            jdbcTemplate.update(sql, Crypto.crypt(newPassword), userId);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, STATUS, ERROR_MESSAGE) " +
                            "VALUES (?, 'CHANGE_PASSWORD', 'USER', ?, 'SUCCESS', ?)";
            jdbcTemplate.update(auditSql, userId, userId, "임시 비밀번호에서 정상 비밀번호로 변경됨");
            
            return true;
        } catch (Exception e) {
            logger.error("임시 비밀번호에서 새 비밀번호로 변경 중 오류 발생", e);
            return false;
        }
    }
    
    // 사용자 비밀번호 초기화
    @Transactional
    public boolean resetUserPassword(String userId, String defaultPassword, String resetBy) {
        try {
            // 사용자 존재 여부 확인
            String checkSql = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
            int userCount = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);
            
            if (userCount == 0) {
                return false;
            }
            
            // 비밀번호 초기화
            String resetSql = "UPDATE USERS SET PASSWORD = ?, PASSWORD_CHANGE_DATE = CURRENT DATE, " +
                            "LOGIN_FAIL_COUNT = 0, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                            "WHERE USER_ID = ?";
            jdbcTemplate.update(resetSql, Crypto.crypt(defaultPassword), resetBy, userId);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, GRANTED_BY, STATUS) " +
                            "VALUES (?, 'RESET_PASSWORD', 'USER', ?, ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, userId, resetBy);
            
            return true;
        } catch (Exception e) {
            logger.error("사용자 비밀번호 초기화 중 오류 발생", e);
            return false;
        }
    }
    
    // 사용자 계정 초기화
    @Transactional
    public boolean resetUserAccount(String userId, String resetBy) {
        try {
            // 사용자 존재 여부 확인
            String checkSql = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
            int userCount = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);
            
            if (userCount == 0) {
                return false;
            }
            
            // 계정 초기화 (비밀번호: 1234, 로그인 실패 횟수: 0, 상태: ACTIVE)
            String resetSql = "UPDATE USERS SET PASSWORD = ?, PASSWORD_CHANGE_DATE = CURRENT DATE, " +
                            "LOGIN_FAIL_COUNT = 0, STATUS = 'ACTIVE', MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                            "WHERE USER_ID = ?";
            jdbcTemplate.update(resetSql, Crypto.crypt("1234"), resetBy, userId);
            
            // 기존 세션 삭제
            String sessionSql = "DELETE FROM USER_SESSIONS WHERE USER_ID = ?";
            jdbcTemplate.update(sessionSql, userId);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, GRANTED_BY, STATUS) " +
                            "VALUES (?, 'RESET_ACCOUNT', 'USER', ?, ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, userId, resetBy);
            
            return true;
        } catch (Exception e) {
            logger.error("사용자 계정 초기화 중 오류 발생", e);
            return false;
        }
    }
    
    // 사용자 계정 잠금 해제
    @Transactional
    public boolean unlockUserAccount(String userId, String unlockedBy) {
        try {
            // 사용자 존재 여부 확인
            String checkSql = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
            int userCount = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);
            
            if (userCount == 0) {
                return false;
            }
            
            // 계정 잠금 해제
            String unlockSql = "UPDATE USERS SET LOGIN_FAIL_COUNT = 0, STATUS = 'ACTIVE', " +
                             "MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP " +
                             "WHERE USER_ID = ?";
            jdbcTemplate.update(unlockSql, unlockedBy, userId);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, GRANTED_BY, STATUS) " +
                            "VALUES (?, 'UNLOCK_ACCOUNT', 'USER', ?, ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, userId, unlockedBy);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자의 현재 그룹 조회
    public Map<String, Object> getCurrentUserGroup(String userId) {
        try {
            String sql = "SELECT ugm.GROUP_ID, ug.GROUP_NAME " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN USER_GROUPS ug ON ugm.GROUP_ID = ug.GROUP_ID " +
                        "WHERE ugm.USER_ID = ?";
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            logger.error("사용자 현재 그룹 조회 중 오류 발생", e);
            return null;
        }
    }
}
