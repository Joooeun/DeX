package kr.Windmill;

import kr.Windmill.ui.SQLTemplatePage;
import kr.Windmill.util.*;
import org.junit.Assert;
import org.junit.Test;

public class SqlTemplateTests extends BaseUITest {
    @Test
    public void sqlTemplatePageLoads() {
        LoginHelper.login(driver, BASE_URL, ADMIN_ID, ADMIN_PASSWORD);
        NavigationHelper.navigateTo(driver, BASE_URL, "/SQLTemplate");
        SQLTemplatePage page = new SQLTemplatePage(driver);
        Assert.assertTrue(page.isLoaded());
        ScreenshotHelper.takeScreenshot(driver, "sql_template_page");
    }
}
