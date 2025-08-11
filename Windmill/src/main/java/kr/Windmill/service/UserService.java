package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class UserService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 사용자 목록 조회
    public List<Map<String, Object>> getUserList() {
        String sql = "SELECT USER_ID, USER_NAME, STATUS, LAST_LOGIN_TIMESTAMP, LOGIN_FAIL_COUNT, CREATED_TIMESTAMP FROM USERS ORDER BY CREATED_TIMESTAMP DESC";
        return jdbcTemplate.queryForList(sql);
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
            String sql = "INSERT INTO USERS (USER_ID, USER_NAME, PASSWORD, STATUS, CREATED_BY) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                userData.get("userId"),
                userData.get("userName"),
                userData.get("password"),
                userData.get("status"),
                userData.get("createdBy")
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자 수정
    @Transactional
    public boolean updateUser(String userId, Map<String, Object> userData) {
        try {
            String sql = "UPDATE USERS SET USER_NAME = ?, STATUS = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE USER_ID = ?";
            jdbcTemplate.update(sql,
                userData.get("userName"),
                userData.get("status"),
                userData.get("modifiedBy"),
                userId
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', '사용자가 존재하지 않습니다')";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent);
                
                result.put("success", false);
                result.put("message", "사용자가 존재하지 않습니다.");
                return result;
            }
            
            Map<String, Object> user = userInfo.get(0);
            String status = (String) user.get("STATUS");
            Integer failCount = (Integer) user.get("LOGIN_FAIL_COUNT");
            
            // 계정 상태 확인
            if (!"ACTIVE".equals(status)) {
                // 비활성 계정 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', '비활성화된 계정입니다')";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent);
                
                result.put("success", false);
                result.put("message", "비활성화된 계정입니다.");
                return result;
            }
            
            // 로그인 실패 횟수 확인
            if (failCount != null && failCount >= 5) {
                // 계정 잠금 상태 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', '로그인 실패 횟수 초과로 계정이 잠겼습니다')";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent);
                
                result.put("success", false);
                result.put("message", "로그인 실패 횟수 초과로 계정이 잠겼습니다.");
                return result;
            }
            
            // 비밀번호 확인 (실제로는 암호화된 비밀번호 비교)
            String passwordSql = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ? AND PASSWORD = ?";
            int passwordMatch = jdbcTemplate.queryForObject(passwordSql, Integer.class, userId, password);
            
            if (passwordMatch == 0) {
                // 로그인 실패 횟수 증가
                String failSql = "UPDATE USERS SET LOGIN_FAIL_COUNT = LOGIN_FAIL_COUNT + 1 WHERE USER_ID = ?";
                jdbcTemplate.update(failSql, userId);
                
                // 비밀번호 불일치 로그인 시도 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', '비밀번호가 일치하지 않습니다')";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent);
                
                result.put("success", false);
                result.put("message", "비밀번호가 일치하지 않습니다.");
                return result;
            }
            
            // 로그인 성공 처리
            String sessionId = "SESS_" + System.currentTimeMillis() + "_" + userId;
            
            // 로그인 실패 횟수 초기화 및 마지막 로그인 시간 업데이트
            String successSql = "UPDATE USERS SET LOGIN_FAIL_COUNT = 0, LAST_LOGIN_TIMESTAMP = CURRENT TIMESTAMP WHERE USER_ID = ?";
            jdbcTemplate.update(successSql, userId);
            
            // 세션 생성
            String sessionSql = "INSERT INTO USER_SESSIONS (SESSION_ID, USER_ID, IP_ADDRESS, USER_AGENT) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sessionSql, sessionId, userId, ipAddress, userAgent);
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS) VALUES (?, 'LOGIN', 'USER', ?, ?, ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, sessionId);
            
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("message", "로그인 성공");
            
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹 목록 조회
    public List<Map<String, Object>> getGroupList() {
        String sql = "SELECT * FROM USER_GROUPS WHERE STATUS = 'ACTIVE' ORDER BY GROUP_NAME";
        return jdbcTemplate.queryForList(sql);
    }
    
    // 사용자 그룹 매핑
    @Transactional
    public boolean assignUserToGroup(String userId, String groupId, String assignedBy) {
        try {
            String sql = "INSERT INTO USER_GROUP_MAPPING (USER_ID, GROUP_ID, ASSIGNED_BY) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, userId, groupId, assignedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자 그룹 해제
    @Transactional
    public boolean removeUserFromGroup(String userId, String groupId) {
        try {
            String sql = "DELETE FROM USER_GROUP_MAPPING WHERE USER_ID = ? AND GROUP_ID = ?";
            jdbcTemplate.update(sql, userId, groupId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // SQL 템플릿 권한 조회
    public List<Map<String, Object>> getSqlTemplatePermissions(String userId) {
        try {
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.CATEGORY_PATH, " +
                        "CASE WHEN p.USER_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM SQL_TEMPLATE t " +
                        "LEFT JOIN SQL_TEMPLATE_PERMISSIONS p ON t.TEMPLATE_ID = p.TEMPLATE_ID AND p.USER_ID = ? " +
                        "WHERE t.STATUS = 'ACTIVE' " +
                        "ORDER BY t.CATEGORY_PATH, t.TEMPLATE_NAME";
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // 연결 정보 권한 조회
    public List<Map<String, Object>> getConnectionPermissions(String userId) {
        try {
            String sql = "SELECT c.CONNECTION_ID, c.CONNECTION_NAME, c.DB_TYPE, " +
                        "CASE WHEN p.USER_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM DATABASE_CONNECTION c " +
                        "LEFT JOIN CONNECTION_PERMISSIONS p ON c.CONNECTION_ID = p.CONNECTION_ID AND p.USER_ID = ? " +
                        "WHERE c.STATUS = 'ACTIVE' " +
                        "ORDER BY c.CONNECTION_NAME";
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // 사용자 권한 저장
    @Transactional
    public boolean saveUserPermissions(String userId, Map<String, Object> permissions, String modifiedBy) {
        try {
            // SQL 템플릿 권한 저장
            List<Map<String, Object>> sqlPermissions = (List<Map<String, Object>>) permissions.get("sqlTemplatePermissions");
            if (sqlPermissions != null) {
                // 기존 SQL 템플릿 권한 삭제
                String deleteSqlSql = "DELETE FROM SQL_TEMPLATE_PERMISSIONS WHERE USER_ID = ?";
                jdbcTemplate.update(deleteSqlSql, userId);
                
                // 새로운 SQL 템플릿 권한 추가
                for (Map<String, Object> permission : sqlPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertSqlSql = "INSERT INTO SQL_TEMPLATE_PERMISSIONS (USER_ID, TEMPLATE_ID, GRANTED_BY) VALUES (?, ?, ?)";
                        jdbcTemplate.update(insertSqlSql, userId, permission.get("templateId"), modifiedBy);
                    }
                }
            }
            
            // 연결 정보 권한 저장
            List<Map<String, Object>> connPermissions = (List<Map<String, Object>>) permissions.get("connectionPermissions");
            if (connPermissions != null) {
                // 기존 연결 정보 권한 삭제
                String deleteConnSql = "DELETE FROM CONNECTION_PERMISSIONS WHERE USER_ID = ?";
                jdbcTemplate.update(deleteConnSql, userId);
                
                // 새로운 연결 정보 권한 추가
                for (Map<String, Object> permission : connPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertConnSql = "INSERT INTO CONNECTION_PERMISSIONS (USER_ID, CONNECTION_ID, GRANTED_BY) VALUES (?, ?, ?)";
                        jdbcTemplate.update(insertConnSql, userId, permission.get("connectionId"), modifiedBy);
                    }
                }
            }
            
            // 감사 로그 기록
            String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, MODIFIED_BY, STATUS) VALUES (?, 'PERMISSION_UPDATE', 'USER', ?, 'SUCCESS')";
            jdbcTemplate.update(auditSql, userId, modifiedBy);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 사용자 활동 로그 조회
    public List<Map<String, Object>> getUserActivityLogs(String userId, String dateRange) {
        try {
            String sql = "SELECT TIMESTAMP, ACTION_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE, DETAILS " +
                        "FROM AUDIT_LOGS " +
                        "WHERE USER_ID = ? ";
            
            if (dateRange != null && !dateRange.equals("all")) {
                int days = Integer.parseInt(dateRange);
                sql += "AND TIMESTAMP >= CURRENT TIMESTAMP - " + days + " DAYS ";
            }
            
            sql += "ORDER BY TIMESTAMP DESC";
            
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // Common.UserConf와 동일한 기능 - DB 기반 사용자 설정 조회
    public Map<String, String> getUserConfig(String userId) {
        try {
            String sql = "SELECT USER_ID, USER_NAME, STATUS, IP_ADDRESS, LAST_LOGIN_TIMESTAMP, " +
                        "LOGIN_FAIL_COUNT, CREATED_TIMESTAMP " +
                        "FROM USERS WHERE USER_ID = ?";
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
            if (result.isEmpty()) {
                return new HashMap<>();
            }
            
            Map<String, Object> user = result.get(0);
            Map<String, String> config = new HashMap<>();
            
            config.put("UserName", (String) user.get("USER_ID"));
            config.put("ID", (String) user.get("USER_ID"));
            config.put("NAME", (String) user.get("USER_NAME"));
            config.put("IP", (String) user.get("IP_ADDRESS"));
            config.put("STATUS", (String) user.get("STATUS"));
            
            // 연결 정보 권한 조회
            String connectionSql = "SELECT c.CONNECTION_NAME FROM DATABASE_CONNECTION c " +
                                 "INNER JOIN CONNECTION_PERMISSIONS p ON c.CONNECTION_ID = p.CONNECTION_ID " +
                                 "WHERE p.USER_ID = ? AND c.STATUS = 'ACTIVE'";
            List<Map<String, Object>> connections = jdbcTemplate.queryForList(connectionSql, userId);
            String connectionList = connections.stream()
                .map(conn -> (String) conn.get("CONNECTION_NAME"))
                .collect(java.util.stream.Collectors.joining(","));
            config.put("CONNECTION", connectionList);
            
            // 메뉴 권한 조회 (SQL 템플릿 기반)
            String menuSql = "SELECT t.CATEGORY_PATH FROM SQL_TEMPLATE t " +
                           "INNER JOIN SQL_TEMPLATE_PERMISSIONS p ON t.TEMPLATE_ID = p.TEMPLATE_ID " +
                           "WHERE p.USER_ID = ? AND t.STATUS = 'ACTIVE' " +
                           "GROUP BY t.CATEGORY_PATH";
            List<Map<String, Object>> menus = jdbcTemplate.queryForList(menuSql, userId);
            String menuList = menus.stream()
                .map(menu -> (String) menu.get("CATEGORY_PATH"))
                .collect(java.util.stream.Collectors.joining(","));
            config.put("MENU", menuList);
            
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // Common.UserList와 동일한 기능 - DB 기반 사용자 목록 조회
    public List<Map<String, String>> getUserListForCompatibility() {
        try {
            String sql = "SELECT USER_ID, USER_NAME, STATUS FROM USERS ORDER BY USER_ID";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            
            List<Map<String, String>> userList = new ArrayList<>();
            for (Map<String, Object> user : result) {
                Map<String, String> userMap = new HashMap<>();
                userMap.put("id", (String) user.get("USER_ID"));
                userMap.put("name", (String) user.get("USER_NAME"));
                userList.add(userMap);
            }
            
            return userList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
