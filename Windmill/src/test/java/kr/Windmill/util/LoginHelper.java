package kr.Windmill.util;

import kr.Windmill.ui.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginHelper {
    public static void login(WebDriver driver, String baseUrl, String username, String password) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
        loginPage.login(username, password);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/index"));
    }
}
