package kr.Windmill.pages;

import org.openqa.selenium.By;

public class LoginPage {
    public static final By ID_INPUT = By.cssSelector("[data-testid='login-id']");
    public static final By PW_INPUT = By.cssSelector("[data-testid='login-password']");
    public static final By SUBMIT_BUTTON = By.cssSelector("[data-testid='login-submit']");
}
