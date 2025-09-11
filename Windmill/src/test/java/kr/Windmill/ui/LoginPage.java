package kr.Windmill.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By idField = By.name("id");
    private final By passwordField = By.name("pw");
    private final By loginButton = By.cssSelector("button[type='submit']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/Login");
    }

    public void login(String username, String password) {
        WebElement idInput = wait.until(ExpectedConditions.presenceOfElementLocated(idField));
        WebElement pwInput = driver.findElement(passwordField);

        idInput.clear();
        idInput.sendKeys(username);
        pwInput.clear();
        pwInput.sendKeys(password);

        driver.findElement(loginButton).click();
    }
}
