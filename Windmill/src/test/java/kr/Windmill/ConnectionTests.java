package kr.Windmill;

import kr.Windmill.ui.ConnectionPage;
import kr.Windmill.util.*;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionTests extends BaseUITest {
    @Test
    public void connectionPageLoads() {
        LoginHelper.login(driver, BASE_URL, ADMIN_ID, ADMIN_PASSWORD);
        NavigationHelper.navigateTo(driver, BASE_URL, "/Connection");
        ConnectionPage page = new ConnectionPage(driver);
        Assert.assertTrue(page.isLoaded());
    }
}
