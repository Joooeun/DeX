package kr.Windmill.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.PasswordPolicyService;

@Component
public class LoginInterceptor implements HandlerInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

	private static final String AJAX_HEADER_NAME = "X-Requested-With";
	private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";

	private static final List<String> PASSWORD_CHANGE_ALLOWED_PREFIXES = Arrays.asList(
		"/index",
		"/index2",
		"/logout",
		"/User/changePW",
		"/resources/"
	);
	
	// 로그에서 제외할 URL 패턴들
	private static final List<Pattern> EXCLUDED_PATTERNS = Arrays.asList(
		Pattern.compile("^/$"),
		Pattern.compile("^/index$"),
		Pattern.compile("^/index2$"),
		Pattern.compile("^/SQL/list$"),
		Pattern.compile("^/Connection/.*"),
		Pattern.compile("^/Dashboard.*"),
		Pattern.compile("^/DexStatus.*"),
		Pattern.compile("^/SQLTemplate.*"),
		Pattern.compile("^/SystemConfig.*")
	);

	@Autowired
	private PasswordPolicyService passwordPolicyService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

		HttpSession session = request.getSession();
		String memberId = (String) session.getAttribute("memberId");
		String requestURI = request.getRequestURI();

		boolean isExcluded = EXCLUDED_PATTERNS.stream()
			.anyMatch(pattern -> pattern.matcher(requestURI).matches());
		
		if (!isExcluded) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String strNowDate = simpleDateFormat.format(new Date());

			logger.info("{} {} {}{}", strNowDate, memberId, requestURI,
					(request.getParameter("Path") != null ? " " + request.getParameter("Path") : ""));
		}

		if (memberId == null) {
			if (isAjaxRequest(request)) {
				response.setHeader("SESSION_EXPIRED", "true");
			} else {
				try {
					PrintWriter out = response.getWriter();
					out.println("<html>");
					out.println("<script>");
					out.println("window.parent.location.href = '/Login'");
					out.println("</script>");
					out.println("</html>");
					out.flush();
					out.close();
				} catch (IOException e) {
					logger.error("응답 출력 중 오류 발생", e);
				}
			}
			return false;
		}

		boolean mustChange = passwordPolicyService.mustChangePassword(memberId);
		session.setAttribute("changePW", mustChange);
		if (mustChange) {
			session.setAttribute("passwordChangeReason", passwordPolicyService.resolveChangeReason(memberId));
			if (!isPasswordChangeAllowed(requestURI)) {
				if (isAjaxRequest(request)) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setHeader("PASSWORD_CHANGE_REQUIRED", "true");
					response.setContentType("application/json;charset=UTF-8");
					try {
						PrintWriter out = response.getWriter();
						out.print("{\"success\":false,\"message\":\"비밀번호 변경이 필요합니다.\",\"passwordChangeRequired\":true}");
						out.flush();
					} catch (IOException e) {
						logger.error("비밀번호 변경 필요 응답 출력 중 오류", e);
					}
				} else {
					try {
						response.sendRedirect("/index");
					} catch (IOException e) {
						logger.error("비밀번호 변경 필요 리다이렉트 실패", e);
					}
				}
				return false;
			}
		}

		return true;
	}

	private boolean isPasswordChangeAllowed(String requestURI) {
		for (String prefix : PASSWORD_CHANGE_ALLOWED_PREFIXES) {
			if (requestURI.equals(prefix) || requestURI.startsWith(prefix + "/") || requestURI.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAjaxRequest(HttpServletRequest request) {
		return AJAX_HEADER_VALUE.equals(request.getHeader(AJAX_HEADER_NAME));
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
		if (ex != null) {
			if (ex instanceof FileNotFoundException) {
				FileNotFoundException fnfe = (FileNotFoundException) ex;
				if (fnfe.getMessage() != null && fnfe.getMessage().contains("${system.root.path}")) {
					logger.error("시스템 프로퍼티 설정 오류: " + fnfe.getMessage());
					
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
			
			logger.error("요청 처리 중 오류 발생: " + request.getRequestURI(), ex);
		}
	}

}
