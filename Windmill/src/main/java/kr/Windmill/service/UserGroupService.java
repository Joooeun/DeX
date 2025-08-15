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
            // groupId가 없으면 자동 생성
            String groupId = (String) groupData.get("groupId");
            if (groupId == null || groupId.trim().isEmpty()) {
                groupId = "GROUP_" + System.currentTimeMillis();
            }
            
            String sql = "INSERT INTO USER_GROUPS (GROUP_ID, GROUP_NAME, GROUP_DESCRIPTION, STATUS, CREATED_BY) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                groupId,
                groupData.get("groupName"),
                groupData.get("description"),
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
            String sql = "UPDATE USER_GROUPS SET GROUP_NAME = ?, GROUP_DESCRIPTION = ?, STATUS = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE GROUP_ID = ?";
            jdbcTemplate.update(sql,
                groupData.get("groupName"),
                groupData.get("description"),
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
    

    
    // 사용 가능한 사용자 목록 조회 (그룹에 속하지 않은 사용자)
    public List<Map<String, Object>> getAvailableUsers(String groupId) {
        String sql = "SELECT USER_ID, USER_NAME FROM USERS " +
                    "WHERE USER_ID NOT IN (SELECT USER_ID FROM USER_GROUP_MAPPING) " +
                    "AND STATUS = 'ACTIVE' " +
                    "ORDER BY USER_NAME";
        return jdbcTemplate.queryForList(sql);
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
    
    // 사용자를 그룹에 추가 (단일 그룹)
    @Transactional
    public boolean addUserToGroup(String groupId, String userId, String assignedBy) {
        try {
            // 기존 그룹 할당 삭제 후 새로운 그룹 할당
            String deleteSql = "DELETE FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
            jdbcTemplate.update(deleteSql, userId);
            
            String insertSql = "INSERT INTO USER_GROUP_MAPPING (USER_ID, GROUP_ID, ASSIGNED_BY) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, userId, groupId, assignedBy);
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
    
    // SQL 템플릿 카테고리 권한 조회
    public List<Map<String, Object>> getSqlTemplateCategoryPermissions(String groupId) {
        try {
            String sql = "SELECT stc.CATEGORY_ID, stc.CATEGORY_NAME, stc.CATEGORY_DESCRIPTION, " +
                        "CASE WHEN gcm.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM SQL_TEMPLATE_CATEGORY stc " +
                        "LEFT JOIN GROUP_CATEGORY_MAPPING gcm ON stc.CATEGORY_ID = gcm.CATEGORY_ID AND gcm.GROUP_ID = ? " +
                        "WHERE stc.STATUS = 'ACTIVE' " +
                        "ORDER BY stc.CATEGORY_ORDER, stc.CATEGORY_NAME";
            return jdbcTemplate.queryForList(sql, groupId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 연결 정보 권한 조회
    public List<Map<String, Object>> getConnectionPermissions(String groupId) {
        try {
            String sql = "SELECT c.CONNECTION_ID, c.DB_TYPE, " +
                        "CASE WHEN gcm.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                        "FROM DATABASE_CONNECTION c " +
                        "LEFT JOIN GROUP_CONNECTION_MAPPING gcm ON c.CONNECTION_ID = gcm.CONNECTION_ID AND gcm.GROUP_ID = ? " +
                        "WHERE c.STATUS = 'ACTIVE' " +
                        "ORDER BY c.CONNECTION_ID";
            return jdbcTemplate.queryForList(sql, groupId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 그룹 권한 저장 (단순화)
    @Transactional
    public boolean saveGroupPermissions(String groupId, Map<String, Object> permissions, String modifiedBy) {
        try {
            // SQL 템플릿 카테고리 권한 저장
            List<Map<String, Object>> categoryPermissions = (List<Map<String, Object>>) permissions.get("categoryPermissions");
            if (categoryPermissions != null) {
                // 기존 카테고리 권한 삭제
                String deleteCategorySql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ?";
                jdbcTemplate.update(deleteCategorySql, groupId);
                
                // 새로운 카테고리 권한 추가
                for (Map<String, Object> permission : categoryPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertCategorySql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                        jdbcTemplate.update(insertCategorySql, groupId, permission.get("categoryId"), modifiedBy);
                    }
                }
            }
            
            // 연결 정보 권한 저장
            List<Map<String, Object>> connPermissions = (List<Map<String, Object>>) permissions.get("connectionPermissions");
            if (connPermissions != null) {
                // 기존 연결 정보 권한 삭제
                String deleteConnSql = "DELETE FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ?";
                jdbcTemplate.update(deleteConnSql, groupId);
                
                // 새로운 연결 정보 권한 추가
                for (Map<String, Object> permission : connPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertConnSql = "INSERT INTO GROUP_CONNECTION_MAPPING (GROUP_ID, CONNECTION_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
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
