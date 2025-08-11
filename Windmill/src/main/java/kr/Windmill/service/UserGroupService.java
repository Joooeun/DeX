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
public class UserGroupService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 그룹 목록 조회
    public List<Map<String, Object>> getGroupList() {
        String sql = "SELECT g.*, " +
                    "(SELECT COUNT(*) FROM USER_GROUP_MAPPING WHERE GROUP_ID = g.GROUP_ID) AS MEMBER_COUNT " +
                    "FROM USER_GROUPS g ORDER BY g.CREATED_TIMESTAMP DESC";
        return jdbcTemplate.queryForList(sql);
    }
    
    // 그룹 상세 조회
    public Map<String, Object> getGroupDetail(String groupId) {
        String sql = "SELECT * FROM USER_GROUPS WHERE GROUP_ID = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, groupId);
        return result.isEmpty() ? null : result.get(0);
    }
    
    // 그룹 생성
    @Transactional
    public boolean createGroup(Map<String, Object> groupData) {
        try {
            String sql = "INSERT INTO USER_GROUPS (GROUP_ID, GROUP_NAME, DESCRIPTION, PARENT_GROUP_ID, STATUS, CREATED_BY) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                groupData.get("groupId"),
                groupData.get("groupName"),
                groupData.get("description"),
                groupData.get("parentGroupId"),
                groupData.get("status"),
                groupData.get("createdBy")
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹 수정
    @Transactional
    public boolean updateGroup(String groupId, Map<String, Object> groupData) {
        try {
            String sql = "UPDATE USER_GROUPS SET GROUP_NAME = ?, DESCRIPTION = ?, PARENT_GROUP_ID = ?, STATUS = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE GROUP_ID = ?";
            jdbcTemplate.update(sql,
                groupData.get("groupName"),
                groupData.get("description"),
                groupData.get("parentGroupId"),
                groupData.get("status"),
                groupData.get("modifiedBy"),
                groupId
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 그룹 삭제
    @Transactional
    public boolean deleteGroup(String groupId) {
        try {
            // 그룹 멤버 매핑 삭제
            String deleteMappingSql = "DELETE FROM USER_GROUP_MAPPING WHERE GROUP_ID = ?";
            jdbcTemplate.update(deleteMappingSql, groupId);
            
            // 그룹 삭제
            String deleteGroupSql = "DELETE FROM USER_GROUPS WHERE GROUP_ID = ?";
            jdbcTemplate.update(deleteGroupSql, groupId);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 상위 그룹 목록 조회
    public List<Map<String, Object>> getParentGroupList() {
        String sql = "SELECT GROUP_ID, GROUP_NAME FROM USER_GROUPS WHERE STATUS = 'ACTIVE' ORDER BY GROUP_NAME";
        return jdbcTemplate.queryForList(sql);
    }
    
    // 사용 가능한 사용자 목록 조회 (그룹에 속하지 않은 사용자)
    public List<Map<String, Object>> getAvailableUsers(String groupId) {
        String sql = "SELECT USER_ID, USER_NAME FROM USERS " +
                    "WHERE USER_ID NOT IN (SELECT USER_ID FROM USER_GROUP_MAPPING WHERE GROUP_ID = ?) " +
                    "AND STATUS = 'ACTIVE' " +
                    "ORDER BY USER_NAME";
        return jdbcTemplate.queryForList(sql, groupId);
    }
    
    // 그룹 멤버 목록 조회
    public List<Map<String, Object>> getGroupMembers(String groupId) {
        String sql = "SELECT u.USER_ID, u.USER_NAME, u.STATUS " +
                    "FROM USERS u " +
                    "INNER JOIN USER_GROUP_MAPPING m ON u.USER_ID = m.USER_ID " +
                    "WHERE m.GROUP_ID = ? " +
                    "ORDER BY u.USER_NAME";
        return jdbcTemplate.queryForList(sql, groupId);
    }
    
    // 사용자를 그룹에 추가
    @Transactional
    public boolean addUserToGroup(String groupId, String userId, String assignedBy) {
        try {
            String sql = "INSERT INTO USER_GROUP_MAPPING (USER_ID, GROUP_ID, ASSIGNED_BY) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, userId, groupId, assignedBy);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 사용자를 그룹에서 제거
    @Transactional
    public boolean removeUserFromGroup(String groupId, String userId) {
        try {
            String sql = "DELETE FROM USER_GROUP_MAPPING WHERE GROUP_ID = ? AND USER_ID = ?";
            jdbcTemplate.update(sql, groupId, userId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // SQL 템플릿 권한 조회
    public List<Map<String, Object>> getSqlTemplatePermissions(String groupId) {
        try {
            String sql = "SELECT t.TEMPLATE_ID, t.TEMPLATE_NAME, t.CATEGORY_PATH, " +
                        "CASE WHEN p.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM SQL_TEMPLATE t " +
                        "LEFT JOIN SQL_TEMPLATE_GROUP_PERMISSIONS p ON t.TEMPLATE_ID = p.TEMPLATE_ID AND p.GROUP_ID = ? " +
                        "WHERE t.STATUS = 'ACTIVE' " +
                        "ORDER BY t.CATEGORY_PATH, t.TEMPLATE_NAME";
            return jdbcTemplate.queryForList(sql, groupId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 연결 정보 권한 조회
    public List<Map<String, Object>> getConnectionPermissions(String groupId) {
        try {
            String sql = "SELECT c.CONNECTION_ID, c.CONNECTION_NAME, c.DB_TYPE, " +
                        "CASE WHEN p.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM DATABASE_CONNECTION c " +
                        "LEFT JOIN CONNECTION_GROUP_PERMISSIONS p ON c.CONNECTION_ID = p.CONNECTION_ID AND p.GROUP_ID = ? " +
                        "WHERE c.STATUS = 'ACTIVE' " +
                        "ORDER BY c.CONNECTION_NAME";
            return jdbcTemplate.queryForList(sql, groupId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 그룹 권한 저장
    @Transactional
    public boolean saveGroupPermissions(String groupId, Map<String, Object> permissions, String modifiedBy) {
        try {
            // SQL 템플릿 권한 저장
            List<Map<String, Object>> sqlPermissions = (List<Map<String, Object>>) permissions.get("sqlTemplatePermissions");
            if (sqlPermissions != null) {
                // 기존 SQL 템플릿 권한 삭제
                String deleteSqlSql = "DELETE FROM SQL_TEMPLATE_GROUP_PERMISSIONS WHERE GROUP_ID = ?";
                jdbcTemplate.update(deleteSqlSql, groupId);
                
                // 새로운 SQL 템플릿 권한 추가
                for (Map<String, Object> permission : sqlPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertSqlSql = "INSERT INTO SQL_TEMPLATE_GROUP_PERMISSIONS (GROUP_ID, TEMPLATE_ID, GRANTED_BY) VALUES (?, ?, ?)";
                        jdbcTemplate.update(insertSqlSql, groupId, permission.get("templateId"), modifiedBy);
                    }
                }
            }
            
            // 연결 정보 권한 저장
            List<Map<String, Object>> connPermissions = (List<Map<String, Object>>) permissions.get("connectionPermissions");
            if (connPermissions != null) {
                // 기존 연결 정보 권한 삭제
                String deleteConnSql = "DELETE FROM CONNECTION_GROUP_PERMISSIONS WHERE GROUP_ID = ?";
                jdbcTemplate.update(deleteConnSql, groupId);
                
                // 새로운 연결 정보 권한 추가
                for (Map<String, Object> permission : connPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertConnSql = "INSERT INTO CONNECTION_GROUP_PERMISSIONS (GROUP_ID, CONNECTION_ID, GRANTED_BY) VALUES (?, ?, ?)";
                        jdbcTemplate.update(insertConnSql, groupId, permission.get("connectionId"), modifiedBy);
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
