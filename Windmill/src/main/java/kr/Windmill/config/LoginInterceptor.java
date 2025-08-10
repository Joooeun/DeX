package kr.Windmill.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

	private static final String AJAX_HEADER_NAME = "X-Requested-With";
	private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";

	// 요청을 컨트롤러에 보내기 전 작업
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

		HttpSession session = request.getSession();

//		logger.info(new Date()+ " / " + session.getMaxInactiveInterval());

		String memberId = (String) session.getAttribute("memberId");

		List vowelsList = Arrays
				.asList("/,/index,/index2,/SQL/list,/Connection/list,/Connection/sessionCon".split(","));

		if (!vowelsList.contains(request.getRequestURI())) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate = simpleDateFormat.format(new Date());

			logger.info("{} {} {}{}", strNowDate, memberId, request.getRequestURI(),
					(request.getParameter("Path") != null ? " " + request.getParameter("Path") : ""));
		}

		if (memberId != null) {
			return true;
		} else {

			if (isAjaxRequest(request)) {
				response.setHeader("SESSION_EXPIRED", "true");
			} else {
				try {

					java.io.PrintWriter out = response.getWriter();
					out.println("<html>");
					out.println("<script>");
					out.println("window.parent.location.href = '/Login'");
					out.println("</script>");
					out.println("</html>");
					out.flush();
					out.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return false;

		}
	}

	// Ajax 요청인지 체크하는 메소드
	private boolean isAjaxRequest(HttpServletRequest request) {
		return AJAX_HEADER_VALUE.equals(request.getHeader(AJAX_HEADER_NAME));
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
		// 시스템 프로퍼티 설정 오류 처리
		if (ex != null) {
			if (ex instanceof FileNotFoundException) {
				FileNotFoundException fnfe = (FileNotFoundException) ex;
				if (fnfe.getMessage() != null && fnfe.getMessage().contains("${system.root.path}")) {
					logger.error("시스템 프로퍼티 설정 오류: " + fnfe.getMessage());
					
					// Setting 화면으로 리다이렉트
					if (!isAjaxRequest(request)) {
						try {
							response.sendRedirect("/Setting");
						} catch (IOException e) {
							logger.error("리다이렉트 실패", e);
						}
					}
					return;
				}
			}
			
			// 기타 예외는 로깅만
			logger.error("요청 처리 중 오류 발생: " + request.getRequestURI(), ex);
		}
	}

}
