package kr.Windmill;

import kr.Windmill.ui.DashboardPage;
import kr.Windmill.util.BaseUITest;
import kr.Windmill.util.LoginHelper;
import org.junit.Assert;
import org.junit.Test;

public class LoginTests extends BaseUITest {
    @Test
    public void loginSucceeds() {
        LoginHelper.login(driver, BASE_URL, ADMIN_ID, ADMIN_PASSWORD);
        DashboardPage dashboard = new DashboardPage(driver);
        Assert.assertTrue(dashboard.isLoaded());
    }
}
