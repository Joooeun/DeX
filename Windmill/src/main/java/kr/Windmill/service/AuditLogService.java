package kr.Windmill.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditLogService {

	private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 템플릿 저장 시 audit_log 기록
	 * @param templateId 템플릿 ID
	 * @param userId 사용자 ID
	 * @param actionType CREATE 또는 UPDATE
	 * @param templateData 템플릿 전체 정보 (JSON으로 직렬화)
	 * @param ipAddress IP 주소
	 * @param userAgent User-Agent
	 * @param sessionId 세션 ID
	 */
	@Transactional
	public void logTemplateSave(String templateId, String userId, String actionType, 
	                            Map<String, Object> templateData, String ipAddress, 
	                            String userAgent, String sessionId) {
		try {
			// 템플릿 데이터를 JSON으로 직렬화
			String templateJson = objectMapper.writeValueAsString(templateData);
			
			String sql = "INSERT INTO AUDIT_LOGS (" +
			             "USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "NEW_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS, CREATED_TIMESTAMP" +
			             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', CURRENT TIMESTAMP)";
			
			jdbcTemplate.update(sql,
				userId,
				actionType,
				"SQL_TEMPLATE",
				templateId,
				templateJson,
				ipAddress,
				userAgent,
				sessionId
			);
			
			logger.debug("템플릿 저장 audit_log 기록 완료: templateId={}, actionType={}", templateId, actionType);
		} catch (Exception e) {
			logger.error("템플릿 저장 audit_log 기록 실패: templateId={}, actionType={}", templateId, actionType, e);
			// audit_log 기록 실패해도 메인 로직은 계속 진행
		}
	}

	/**
	 * 템플릿 조회 시 audit_log 기록 (24시간 체크 포함)
	 * @param templateId 템플릿 ID
	 * @param userId 사용자 ID
	 * @param templateData 템플릿 전체 정보 (JSON으로 직렬화)
	 * @param ipAddress IP 주소
	 * @param userAgent User-Agent
	 * @param sessionId 세션 ID
	 * @return 기록 여부 (24시간 체크 결과)
	 */
	@Transactional
	public boolean logTemplateView(String templateId, String userId, 
	                               Map<String, Object> templateData, String ipAddress, 
	                               String userAgent, String sessionId) {
		try {
			// 마지막 조회 시간 확인 (24시간 체크)
			if (!shouldLogView(templateId, userId)) {
				logger.debug("템플릿 조회 audit_log 기록 스킵 (24시간 미경과): templateId={}, userId={}", templateId, userId);
				return false;
			}
			
			// 템플릿 데이터를 JSON으로 직렬화
			String templateJson = objectMapper.writeValueAsString(templateData);
			
			String sql = "INSERT INTO AUDIT_LOGS (" +
			             "USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "NEW_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS, CREATED_TIMESTAMP" +
			             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', CURRENT TIMESTAMP)";
			
			jdbcTemplate.update(sql,
				userId,
				"VIEW",
				"SQL_TEMPLATE",
				templateId,
				templateJson,
				ipAddress,
				userAgent,
				sessionId
			);
			
			logger.debug("템플릿 조회 audit_log 기록 완료: templateId={}, userId={}", templateId, userId);
			return true;
		} catch (Exception e) {
			logger.error("템플릿 조회 audit_log 기록 실패: templateId={}, userId={}", templateId, userId, e);
			return false;
		}
	}

	/**
	 * 템플릿 삭제 시 audit_log 기록
	 * @param templateId 템플릿 ID
	 * @param userId 사용자 ID
	 * @param templateData 삭제 전 템플릿 전체 정보 (JSON으로 직렬화)
	 * @param ipAddress IP 주소
	 * @param userAgent User-Agent
	 * @param sessionId 세션 ID
	 */
	@Transactional
	public void logTemplateDelete(String templateId, String userId, 
	                              Map<String, Object> templateData, String ipAddress, 
	                              String userAgent, String sessionId) {
		try {
			// 템플릿 데이터를 JSON으로 직렬화
			String templateJson = objectMapper.writeValueAsString(templateData);
			
			String sql = "INSERT INTO AUDIT_LOGS (" +
			             "USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "OLD_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS, CREATED_TIMESTAMP" +
			             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', CURRENT TIMESTAMP)";
			
			jdbcTemplate.update(sql,
				userId,
				"DELETE",
				"SQL_TEMPLATE",
				templateId,
				templateJson,
				ipAddress,
				userAgent,
				sessionId
			);
			
			logger.debug("템플릿 삭제 audit_log 기록 완료: templateId={}, userId={}", templateId, userId);
		} catch (Exception e) {
			logger.error("템플릿 삭제 audit_log 기록 실패: templateId={}, userId={}", templateId, userId, e);
			// audit_log 기록 실패해도 메인 로직은 계속 진행
		}
	}

	/**
	 * 템플릿 복구 시 audit_log 기록
	 * @param templateId 템플릿 ID
	 * @param userId 사용자 ID
	 * @param templateData 복구할 템플릿 전체 정보 (JSON으로 직렬화)
	 * @param restoreFromLogId 복구 출처 audit_log ID
	 * @param ipAddress IP 주소
	 * @param userAgent User-Agent
	 * @param sessionId 세션 ID
	 */
	@Transactional
	public void logTemplateRestore(String templateId, String userId, 
	                               Map<String, Object> templateData, Long restoreFromLogId,
	                               String ipAddress, String userAgent, String sessionId) {
		try {
			// 템플릿 데이터를 JSON으로 직렬화
			String templateJson = objectMapper.writeValueAsString(templateData);
			
			// 복구 출처 정보를 메시지에 포함
			String errorMessage = "복구 출처: LOG_ID=" + restoreFromLogId;
			
			String sql = "INSERT INTO AUDIT_LOGS (" +
			             "USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "NEW_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, STATUS, ERROR_MESSAGE, CREATED_TIMESTAMP" +
			             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', ?, CURRENT TIMESTAMP)";
			
			jdbcTemplate.update(sql,
				userId,
				"RESTORE",
				"SQL_TEMPLATE",
				templateId,
				templateJson,
				ipAddress,
				userAgent,
				sessionId,
				errorMessage
			);
			
			logger.debug("템플릿 복구 audit_log 기록 완료: templateId={}, restoreFromLogId={}", templateId, restoreFromLogId);
		} catch (Exception e) {
			logger.error("템플릿 복구 audit_log 기록 실패: templateId={}, restoreFromLogId={}", templateId, restoreFromLogId, e);
			// audit_log 기록 실패해도 메인 로직은 계속 진행
		}
	}

	/**
	 * 템플릿 조회 시 24시간 체크
	 * @param templateId 템플릿 ID
	 * @param userId 사용자 ID
	 * @return true: 기록 필요 (24시간 경과), false: 기록 불필요 (24시간 미경과)
	 */
	private boolean shouldLogView(String templateId, String userId) {
		try {
			// 마지막 VIEW 로그 조회
			String sql = "SELECT CREATED_TIMESTAMP FROM AUDIT_LOGS " +
			             "WHERE RESOURCE_TYPE = 'SQL_TEMPLATE' " +
			             "AND RESOURCE_ID = ? " +
			             "AND USER_ID = ? " +
			             "AND ACTION_TYPE = 'VIEW' " +
			             "ORDER BY CREATED_TIMESTAMP DESC " +
			             "FETCH FIRST 1 ROW ONLY";
			
			List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId, userId);
			
			if (results.isEmpty()) {
				// 조회 기록이 없으면 기록 필요
				return true;
			}
			
			// 마지막 조회 시간 확인
			Timestamp lastViewTime = (Timestamp) results.get(0).get("CREATED_TIMESTAMP");
			LocalDateTime lastView = lastViewTime.toLocalDateTime();
			LocalDateTime now = LocalDateTime.now();
			
			// 24시간(86400초) 경과 여부 확인
			long hoursSinceLastView = java.time.Duration.between(lastView, now).toHours();
			
			return hoursSinceLastView >= 24;
		} catch (Exception e) {
			logger.error("24시간 체크 실패: templateId={}, userId={}", templateId, userId, e);
			// 오류 시 기록하도록 함 (안전한 선택)
			return true;
		}
	}

	/**
	 * 템플릿 히스토리 조회
	 * @param templateId 템플릿 ID
	 * @return audit_log 목록
	 */
	public List<Map<String, Object>> getTemplateHistory(String templateId) {
		try {
			String sql = "SELECT LOG_ID, USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "OLD_VALUE, NEW_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, " +
			             "EXECUTION_TIME, STATUS, ERROR_MESSAGE, CREATED_TIMESTAMP " +
			             "FROM AUDIT_LOGS " +
			             "WHERE RESOURCE_TYPE = 'SQL_TEMPLATE' " +
			             "AND RESOURCE_ID = ? " +
			             "ORDER BY CREATED_TIMESTAMP DESC";
			
			return jdbcTemplate.queryForList(sql, templateId);
		} catch (Exception e) {
			logger.error("템플릿 히스토리 조회 실패: templateId={}", templateId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 특정 audit_log 조회
	 * @param logId audit_log ID
	 * @return audit_log 정보
	 */
	public Map<String, Object> getAuditLog(Long logId) {
		try {
			String sql = "SELECT LOG_ID, USER_ID, ACTION_TYPE, RESOURCE_TYPE, RESOURCE_ID, " +
			             "OLD_VALUE, NEW_VALUE, IP_ADDRESS, USER_AGENT, SESSION_ID, " +
			             "EXECUTION_TIME, STATUS, ERROR_MESSAGE, CREATED_TIMESTAMP " +
			             "FROM AUDIT_LOGS " +
			             "WHERE LOG_ID = ?";
			
			List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, logId);
			
			if (results.isEmpty()) {
				return null;
			}
			
			return results.get(0);
		} catch (Exception e) {
			logger.error("audit_log 조회 실패: logId={}", logId, e);
			return null;
		}
	}
}

