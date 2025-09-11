package kr.Windmill.util;

import org.openqa.selenium.WebDriver;

public class NavigationHelper {
    public static void navigateTo(WebDriver driver, String baseUrl, String path) {
        driver.get(baseUrl + path);
    }
}
