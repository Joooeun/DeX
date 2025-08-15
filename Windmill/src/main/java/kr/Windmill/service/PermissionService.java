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
    
    // SQL 템플릿 카테고리 권한 확인 (단순화)
    public boolean checkSqlTemplateCategoryPermission(String userId, String categoryId) {
        try {
            String sql = "SELECT COUNT(*) FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN GROUP_CATEGORY_MAPPING gcm ON ugm.GROUP_ID = gcm.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? AND gcm.CATEGORY_ID = ?";
            
            int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, categoryId);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    

    
    // 연결 정보 권한 확인 (단순화)
    public boolean checkConnectionPermission(String userId, String connectionId) {
        try {
            String sql = "SELECT COUNT(*) FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN GROUP_CONNECTION_MAPPING gcm ON ugm.GROUP_ID = gcm.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? AND gcm.CONNECTION_ID = ?";
            
            int count = jdbcTemplate.queryForObject(sql, Integer.class, userId, connectionId);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자가 접근 가능한 SQL 템플릿 카테고리 목록 조회
    public List<String> getAuthorizedCategories(String userId) {
        try {
            String sql = "SELECT DISTINCT gcm.CATEGORY_ID " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN GROUP_CATEGORY_MAPPING gcm ON ugm.GROUP_ID = gcm.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? " +
                        "ORDER BY gcm.CATEGORY_ID";
            
            return jdbcTemplate.queryForList(sql, String.class, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 사용자의 SQL 템플릿 카테고리 권한 상세 목록 조회
    public List<Map<String, Object>> getUserSqlTemplateCategoryPermissions(String userId) {
        try {
            String sql = "SELECT gcm.CATEGORY_ID, ug.GROUP_NAME, stc.CATEGORY_NAME, stc.CATEGORY_DESCRIPTION " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN GROUP_CATEGORY_MAPPING gcm ON ugm.GROUP_ID = gcm.GROUP_ID " +
                        "INNER JOIN USER_GROUPS ug ON gcm.GROUP_ID = ug.GROUP_ID " +
                        "LEFT JOIN SQL_TEMPLATE_CATEGORY stc ON gcm.CATEGORY_ID = stc.CATEGORY_ID " +
                        "WHERE ugm.USER_ID = ? " +
                        "ORDER BY stc.CATEGORY_ORDER, stc.CATEGORY_NAME";
            
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    

    
    // 사용자의 연결 정보 권한 목록 조회
    public List<Map<String, Object>> getUserConnectionPermissions(String userId) {
        try {
            String sql = "SELECT gcm.CONNECTION_ID, ug.GROUP_NAME " +
                        "FROM USER_GROUP_MAPPING ugm " +
                        "INNER JOIN GROUP_CONNECTION_MAPPING gcm ON ugm.GROUP_ID = gcm.GROUP_ID " +
                        "INNER JOIN USER_GROUPS ug ON gcm.GROUP_ID = ug.GROUP_ID " +
                        "WHERE ugm.USER_ID = ? " +
                        "ORDER BY gcm.CONNECTION_ID";
            
            return jdbcTemplate.queryForList(sql, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 그룹에 SQL 템플릿 카테고리 권한 부여
    public boolean grantSqlTemplateCategoryPermission(String groupId, String categoryId, String grantedBy) {
        try {
            String sql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) " +
                        "VALUES (?, ?, ?, CURRENT TIMESTAMP)";
            jdbcTemplate.update(sql, groupId, categoryId, grantedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    

    
    // 그룹에 연결 정보 권한 부여
    public boolean grantConnectionPermission(String groupId, String connectionId, String grantedBy) {
        try {
            String sql = "INSERT INTO GROUP_CONNECTION_MAPPING (GROUP_ID, CONNECTION_ID, GRANTED_BY, GRANTED_TIMESTAMP) " +
                        "VALUES (?, ?, ?, CURRENT TIMESTAMP)";
            jdbcTemplate.update(sql, groupId, connectionId, grantedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹의 SQL 템플릿 카테고리 권한 해제
    public boolean revokeSqlTemplateCategoryPermission(String groupId, String categoryId) {
        try {
            String sql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID = ?";
            jdbcTemplate.update(sql, groupId, categoryId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹의 모든 카테고리 권한 해제
    public boolean revokeAllCategoryPermissions(String groupId) {
        try {
            String sql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ?";
            jdbcTemplate.update(sql, groupId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹의 모든 연결정보 권한 해제
    public boolean revokeAllConnectionPermissions(String groupId) {
        try {
            String sql = "DELETE FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ?";
            jdbcTemplate.update(sql, groupId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    

    
    // 그룹의 연결 정보 권한 해제
    public boolean revokeConnectionPermission(String groupId, String connectionId) {
        try {
            String sql = "DELETE FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ? AND CONNECTION_ID = ?";
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
    
    // 모든 SQL 템플릿 카테고리 목록 조회
    public List<Map<String, Object>> getAllCategories() {
        try {
            String sql = "SELECT CATEGORY_ID, CATEGORY_NAME, CATEGORY_DESCRIPTION, CATEGORY_ORDER, STATUS " +
                        "FROM SQL_TEMPLATE_CATEGORY " +
                        "WHERE STATUS = 'ACTIVE' " +
                        "ORDER BY CATEGORY_ORDER, CATEGORY_NAME";
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 그룹별 SQL 템플릿 카테고리 권한 목록 조회
    public List<Map<String, Object>> getGroupCategoryPermissions(String groupId) {
        try {
            String sql = "SELECT gcm.CATEGORY_ID, gcm.GRANTED_BY, gcm.GRANTED_TIMESTAMP, " +
                        "stc.CATEGORY_NAME, stc.CATEGORY_DESCRIPTION " +
                        "FROM GROUP_CATEGORY_MAPPING gcm " +
                        "LEFT JOIN SQL_TEMPLATE_CATEGORY stc ON gcm.CATEGORY_ID = stc.CATEGORY_ID " +
                        "WHERE gcm.GROUP_ID = ? " +
                        "ORDER BY stc.CATEGORY_ORDER, stc.CATEGORY_NAME";
            
            return jdbcTemplate.queryForList(sql, groupId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
