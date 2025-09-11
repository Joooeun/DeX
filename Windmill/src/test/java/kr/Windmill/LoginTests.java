package kr.Windmill;

import kr.Windmill.ui.DashboardPage;
import kr.Windmill.ui.LoginPage;
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

    @Test
    public void loginFailsWithInvalidCredentials() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(BASE_URL);
        loginPage.login("wrong", "wrong");
        Assert.assertTrue("Should remain on login page", loginPage.isAt());
        Assert.assertNotNull("Error message should be shown", loginPage.getErrorMessage());
    }
}
