package kr.dbrain.Windmill.config;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class LoginInterceptor implements HandlerInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

	// 요청을 컨트롤러에 보내기 전 작업
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

		HttpSession session = request.getSession();

		System.out.println(new Date().toLocaleString()+" / "+new Date(session.getLastAccessedTime()).toLocaleString() + " / " + session.getMaxInactiveInterval());

		String memberId = (String) session.getAttribute("memberId");

		if (memberId != null && memberId.equals("radius")) {
			return true;
		} else {
			try {
				java.io.PrintWriter out = response.getWriter();
				out.println("<html>");
				out.println("<script>");
				out.println("window.parent.location.href = '/Login'");
				out.println("</script>");
				out.println("</html>");
				out.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;

		}
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		// TODO Auto-generated method stub

	}

}
