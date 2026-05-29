package kr.Windmill.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.Windmill.util.Crypto;
import kr.Windmill.util.PasswordValidator;

@Service
public class PasswordPolicyService {

    public static final String REASON_INIT = "INIT";
    public static final String REASON_EXPIRED = "EXPIRED";

    private static final int HISTORY_LIMIT = 5;
    private static final int EXPIRE_DAYS = 90;

    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicyService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean mustChangePassword(String userId) {
        Map<String, Object> user = getUserPasswordInfo(userId);
        if (user == null) {
            return false;
        }
        return isInitPassword(user) || isPasswordExpired((Date) user.get("PASSWORD_CHANGE_DATE"));
    }

    public String resolveChangeReason(String userId) {
        Map<String, Object> user = getUserPasswordInfo(userId);
        if (user == null) {
            return null;
        }
        if (isInitPassword(user)) {
            return REASON_INIT;
        }
        if (isPasswordExpired((Date) user.get("PASSWORD_CHANGE_DATE"))) {
            return REASON_EXPIRED;
        }
        return null;
    }

    public void validateNewPassword(String userId, String rawPassword, String currentEncryptedPassword) {
        PasswordValidator.validate(rawPassword);
        assertNotReused(userId, rawPassword, currentEncryptedPassword);
    }

    public void assertNotReused(String userId, String rawPassword, String currentEncryptedPassword) {
        String newEncrypted = Crypto.crypt(rawPassword);
        if (currentEncryptedPassword != null && newEncrypted.equals(currentEncryptedPassword)) {
            throw new PasswordPolicyException("새 비밀번호는 현재 비밀번호와 같을 수 없습니다.");
        }
        List<String> recentHashes = selectRecentPasswordHashes(userId, HISTORY_LIMIT);
        for (String hash : recentHashes) {
            if (newEncrypted.equals(hash)) {
                throw new PasswordPolicyException("최근 사용한 비밀번호는 재사용할 수 없습니다.");
            }
        }
    }

    @Transactional
    public void recordPasswordHistory(String userId, String previousEncryptedPassword) {
        if (previousEncryptedPassword == null || previousEncryptedPassword.trim().isEmpty()) {
            return;
        }
        try {
            jdbcTemplate.update(
                "INSERT INTO USER_PASSWORD_HIST (USER_ID, PASSWORD_HASH, CREATED_TIMESTAMP) VALUES (?, ?, CURRENT TIMESTAMP)",
                userId, previousEncryptedPassword);
            deleteOlderThanKeep(userId, HISTORY_LIMIT);
        } catch (Exception e) {
            logger.warn("비밀번호 이력 저장 실패 (USER_PASSWORD_HIST 테이블 미존재 가능): userId={}", userId, e);
        }
    }

    public static String changeReasonText(String reason) {
        if (REASON_EXPIRED.equals(reason)) {
            return "비밀번호 사용 기간(90일)이 만료되어 변경이 필요합니다.";
        }
        return "보안 정책에 따라 비밀번호 변경이 필요합니다.";
    }

    private Map<String, Object> getUserPasswordInfo(String userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT PASSWORD, TEMP_PASSWORD, PASSWORD_CHANGE_DATE FROM USERS WHERE USER_ID = ?",
            userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean isInitPassword(Map<String, Object> user) {
        String password = (String) user.get("PASSWORD");
        String tempPassword = (String) user.get("TEMP_PASSWORD");
        return tempPassword != null && tempPassword.equals(password);
    }

    private boolean isPasswordExpired(Date passwordChangeDate) {
        if (passwordChangeDate == null) {
            return true;
        }
        long days = ChronoUnit.DAYS.between(passwordChangeDate.toLocalDate(), LocalDate.now());
        return days >= EXPIRE_DAYS;
    }

    private List<String> selectRecentPasswordHashes(String userId, int limit) {
        return jdbcTemplate.queryForList(
            "SELECT PASSWORD_HASH FROM USER_PASSWORD_HIST WHERE USER_ID = ? ORDER BY CREATED_TIMESTAMP DESC FETCH FIRST ? ROWS ONLY",
            String.class, userId, limit);
    }

    private void deleteOlderThanKeep(String userId, int keep) {
        List<Long> histIds = jdbcTemplate.queryForList(
            "SELECT HIST_ID FROM USER_PASSWORD_HIST WHERE USER_ID = ? ORDER BY CREATED_TIMESTAMP DESC",
            Long.class, userId);
        for (int i = keep; i < histIds.size(); i++) {
            jdbcTemplate.update("DELETE FROM USER_PASSWORD_HIST WHERE HIST_ID = ?", histIds.get(i));
        }
    }
}
