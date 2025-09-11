package kr.Windmill.util;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class ScreenshotHelper {
    public static void takeScreenshot(WebDriver driver, String filename) {
        try {
            File screenshotDir = new File("screenshots");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }
            File screenshot = new File(screenshotDir, filename + ".png");
            File tmp = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            tmp.renameTo(screenshot);
        } catch (Exception e) {
            System.err.println("스크린샷 촬영 실패: " + e.getMessage());
        }
    }
}
