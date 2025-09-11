package kr.Windmill.util;

import kr.Windmill.ui.LoginPage;
import org.openqa.selenium.WebDriver;

public class LoginHelper {
    public static void login(WebDriver driver, String baseUrl, String username, String password) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
        loginPage.login(username, password);
    }
}
