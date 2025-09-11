package kr.Windmill.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.PermissionService;
import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.UserService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

public class DashboardPermissionTest {

    private LoginController controller;
    private PermissionService permissionService;

    @Before
    public void setUp() {
        Common common = mock(Common.class);
        Log log = mock(Log.class);
        UserService userService = mock(UserService.class);
        permissionService = mock(PermissionService.class);
        SystemConfigService configService = mock(SystemConfigService.class);
        controller = new LoginController(common, log, userService, permissionService, configService);
    }

    @Test
    public void testPermissionDeniedRedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", "user");
        request.setSession(session);

        when(permissionService.isAdmin("user")).thenReturn(false);

        ModelAndView mv = new ModelAndView();
        ModelAndView result = controller.dashboard(request, mv, session);
        assertEquals("redirect:/index", result.getViewName());
    }
}
