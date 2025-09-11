package kr.Windmill.controller;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;

import kr.Windmill.service.PermissionService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.UserService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@RunWith(Parameterized.class)
public class LoginControllerTest {

    private final String userId;
    private final String password;
    private LoginController controller;

    public LoginControllerTest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String longStr = new String(new char[256]).replace('\0', 'a');
        return Arrays.asList(new Object[][] {
            {"", ""},
            {"user", ""},
            {"", "pass"},
            {longStr, "pw"},
            {"user", longStr}
        });
    }

    @Before
    public void setUp() {
        UserService userService = mock(UserService.class);
        PermissionService permService = mock(PermissionService.class);
        SystemConfigService configService = mock(SystemConfigService.class);
        Common common = mock(Common.class);
        Log log = mock(Log.class);

        Map<String, Object> failResult = new HashMap<>();
        failResult.put("success", false);
        failResult.put("message", "계정정보가 올바르지 않습니다.");
        when(userService.login(anyString(), anyString(), anyString(), anyString())).thenReturn(failResult);
        when(configService.getIntConfigValue(anyString(), anyInt())).thenReturn(1);
        when(common.getIp(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(common.showMessageAndRedirect(anyString(), anyString(), anyString())).thenReturn("errorRedirect");

        controller = new LoginController(common, log, userService, permService, configService);
    }

    @Test
    public void testInvalidCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", userId);
        request.setParameter("pw", password);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ConcurrentModel model = new ConcurrentModel();

        String viewName = controller.login(request, model, response);

        assertEquals("/common/messageRedirect", viewName);
        assertEquals("errorRedirect", model.getAttribute("params"));
    }
}
