package kr.Windmill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PermissionService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // SQL 템플릿 권한 확인
    public boolean checkSqlTemplatePermission(String userId, String templateId, String accessType) {
        try {
            String sql = "SELECT COUNT(*) FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN SQL_TEMPLATE_PERMISSIONS stp ON ugm.GROUP_ID = stp.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? AND stp.TEMPLATE_ID = ? AND stp.ACCESS_TYPE = ?";
            
            int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, templateId, accessType);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 연결 정보 권한 확인
    public boolean checkConnectionPermission(String userId, String connectionId, String accessType) {
        try {
            String sql = "SELECT COUNT(*) FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN CONNECTION_PERMISSIONS cp ON ugm.GROUP_ID = cp.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? AND cp.CONNECTION_ID = ? AND cp.ACCESS_TYPE = ?";
            
            int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, connectionId, accessType);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자의 SQL 템플릿 권한 목록 조회
    public List<Map<String, Object>> getUserSqlTemplatePermissions(String userId) {
        try {
            String sql = "SELECT stp.TEMPLATE_ID, stp.FOLDER_PATH, stp.ACCESS_TYPE, ug.GROUP_NAME " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN SQL_TEMPLATE_PERMISSIONS stp ON ugm.GROUP_ID = stp.GROUP_ID " +
                        "INNER JOIN USER_GROUPS ug ON stp.GROUP_ID = ug.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? " +
                        "ORDER BY stp.FOLDER_PATH, stp.TEMPLATE_ID";
            
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 사용자의 연결 정보 권한 목록 조회
    public List<Map<String, Object>> getUserConnectionPermissions(String userId) {
        try {
            String sql = "SELECT cp.CONNECTION_ID, cp.ACCESS_TYPE, ug.GROUP_NAME " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN CONNECTION_PERMISSIONS cp ON ugm.GROUP_ID = cp.GROUP_ID " +
                        "INNER JOIN USER_GROUPS ug ON cp.GROUP_ID = ug.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? " +
                        "ORDER BY cp.CONNECTION_ID";
            
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 그룹에 SQL 템플릿 권한 부여
    public boolean grantSqlTemplatePermission(String groupId, String templateId, String accessType, String grantedBy) {
        try {
            String sql = "INSERT INTO SQL_TEMPLATE_PERMISSIONS (GROUP_ID, TEMPLATE_ID, ACCESS_TYPE, GRANTED_BY) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, groupId, templateId, accessType, grantedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹에 연결 정보 권한 부여
    public boolean grantConnectionPermission(String groupId, String connectionId, String accessType, String grantedBy) {
        try {
            String sql = "INSERT INTO CONNECTION_PERMISSIONS (GROUP_ID, CONNECTION_ID, ACCESS_TYPE, GRANTED_BY) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, groupId, connectionId, accessType, grantedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹의 SQL 템플릿 권한 해제
    public boolean revokeSqlTemplatePermission(String groupId, String templateId) {
        try {
            String sql = "DELETE FROM SQL_TEMPLATE_PERMISSIONS WHERE GROUP_ID = ? AND TEMPLATE_ID = ?";
            jdbcTemplate.update(sql, groupId, templateId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹의 연결 정보 권한 해제
    public boolean revokeConnectionPermission(String groupId, String connectionId) {
        try {
            String sql = "DELETE FROM CONNECTION_PERMISSIONS WHERE GROUP_ID = ? AND CONNECTION_ID = ?";
            jdbcTemplate.update(sql, groupId, connectionId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 관리자 권한 확인 (ADMIN_GROUP 소속 여부)
    public boolean isAdmin(String userId) {
        try {
            // admin 사용자는 항상 관리자 권한
            if ("admin".equals(userId)) {
                return true;
            }
            
            String sql = "SELECT COUNT(*) FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN USER_GROUPS ug ON ugm.GROUP_ID = ug.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? AND ug.GROUP_ID = 'ADMIN_GROUP'";
            
            int count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자 세션 유효성 확인
    public boolean isValidSession(String sessionId, String userId) {
        try {
            String sql = "SELECT COUNT(*) FROM USER_SESSIONS WHERE SESSION_ID = ? AND USER_ID = ? AND STATUS = 'ACTIVE'";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId, userId);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 세션 만료 처리
    public void expireSession(String sessionId) {
        try {
            String sql = "UPDATE USER_SESSIONS SET STATUS = 'EXPIRED' WHERE SESSION_ID = ?";
            jdbcTemplate.update(sql, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
