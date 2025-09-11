package kr.Windmill.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SQLTemplatePage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final By categoryList = By.id("categoryList");

    public SQLTemplatePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/SQLTemplate");
    }

    public boolean isLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(categoryList));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
