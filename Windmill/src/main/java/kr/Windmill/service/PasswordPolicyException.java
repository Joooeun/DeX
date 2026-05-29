package kr.Windmill.service;

public class PasswordPolicyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PasswordPolicyException(String message) {
        super(message);
    }
}
