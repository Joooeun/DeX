package kr.Windmill.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    // 사용자 목록 조회 (페이징 포함, statusFilter 추가)
    public Map<String, Object> getUserList(String searchKeyword, String groupFilter, String statusFilter, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 전체 개수 조회 (중복 제거) - 데이터 조회 쿼리와 동일한 조건 사용
        StringBuilder countSqlBuilder = new StringBuilder();
        countSqlBuilder.append("SELECT COUNT(DISTINCT u.USER_ID) FROM USERS u ");
        
        List<String> whereConditions = new ArrayList<>();
        List<Object> baseParams = new ArrayList<>();
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            whereConditions.add("(u.USER_ID LIKE ? OR u.USER_NAME LIKE ?)");
            String likePattern = "%" + searchKeyword.trim() + "%";
            baseParams.add(likePattern);
            baseParams.add(likePattern);
        }
        
        // 그룹 필터가 있는 경우 EXISTS 서브쿼리 사용 (데이터 조회 쿼리와 동일)
        if (groupFilter != null && !groupFilter.trim().isEmpty()) {
            whereConditions.add("EXISTS (SELECT 1 FROM USER_GROUP_MAPPING ugm WHERE ugm.USER_ID = u.USER_ID AND ugm.GROUP_ID = ?)");
            baseParams.add(groupFilter);
        }
        
        // 상태 필터 추가
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equals(statusFilter)) {
            if ("EXPIRED".equals(statusFilter)) {
                whereConditions.add("(u.ACCOUNT_END_DATE IS NOT NULL AND u.ACCOUNT_END_DATE < CURRENT DATE)");
            } else {
                whereConditions.add("u.STATUS = ?");
                baseParams.add(statusFilter);
            }
        }
        
        if (!whereConditions.isEmpty()) {
            countSqlBuilder.append("WHERE ").append(String.join(" AND ", whereConditions));
        }
        
        int totalCount;
        if (baseParams.isEmpty()) {
            totalCount = jdbcTemplate.queryForObject(countSqlBuilder.toString(), Integer.class);
        } else {
            totalCount = jdbcTemplate.queryForObject(countSqlBuilder.toString(), baseParams.toArray(), Integer.class);
        }
        
        // 페이징된 데이터 조회 - DB2 표준 문법 사용 (중복 제거)
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM (");
        sqlBuilder.append("SELECT ROW_NUMBER() OVER (ORDER BY u.CREATED_TIMESTAMP DESC) AS RN, ");
        sqlBuilder.append("u.USER_ID, u.USER_NAME, u.STATUS, u.IP_RESTRICTION, u.LAST_LOGIN_TIMESTAMP, u.LOGIN_FAIL_COUNT, u.CREATED_TIMESTAMP, ");
        sqlBuilder.append("u.ACCOUNT_START_DATE, u.ACCOUNT_END_DATE, ");
        // 여러 그룹명을 쉼표로 구분하여 합치기 (DB2 LISTAGG 사용)
        sqlBuilder.append("(SELECT LISTAGG(ug2.GROUP_NAME, ', ') WITHIN GROUP (ORDER BY ug2.GROUP_NAME) ");
        sqlBuilder.append("FROM USER_GROUP_MAPPING ugm2 ");
        sqlBuilder.append("INNER JOIN USER_GROUPS ug2 ON ugm2.GROUP_ID = ug2.GROUP_ID ");
        sqlBuilder.append("WHERE ugm2.USER_ID = u.USER_ID) AS GROUP_NAME ");
        sqlBuilder.append("FROM USERS u ");
        
        List<Object> params = new ArrayList<>(baseParams);
        if (!whereConditions.isEmpty()) {
            sqlBuilder.append("WHERE ").append(String.join(" AND ", whereConditions));
        }
        
        sqlBuilder.append(") AS PAGED_DATA ");
        sqlBuilder.append("WHERE RN BETWEEN ? AND ?");
        
        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;
        params.add(startRow);
        params.add(endRow);
        
        List<Map<String, Object>> userList = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
        
        // 사용 기간 만료 사용자를 EXPIRED 상태로 표시 (화면 표시용)
        for (Map<String, Object> user : userList) {
            String currentStatus = (String) user.get("STATUS");
            Date accountStartDate = (Date) user.get("ACCOUNT_START_DATE");
            Date accountEndDate = (Date) user.get("ACCOUNT_END_DATE");
            
            // ACTIVE 또는 LOCKED 상태이고 사용 기간이 만료된 경우 EXPIRED로 표시
            if (("ACTIVE".equals(currentStatus) || "LOCKED".equals(currentStatus)) && !isAccountPeriodValid(accountStartDate, accountEndDate)) {
                user.put("STATUS", "EXPIRED");
            }
        }
        
        // 페이징 정보 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        result.put("userList", userList);
        result.put("totalCount", totalCount);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        return result;
    }
    
    // 기존 메서드 호환성을 위한 오버로드 (statusFilter 없이 호출)
    public Map<String, Object> getUserList(String searchKeyword, String groupFilter, int page, int pageSize) {
        return getUserList(searchKeyword, groupFilter, null, page, pageSize);
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
            
            // 사용 기간 필드 처리
            Object accountStartDateObj = userData.get("accountStartDate");
            Object accountEndDateObj = userData.get("accountEndDate");
            java.sql.Date accountStartDate = null;
            java.sql.Date accountEndDate = null;
            
            if (accountStartDateObj != null && !accountStartDateObj.toString().trim().isEmpty()) {
                accountStartDate = java.sql.Date.valueOf(accountStartDateObj.toString());
            }
            if (accountEndDateObj != null && !accountEndDateObj.toString().trim().isEmpty()) {
                accountEndDate = java.sql.Date.valueOf(accountEndDateObj.toString());
            }
            
            String sql = "INSERT INTO USERS (USER_ID, USER_NAME, PASSWORD, TEMP_PASSWORD, PASSWORD_CHANGE_DATE, STATUS, IP_RESTRICTION, EXCEL_DOWNLOAD_IP_PATTERN, ACCOUNT_START_DATE, ACCOUNT_END_DATE, CREATED_BY) VALUES (?, ?, ?, ?, CURRENT DATE, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, 
                userData.get("userId"),
                userData.get("userName"),
                encryptedPassword,  // PASSWORD
                encryptedPassword,  // TEMP_PASSWORD (동일한 값)
                userData.get("status"),
                userData.get("ipRestriction"),
                userData.get("excelDownloadIpPattern"),
                accountStartDate,   // ACCOUNT_START_DATE
                accountEndDate,     // ACCOUNT_END_DATE
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
            sqlBuilder.append("UPDATE USERS SET USER_NAME = ?, STATUS = ?, IP_RESTRICTION = ?, EXCEL_DOWNLOAD_IP_PATTERN = ?, MODIFIED_BY = ?, MODIFIED_TIMESTAMP = CURRENT TIMESTAMP");
            
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(userData.get("userName"));
            params.add(userData.get("status"));
            params.add(userData.get("ipRestriction"));
            params.add(userData.get("excelDownloadIpPattern"));
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
            
            // 사용 기간 필드 처리
            Object accountStartDateObj = userData.get("accountStartDate");
            Object accountEndDateObj = userData.get("accountEndDate");
            java.sql.Date accountStartDate = null;
            java.sql.Date accountEndDate = null;
            
            if (accountStartDateObj != null && !accountStartDateObj.toString().trim().isEmpty()) {
                accountStartDate = java.sql.Date.valueOf(accountStartDateObj.toString());
            }
            if (accountEndDateObj != null && !accountEndDateObj.toString().trim().isEmpty()) {
                accountEndDate = java.sql.Date.valueOf(accountEndDateObj.toString());
            }
            
            sqlBuilder.append(", ACCOUNT_START_DATE = ?, ACCOUNT_END_DATE = ?");
            params.add(accountStartDate);
            params.add(accountEndDate);
            
            sqlBuilder.append(" WHERE USER_ID = ?");
            params.add(userId);
            
            jdbcTemplate.update(sqlBuilder.toString(), params.toArray());
            
            // 사용 기간 변경 시 LOCKED 상태이고 사용 기간이 유효하면 ACTIVE로 자동 변경
            String currentStatusSql = "SELECT STATUS FROM USERS WHERE USER_ID = ?";
            String currentStatus = jdbcTemplate.queryForObject(currentStatusSql, String.class, userId);
            
            if ("LOCKED".equals(currentStatus)) {
                // 사용 기간이 유효한지 확인
                if (isAccountPeriodValid(accountStartDate, accountEndDate)) {
                    String activateSql = "UPDATE USERS SET STATUS = 'ACTIVE' WHERE USER_ID = ?";
                    jdbcTemplate.update(activateSql, userId);
                    logger.info("사용자 계정 사용 기간 변경으로 자동 활성화: {}", userId);
                }
            }
            
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
            // 1. 사용자 존재 여부 및 모든 필요한 정보를 한 번에 조회 (서버 부담 최소화)
            // 검증 순서: 존재 확인 → 상태 확인 → 사용 기간 확인 → IP 제한 → 비밀번호 확인 (가장 무거운 연산은 마지막)
            String checkSql = "SELECT STATUS, LOGIN_FAIL_COUNT, ACCOUNT_START_DATE, ACCOUNT_END_DATE, IP_RESTRICTION, PASSWORD, TEMP_PASSWORD FROM USERS WHERE USER_ID = ?";
            List<Map<String, Object>> userInfo = jdbcTemplate.queryForList(checkSql, userId);
            
            if (userInfo.isEmpty()) {
                // 존재하지 않는 사용자 로그인 시도 로그 (상세 정보 기록)
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, "존재하지 않는 사용자 ID");
                
                result.put("success", false);
                result.put("message", "계정정보가 일치하지 않습니다.");
                return result;
            }
            
            Map<String, Object> user = userInfo.get(0);
            String status = (String) user.get("STATUS");
            Integer failCount = (Integer) user.get("LOGIN_FAIL_COUNT");
            Date accountStartDate = (Date) user.get("ACCOUNT_START_DATE");
            Date accountEndDate = (Date) user.get("ACCOUNT_END_DATE");
            String ipRestriction = (String) user.get("IP_RESTRICTION");
            String storedPassword = (String) user.get("PASSWORD");
            String tempPassword = (String) user.get("TEMP_PASSWORD");
            
            // 2. 사용 기간 확인 및 자동 업데이트 (계정 상태 확인 전에 먼저 확인)
            // 사용 기간이 만료되었으면 STATUS를 LOCKED로 업데이트
            boolean isExpired = checkAndUpdateAccountExpiration(userId, accountStartDate, accountEndDate);
            
            // STATUS가 업데이트되었을 수 있으므로 다시 조회
            if (isExpired) {
                String statusCheckSql = "SELECT STATUS FROM USERS WHERE USER_ID = ?";
                status = jdbcTemplate.queryForObject(statusCheckSql, String.class, userId);
            }
            
            // 3. 계정 상태 확인 (가벼운 검증 - 조회한 데이터에서 확인)
            if (!"ACTIVE".equals(status)) {
                String userMessage = "";
                String logMessage = ""; // 로그에 기록할 상세 메시지
                
                if ("LOCKED".equals(status)) {
                    // LOCKED 상태인 경우 사용 기간 만료인지 확인
                    if (isExpired || !isAccountPeriodValid(accountStartDate, accountEndDate)) {
                        userMessage = "계정 사용 기간이 만료되었습니다. 관리자에게 문의하세요.";
                        logMessage = "계정 사용 기간 만료";
                        if (accountStartDate != null && accountEndDate != null) {
                            logMessage += " (시작일: " + accountStartDate + ", 종료일: " + accountEndDate + ")";
                        } else if (accountEndDate != null) {
                            logMessage += " (종료일: " + accountEndDate + ")";
                        } else if (accountStartDate != null) {
                            logMessage += " (시작일: " + accountStartDate + ", 아직 시작 전)";
                        }
                    } else {
                        userMessage = "로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의하세요.";
                        logMessage = "계정 잠금 상태";
                    }
                } else if ("INACTIVE".equals(status)) {
                    userMessage = "비활성화된 계정입니다.";
                    logMessage = "계정 비활성화 상태";
                } else {
                    userMessage = "계정 상태 이상입니다. 관리자에게 문의하세요.";
                    logMessage = "계정 상태 이상 (상태: " + status + ")";
                }
                
                // 계정 상태 문제 로그인 시도 로그 (상세 정보 기록)
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, logMessage);
                
                result.put("success", false);
                result.put("message", userMessage);
                return result;
            }
            
            // 4. 사용 기간 만료 확인 (ACTIVE 상태이지만 사용 기간이 만료된 경우 - 이중 체크)
            if (!isAccountPeriodValid(accountStartDate, accountEndDate)) {
                // 사용 기간 만료로 인한 로그인 실패 로그
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                String logMessage = "계정 사용 기간 만료";
                if (accountStartDate != null && accountEndDate != null) {
                    logMessage += " (시작일: " + accountStartDate + ", 종료일: " + accountEndDate + ")";
                } else if (accountEndDate != null) {
                    logMessage += " (종료일: " + accountEndDate + ")";
                } else if (accountStartDate != null) {
                    logMessage += " (시작일: " + accountStartDate + ", 아직 시작 전)";
                }
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, logMessage);
                
                result.put("success", false);
                result.put("message", "계정 사용 기간이 만료되었습니다. 관리자에게 문의하세요.");
                return result;
            }
            
            // 4. IP 제한 확인 (중간 부담 - JSON 파싱)
            if (!isIpAllowed(userId, ipAddress, ipRestriction)) {
                String userMessage = "계정정보가 일치하지 않습니다."; // 사용자에게 보여줄 메시지
                String logMessage = "IP 제한 위반 (접속 IP: " + ipAddress + ")"; // 로그에 기록할 상세 메시지
                
                // IP 제한 위반 로그인 시도 로그 (상세 정보 기록)
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, logMessage);
                
                result.put("success", false);
                result.put("message", userMessage);
                return result;
            }
            
            // 5. 비밀번호 확인 (가장 무거운 연산 - 암호화 연산은 마지막에)
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
                
                String userMessage = "계정정보가 일치하지 않습니다."; // 사용자에게 보여줄 메시지
                String logMessage = "비밀번호 불일치"; // 로그에 기록할 상세 메시지
                
                // 5번 이상 실패하면 계정 잠금
                if (currentFailCount != null && currentFailCount >= 5) {
                    String lockSql = "UPDATE USERS SET STATUS = 'LOCKED' WHERE USER_ID = ?";
                    jdbcTemplate.update(lockSql, userId);
                    userMessage = "로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의하세요.";
                    logMessage = "비밀번호 불일치 - 계정 잠금 (실패 횟수: " + currentFailCount + ")";
                } else {
                    logMessage = "비밀번호 불일치 (실패 횟수: " + currentFailCount + ")";
                }
                
                // 비밀번호 불일치 로그인 시도 로그 (상세 정보 기록)
                String auditSql = "INSERT INTO AUDIT_LOGS (USER_ID, ACTION_TYPE, RESOURCE_TYPE, IP_ADDRESS, USER_AGENT, STATUS, ERROR_MESSAGE) VALUES (?, 'LOGIN', 'USER', ?, ?, 'FAIL', ?)";
                jdbcTemplate.update(auditSql, userId, ipAddress, userAgent, logMessage);
                
                result.put("success", false);
                result.put("message", userMessage);
                return result;
            }
            
            // 로그인 성공 처리 - 나노초 + 랜덤으로 중복 방지
            String sessionId = "SESS_" + System.nanoTime() + "_" + (int)(Math.random() * 1000) + "_" + userId;
            
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
    
    // 사용자의 그룹 목록 조회 (다중 그룹 지원)
    public List<Map<String, Object>> getUserGroups(String userId) {
        String sql = "SELECT ugm.GROUP_ID, ug.GROUP_NAME, ug.GROUP_DESCRIPTION " +
                    "FROM USER_GROUP_MAPPING ugm " +
                    "INNER JOIN USER_GROUPS ug ON ugm.GROUP_ID = ug.GROUP_ID " +
                    "WHERE ugm.USER_ID = ? " +
                    "ORDER BY ug.GROUP_NAME";
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    // 사용자 그룹 매핑 (단일 그룹) - 하위 호환성 유지
    @Transactional
    public boolean assignUserToGroup(String userId, String groupId, String assignedBy) {
        return assignUserToGroups(userId, Collections.singletonList(groupId), assignedBy);
    }
    
    // 사용자 그룹 매핑 (다중 그룹)
    @Transactional
    public boolean assignUserToGroups(String userId, List<String> groupIds, String assignedBy) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                logger.error("사용자 ID가 없습니다.");
                return false;
            }
            
            if (groupIds == null || groupIds.isEmpty()) {
                // 그룹이 없으면 기존 매핑만 삭제
                String deleteSql = "DELETE FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
                jdbcTemplate.update(deleteSql, userId);
                return true;
            }
            
            // 기존 그룹 매핑 삭제
            String deleteSql = "DELETE FROM USER_GROUP_MAPPING WHERE USER_ID = ?";
            jdbcTemplate.update(deleteSql, userId);
            
            // 새로운 그룹 매핑 추가 (중복 제거)
            Set<String> uniqueGroupIds = new HashSet<>(groupIds);
            String insertSql = "INSERT INTO USER_GROUP_MAPPING (USER_ID, GROUP_ID, ASSIGNED_BY, ASSIGNED_TIMESTAMP) VALUES (?, ?, ?, CURRENT TIMESTAMP)";
            for (String groupId : uniqueGroupIds) {
                if (groupId != null && !groupId.trim().isEmpty()) {
                    jdbcTemplate.update(insertSql, userId, groupId, assignedBy);
                }
            }
            
            logger.debug("사용자 그룹 매핑 완료: userId={}, groupIds={}", userId, uniqueGroupIds);
            return true;
        } catch (Exception e) {
            logger.error("사용자 그룹 매핑 중 오류 발생: userId={}, groupIds={}", userId, groupIds, e);
            return false;
        }
    }
    
    // SQL 템플릿 카테고리 권한 조회 (다중 그룹 지원)
    public List<Map<String, Object>> getSqlTemplateCategoryPermissions(String userId) {
        // DISTINCT를 사용하여 여러 그룹에 속한 경우 중복 제거
        String sql = "SELECT DISTINCT gcm.CATEGORY_ID, stc.CATEGORY_NAME, stc.CATEGORY_DESCRIPTION " +
                    "FROM GROUP_CATEGORY_MAPPING gcm " +
                    "INNER JOIN USER_GROUP_MAPPING ugm ON gcm.GROUP_ID = ugm.GROUP_ID " +
                    "LEFT JOIN SQL_TEMPLATE_CATEGORY stc ON gcm.CATEGORY_ID = stc.CATEGORY_ID " +
                    "WHERE ugm.USER_ID = ? " +
                    "ORDER BY stc.CATEGORY_ORDER, stc.CATEGORY_NAME";
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    // 연결 정보 권한 조회 (다중 그룹 지원)
    public List<Map<String, Object>> getConnectionPermissions(String userId) {
        // DISTINCT를 사용하여 여러 그룹에 속한 경우 중복 제거
        String sql = "SELECT DISTINCT gcm.CONNECTION_ID, dc.DB_TYPE " +
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
    
    // 사용 기간 검증 (Date 객체를 받아서 검증)
    private boolean isAccountPeriodValid(Date accountStartDate, Date accountEndDate) {
        // 둘 다 NULL이면 기간 제한 없음
        if (accountStartDate == null && accountEndDate == null) {
            return true;
        }
        
        java.time.LocalDate today = java.time.LocalDate.now();
        
        // 시작일 체크
        if (accountStartDate != null) {
            java.time.LocalDate start = ((java.sql.Date) accountStartDate).toLocalDate();
            if (today.isBefore(start)) {
                return false; // 아직 시작일 전
            }
        }
        
        // 종료일 체크
        if (accountEndDate != null) {
            java.time.LocalDate end = ((java.sql.Date) accountEndDate).toLocalDate();
            if (today.isAfter(end)) {
                return false; // 종료일 지남
            }
        }
        
        return true;
    }
    
    // 사용 기간 만료 확인 및 자동 업데이트
    private boolean checkAndUpdateAccountExpiration(String userId, Date accountStartDate, Date accountEndDate) {
        // 둘 다 NULL이면 기간 제한 없음
        if (accountStartDate == null && accountEndDate == null) {
            return false;
        }
        
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean isExpired = false;
        
        // 종료일 체크
        if (accountEndDate != null) {
            java.time.LocalDate end = ((java.sql.Date) accountEndDate).toLocalDate();
            if (today.isAfter(end)) {
                isExpired = true;
            }
        }
        
        // 시작일 체크 (아직 시작 전이면 만료는 아니지만 사용 불가)
        if (accountStartDate != null && !isExpired) {
            java.time.LocalDate start = ((java.sql.Date) accountStartDate).toLocalDate();
            if (today.isBefore(start)) {
                // 시작일 전이면 아직 사용 불가 (하지만 만료는 아님)
                return false;
            }
        }
        
        // 만료되었으면 STATUS를 LOCKED로 업데이트
        if (isExpired) {
            try {
                String updateSql = "UPDATE USERS SET STATUS = 'LOCKED' WHERE USER_ID = ? AND STATUS = 'ACTIVE'";
                int updated = jdbcTemplate.update(updateSql, userId);
                if (updated > 0) {
                    logger.info("사용자 계정 사용 기간 만료로 자동 잠금: {}", userId);
                }
            } catch (Exception e) {
                logger.error("사용자 계정 사용 기간 만료 업데이트 중 오류 발생: {}", userId, e);
            }
        }

        return isExpired;
    }
    
    // IP 제한 검증 (ipRestriction 파라미터 추가로 DB 조회 제거)
    private boolean isIpAllowed(String userId, String clientIp, String ipRestriction) {
        try {
            // IP 제한이 설정되지 않은 경우 모든 IP 허용
            if (ipRestriction == null || ipRestriction.trim().isEmpty()) {
                return true;
            }
            
            // IP 제한 목록을 쉼표로 분리
            String[] allowedIps = ipRestriction.split(",");
            
            for (String allowedIp : allowedIps) {
                allowedIp = allowedIp.trim();
                
                // 와일드카드가 있는 경우 패턴 매칭
                if (allowedIp.contains("*")) {
                    if (matchesWildcardPattern(clientIp, allowedIp)) {
                        return true;
                    }
                } else {
                    // 정확한 IP 매칭
                    if (clientIp.equals(allowedIp)) {
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("IP 제한 검증 중 오류 발생", e);
            return false; // 오류 시 보안을 위해 접근 차단
        }
    }
    
    // IP 제한 검증 (기존 메서드 - 호환성 유지)
    public boolean isIpAllowed(String userId, String clientIp) {
        try {
            // 사용자의 IP 제한 정보 조회
            String sql = "SELECT IP_RESTRICTION FROM USERS WHERE USER_ID = ?";
            String ipRestriction = jdbcTemplate.queryForObject(sql, String.class, userId);
            return isIpAllowed(userId, clientIp, ipRestriction);
        } catch (Exception e) {
            logger.error("IP 제한 검증 중 오류 발생", e);
            return false; // 오류 시 보안을 위해 접근 차단
        }
    }
    
    // 와일드카드 패턴 매칭 (예: 192.168.1.*)
    private boolean matchesWildcardPattern(String clientIp, String pattern) {
        try {
            // 패턴을 정규식으로 변환 (* -> .*)
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return clientIp.matches(regex);
        } catch (Exception e) {
            logger.error("와일드카드 패턴 매칭 중 오류 발생", e);
            return false;
        }
    }
}
