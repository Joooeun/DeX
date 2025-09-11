package kr.Windmill.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class LoginInterceptorTest {

    @Test
    public void testSessionTimeoutRedirect() throws Exception {
        LoginInterceptor interceptor = new LoginInterceptor();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());

        assertFalse(result);
        assertTrue(res.getContentAsString().contains("window.parent.location.href = '/Login'"));
    }

    @Test
    public void testSessionTimeoutAjaxHeader() throws Exception {
        LoginInterceptor interceptor = new LoginInterceptor();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(req, res, new Object());

        assertFalse(result);
        assertEquals("true", res.getHeader("SESSION_EXPIRED"));
    }
}
