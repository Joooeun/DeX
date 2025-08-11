package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
}
