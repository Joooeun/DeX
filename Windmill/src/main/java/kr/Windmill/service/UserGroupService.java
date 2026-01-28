package kr.Windmill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserGroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserGroupService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
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
    
    // 그룹명 중복 체크
    public boolean isGroupNameExists(String groupName, String excludeGroupId) {
        try {
            String sql;
            if (excludeGroupId != null && !excludeGroupId.trim().isEmpty()) {
                sql = "SELECT COUNT(*) FROM USER_GROUPS WHERE GROUP_NAME = ? AND GROUP_ID != ?";
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupName, excludeGroupId);
                return count != null && count > 0;
            } else {
                sql = "SELECT COUNT(*) FROM USER_GROUPS WHERE GROUP_NAME = ?";
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupName);
                return count != null && count > 0;
            }
        } catch (Exception e) {
            logger.error("그룹명 중복 체크 중 오류 발생", e);
            return false;
        }
    }
    
    // 그룹 생성
    @Transactional
    public boolean createGroup(Map<String, Object> groupData) {
        try {
            String groupName = (String) groupData.get("groupName");
            if (groupName == null || groupName.trim().isEmpty()) {
                logger.error("그룹명이 없습니다.");
                return false;
            }
            
            // 그룹명 중복 체크
            if (isGroupNameExists(groupName, null)) {
                logger.error("그룹명이 이미 존재합니다: {}", groupName);
                return false;
            }
            
            // groupId가 없으면 자동 생성
            String groupId = (String) groupData.get("groupId");
            if (groupId == null || groupId.trim().isEmpty()) {
                groupId = "GROUP_" + System.currentTimeMillis();
            }
            
            String sql = "INSERT INTO USER_GROUPS (GROUP_ID, GROUP_NAME, GROUP_DESCRIPTION, STATUS, CREATED_BY) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                groupId,
                groupName,
                groupData.get("description"),
                groupData.get("status"),
                groupData.get("createdBy")
            );
            return true;
        } catch (Exception e) {
            logger.error("그룹 생성 실패", e);
            return false;
        }
    }
    
    // 그룹 수정
    @Transactional
    public boolean updateGroup(String groupId, Map<String, Object> groupData) {
        try {
            String groupName = (String) groupData.get("groupName");
            if (groupName == null || groupName.trim().isEmpty()) {
                logger.error("그룹명이 없습니다.");
                return false;
            }
            
            // 그룹명 중복 체크 (자기 자신 제외)
            if (isGroupNameExists(groupName, groupId)) {
                logger.error("그룹명이 이미 존재합니다: {}", groupName);
                return false;
            }
            
            String sql = "UPDATE USER_GROUPS SET GROUP_NAME = ?, GROUP_DESCRIPTION = ?, STATUS = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP WHERE GROUP_ID = ?";
            jdbcTemplate.update(sql,
                groupName,
                groupData.get("description"),
                groupData.get("status"),
                groupData.get("modifiedBy"),
                groupId
            );
            return true;
        } catch (Exception e) {
            logger.error("그룹 수정 실패: {}", groupId, e);
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
            logger.error("그룹 삭제 실패: {}", groupId, e);
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
            logger.error("사용자 그룹 추가 중 오류 발생", e);
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
            logger.error("사용자 그룹 제거 중 오류 발생", e);
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
            logger.error("SQL 템플릿 카테고리 권한 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    // 연결 정보 권한 조회
    public List<Map<String, Object>> getConnectionPermissions(String groupId) {
        try {
            String sql =
                "SELECT c.CONNECTION_ID, c.DB_TYPE, " +
                "CASE WHEN gcm.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                "FROM DATABASE_CONNECTION c " +
                "LEFT JOIN GROUP_CONNECTION_MAPPING gcm ON c.CONNECTION_ID = gcm.CONNECTION_ID AND gcm.GROUP_ID = ? " +
                "UNION ALL " +
                "SELECT s.SFTP_CONNECTION_ID AS CONNECTION_ID, NULL AS DB_TYPE, " +
                "CASE WHEN gcm2.GROUP_ID IS NOT NULL THEN 1 ELSE 0 END AS HAS_PERMISSION " +
                "FROM SFTP_CONNECTION s " +
                "LEFT JOIN GROUP_CONNECTION_MAPPING gcm2 ON s.SFTP_CONNECTION_ID = gcm2.CONNECTION_ID AND gcm2.GROUP_ID = ? " +
                "ORDER BY CONNECTION_ID";
            return jdbcTemplate.queryForList(sql, groupId, groupId);
        } catch (Exception e) {
            logger.error("연결 정보 권한 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    // 메뉴 권한 조회 (기존 GROUP_CATEGORY_MAPPING 테이블 활용)
    public List<Map<String, Object>> getMenuPermissions(String groupId) {
        try {
            // 기본 메뉴 목록 정의
            List<Map<String, Object>> allMenus = new ArrayList<>();
            
            // 대시보드 권한 추가 (기존 MENU_DASHBOARD 유지)
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("MENU_ID", "MENU_DASHBOARD");
            dashboard.put("MENU_NAME", "대시보드");
            dashboard.put("MENU_DESCRIPTION", "대시보드 조회 권한");
            allMenus.add(dashboard);
            
            // 모니터링 템플릿 설정 조회
            String monitoringConfig = systemConfigService.getDashboardMonitoringTemplateConfig();
            if (monitoringConfig != null && !monitoringConfig.trim().isEmpty() && !monitoringConfig.equals("{}")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = mapper.readValue(monitoringConfig, Map.class);
                    
                    // 현재는 단일 템플릿 구조, 향후 여러개 지원 가능하도록 처리
                    List<Map<String, Object>> templates = new ArrayList<>();
                    
                    // 단일 템플릿 구조인 경우
                    if (config.containsKey("templateId")) {
                        templates.add(config);
                    }
                    // 향후 여러개 구조인 경우
                    else if (config.containsKey("templates") && config.get("templates") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> templateList = (List<Map<String, Object>>) config.get("templates");
                        templates.addAll(templateList);
                    }
                    
                    // 각 템플릿마다 권한 추가
                    for (Map<String, Object> templateConfig : templates) {
                        String templateId = (String) templateConfig.get("templateId");
                        String templateName = (String) templateConfig.get("templateName");
                        
                        if (templateId != null && !templateId.trim().isEmpty()) {
                            Map<String, Object> dashboardMonitoring = new HashMap<>();
                            String menuId = "MENU_DASHBOARD_MONITORING_" + templateId;
                            dashboardMonitoring.put("MENU_ID", menuId);
                            dashboardMonitoring.put("MENU_NAME",  (templateName != null ? templateName : templateId));
                            dashboardMonitoring.put("MENU_DESCRIPTION", "");
                            dashboardMonitoring.put("DEPENDS_ON", "MENU_DASHBOARD"); // 의존성 표시
                            dashboardMonitoring.put("TEMPLATE_ID", templateId); // 템플릿 ID 저장
                            allMenus.add(dashboardMonitoring);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("모니터링 템플릿 설정 파싱 실패", e);
                }
            }
            
            Map<String, Object> fileRead = new HashMap<>();
            fileRead.put("MENU_ID", "MENU_FILE_READ");
            fileRead.put("MENU_NAME", "파일 읽기");
            fileRead.put("MENU_DESCRIPTION", "");
            allMenus.add(fileRead);
            
            Map<String, Object> fileWrite = new HashMap<>();
            fileWrite.put("MENU_ID", "MENU_FILE_WRITE");
            fileWrite.put("MENU_NAME", "파일 쓰기");
            fileWrite.put("MENU_DESCRIPTION", "");
            allMenus.add(fileWrite);
                        
            // 현재 그룹의 메뉴 권한 조회 (기존 테이블 활용)
            String sql = "SELECT CATEGORY_ID FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID LIKE 'MENU_%'";
            List<String> grantedMenus = jdbcTemplate.queryForList(sql, String.class, groupId);
            
            // 권한 상태 설정 (기본적으로 권한 없음)
            for (Map<String, Object> menu : allMenus) {
                String menuId = (String) menu.get("MENU_ID");
                boolean hasPermission = grantedMenus.contains(menuId);
                menu.put("HAS_PERMISSION", hasPermission); // 기본값 false, 권한이 있으면 true
            }
            
            return allMenus;
        } catch (Exception e) {
            logger.error("메뉴 권한 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    // 그룹 권한 저장 (단순화)
    @Transactional
    @SuppressWarnings("unchecked")
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
            
            // 메뉴 권한 저장
            List<Map<String, Object>> menuPermissions = (List<Map<String, Object>>) permissions.get("menuPermissions");
            if (menuPermissions != null) {
                logger.info("메뉴 권한 저장 시작 - groupId: {}, menuCount: {}", groupId, menuPermissions.size());
                
                // 기존 메뉴 권한 삭제 (MENU_로 시작하는 것만)
                String deleteMenuSql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID LIKE 'MENU_%'";
                int deletedCount = jdbcTemplate.update(deleteMenuSql, groupId);
                logger.info("기존 메뉴 권한 삭제 완료 - groupId: {}, deletedCount: {}", groupId, deletedCount);
                
                // 새로운 메뉴 권한 추가
                for (Map<String, Object> permission : menuPermissions) {
                    if ((Boolean) permission.get("hasPermission")) {
                        String insertMenuSql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                        try {
                            jdbcTemplate.update(insertMenuSql, groupId, permission.get("menuId"), modifiedBy);
                            logger.info("메뉴 권한 추가 성공 - groupId: {}, menuId: {}", groupId, permission.get("menuId"));
                        } catch (Exception e) {
                            logger.error("메뉴 권한 추가 실패 - groupId: {}, menuId: {}", groupId, permission.get("menuId"), e);
                            throw new RuntimeException("메뉴 권한 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
                        }
                    }
                }
                logger.info("메뉴 권한 저장 완료 - groupId: {}", groupId);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("그룹 권한 저장 중 오류 발생", e);
            return false;
        }
    }
    
    // 비교 기반 메뉴 권한 저장 (DELETE 후 INSERT 대신 추가/삭제만 수행)
    @Transactional
    public boolean saveMenuPermissions(String groupId, List<String> newMenuIds, String modifiedBy) {
        try {
            logger.info("메뉴 권한 저장 시작 - groupId: {}, newMenuCount: {}", groupId, newMenuIds != null ? newMenuIds.size() : 0);
            
            // 중복 제거
            Set<String> newMenuSet = new HashSet<>();
            if (newMenuIds != null) {
                for (String menuId : newMenuIds) {
                    if (menuId != null && !menuId.trim().isEmpty()) {
                        newMenuSet.add(menuId.trim());
                    }
                }
            }
            
            // 기존 메뉴 권한 조회
            String selectSql = "SELECT CATEGORY_ID FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID LIKE 'MENU_%'";
            List<String> existingMenuIds = jdbcTemplate.queryForList(selectSql, String.class, groupId);
            Set<String> existingMenuSet = new HashSet<>(existingMenuIds);
            
            // 삭제할 권한 (기존에는 있지만 새로는 없는 것)
            Set<String> toDelete = new HashSet<>(existingMenuSet);
            toDelete.removeAll(newMenuSet);
            
            // 추가할 권한 (새로는 있지만 기존에는 없는 것)
            Set<String> toAdd = new HashSet<>(newMenuSet);
            toAdd.removeAll(existingMenuSet);
            
            logger.info("메뉴 권한 변경 내역 - 추가: {}, 삭제: {}", toAdd.size(), toDelete.size());
            
            // 삭제 수행
            if (!toDelete.isEmpty()) {
                String deleteSql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID = ?";
                for (String menuId : toDelete) {
                    jdbcTemplate.update(deleteSql, groupId, menuId);
                    logger.info("메뉴 권한 삭제 - groupId: {}, menuId: {}", groupId, menuId);
                }
            }
            
            // 추가 수행
            if (!toAdd.isEmpty()) {
                String insertSql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                for (String menuId : toAdd) {
                    jdbcTemplate.update(insertSql, groupId, menuId, modifiedBy);
                    logger.info("메뉴 권한 추가 - groupId: {}, menuId: {}", groupId, menuId);
                }
            }
            
            logger.info("메뉴 권한 저장 완료 - groupId: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("메뉴 권한 저장 중 오류 발생 - groupId: {}", groupId, e);
            throw new RuntimeException("메뉴 권한 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    // 비교 기반 카테고리 권한 저장
    @Transactional
    public boolean saveCategoryPermissions(String groupId, List<String> newCategoryIds, String modifiedBy) {
        try {
            logger.info("카테고리 권한 저장 시작 - groupId: {}, newCategoryCount: {}", groupId, newCategoryIds != null ? newCategoryIds.size() : 0);
            
            // 중복 제거
            Set<String> newCategorySet = new HashSet<>();
            if (newCategoryIds != null) {
                for (String categoryId : newCategoryIds) {
                    if (categoryId != null && !categoryId.trim().isEmpty() && !categoryId.startsWith("MENU_")) {
                        newCategorySet.add(categoryId.trim());
                    }
                }
            }
            
            // 기존 카테고리 권한 조회 (MENU_로 시작하는 것 제외)
            String selectSql = "SELECT CATEGORY_ID FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID NOT LIKE 'MENU_%'";
            List<String> existingCategoryIds = jdbcTemplate.queryForList(selectSql, String.class, groupId);
            Set<String> existingCategorySet = new HashSet<>(existingCategoryIds);
            
            // 삭제할 권한
            Set<String> toDelete = new HashSet<>(existingCategorySet);
            toDelete.removeAll(newCategorySet);
            
            // 추가할 권한
            Set<String> toAdd = new HashSet<>(newCategorySet);
            toAdd.removeAll(existingCategorySet);
            
            logger.info("카테고리 권한 변경 내역 - 추가: {}, 삭제: {}", toAdd.size(), toDelete.size());
            
            // 삭제 수행
            if (!toDelete.isEmpty()) {
                String deleteSql = "DELETE FROM GROUP_CATEGORY_MAPPING WHERE GROUP_ID = ? AND CATEGORY_ID = ?";
                for (String categoryId : toDelete) {
                    jdbcTemplate.update(deleteSql, groupId, categoryId);
                }
            }
            
            // 추가 수행
            if (!toAdd.isEmpty()) {
                String insertSql = "INSERT INTO GROUP_CATEGORY_MAPPING (GROUP_ID, CATEGORY_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                for (String categoryId : toAdd) {
                    jdbcTemplate.update(insertSql, groupId, categoryId, modifiedBy);
                }
            }
            
            logger.info("카테고리 권한 저장 완료 - groupId: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("카테고리 권한 저장 중 오류 발생 - groupId: {}", groupId, e);
            throw new RuntimeException("카테고리 권한 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    // 비교 기반 연결정보 권한 저장
    @Transactional
    public boolean saveConnectionPermissions(String groupId, List<String> newConnectionIds, String modifiedBy) {
        try {
            logger.info("연결정보 권한 저장 시작 - groupId: {}, newConnectionCount: {}", groupId, newConnectionIds != null ? newConnectionIds.size() : 0);
            
            // 중복 제거
            Set<String> newConnectionSet = new HashSet<>();
            if (newConnectionIds != null) {
                for (String connectionId : newConnectionIds) {
                    if (connectionId != null && !connectionId.trim().isEmpty()) {
                        newConnectionSet.add(connectionId.trim());
                    }
                }
            }
            
            // 기존 연결정보 권한 조회
            String selectSql = "SELECT CONNECTION_ID FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ?";
            List<String> existingConnectionIds = jdbcTemplate.queryForList(selectSql, String.class, groupId);
            Set<String> existingConnectionSet = new HashSet<>(existingConnectionIds);
            
            // 삭제할 권한
            Set<String> toDelete = new HashSet<>(existingConnectionSet);
            toDelete.removeAll(newConnectionSet);
            
            // 추가할 권한
            Set<String> toAdd = new HashSet<>(newConnectionSet);
            toAdd.removeAll(existingConnectionSet);
            
            logger.info("연결정보 권한 변경 내역 - 추가: {}, 삭제: {}", toAdd.size(), toDelete.size());
            
            // 삭제 수행
            if (!toDelete.isEmpty()) {
                String deleteSql = "DELETE FROM GROUP_CONNECTION_MAPPING WHERE GROUP_ID = ? AND CONNECTION_ID = ?";
                for (String connectionId : toDelete) {
                    jdbcTemplate.update(deleteSql, groupId, connectionId);
                }
            }
            
            // 추가 수행
            if (!toAdd.isEmpty()) {
                String insertSql = "INSERT INTO GROUP_CONNECTION_MAPPING (GROUP_ID, CONNECTION_ID, GRANTED_BY, GRANTED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
                for (String connectionId : toAdd) {
                    jdbcTemplate.update(insertSql, groupId, connectionId, modifiedBy);
                }
            }
            
            logger.info("연결정보 권한 저장 완료 - groupId: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("연결정보 권한 저장 중 오류 발생 - groupId: {}", groupId, e);
            throw new RuntimeException("연결정보 권한 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    // 통합 권한 저장 (메뉴, 카테고리, 연결정보를 한 번에)
    @Transactional
    public boolean saveAllPermissions(String groupId, List<String> menuIds, List<String> categoryIds, List<String> connectionIds, String modifiedBy) {
        try {
            logger.info("통합 권한 저장 시작 - groupId: {}", groupId);
            
            // 메뉴 권한 저장
            if (menuIds != null) {
                saveMenuPermissions(groupId, menuIds, modifiedBy);
            }
            
            // 카테고리 권한 저장
            if (categoryIds != null) {
                saveCategoryPermissions(groupId, categoryIds, modifiedBy);
            }
            
            // 연결정보 권한 저장
            if (connectionIds != null) {
                saveConnectionPermissions(groupId, connectionIds, modifiedBy);
            }
            
            logger.info("통합 권한 저장 완료 - groupId: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("통합 권한 저장 중 오류 발생 - groupId: {}", groupId, e);
            throw e;
        }
    }
}
