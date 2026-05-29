package kr.Windmill.util;

import java.util.regex.Pattern;

import kr.Windmill.service.PasswordPolicyException;

/**
 * 비밀번호 복잡도 검증 (8~20자, 영문·숫자·특수문자 각 1개 이상)
 */
public final class PasswordValidator {

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;

    private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[<>\"'|\\\\]");
    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordValidator() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new PasswordPolicyException("비밀번호는 필수입니다.");
        }
        if (rawPassword.length() < PASSWORD_MIN_LENGTH || rawPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new PasswordPolicyException(
                String.format("비밀번호는 %d~%d자여야 합니다.", PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH));
        }
        if (FORBIDDEN_CHARS.matcher(rawPassword).find()) {
            throw new PasswordPolicyException("비밀번호에 허용되지 않는 문자(< > \" ' \\ |)가 포함되어 있습니다.");
        }
        if (!HAS_LETTER.matcher(rawPassword).find()
            || !HAS_DIGIT.matcher(rawPassword).find()
            || !HAS_SPECIAL.matcher(rawPassword).find()) {
            throw new PasswordPolicyException("비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.");
        }
    }
}
