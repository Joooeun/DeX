package kr.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.util.Common;
import kr.Windmill.util.Log;
import kr.Windmill.util.VersionUtil;
import kr.Windmill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class LoginController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
	private final Common com;
	private final Log cLog;
	private final UserService userService;
	
	@Autowired
	public LoginController(Common common, Log log, UserService userService) {
		this.com = common;
		this.cLog = log;
		this.userService = userService;
	}

	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String sample() {
		return "redirect:/index";
	}

	@RequestMapping(path = "/Login")
	public ModelAndView login(HttpServletRequest request, ModelAndView mv) {

		return mv;
	}

	@RequestMapping(path = "/index/login")
	public String login(HttpServletRequest request, Model model, HttpServletResponse response) {

		HttpSession session = request.getSession();
		logger.info("Timeout : {} min", Common.Timeout);
		session.setMaxInactiveInterval(Common.Timeout * 60);

		String userId = request.getParameter("id");
		String password = request.getParameter("pw");
		String ipAddress = com.getIp(request);
		String userAgent = request.getHeader("User-Agent");

		try {
			// 데이터베이스 기반 로그인 처리
			Map<String, Object> loginResult = userService.login(userId, password, ipAddress, userAgent);
			
			if ((Boolean) loginResult.get("success")) {
				// 로그인 성공
				session.setAttribute("memberId", userId);
				session.setAttribute("sessionId", loginResult.get("sessionId"));
				session.setAttribute("changePW", false); // 임시 비밀번호 여부는 DB에서 확인 필요
				
				cLog.userLog(userId, ipAddress, " 로그인 성공");

				response.setHeader("Cache-Control", "no-cache, no-store");
				response.setHeader("Pragma", "no-cache");
				response.setDateHeader("Expires", 0);

				return "redirect:/index";
			} else {
				// 로그인 실패
				cLog.userLog(userId, ipAddress, " 로그인 실패: " + loginResult.get("message"));
				model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
				return "/common/messageRedirect";
			}

		} catch (Exception e) {
			logger.error("로그인 처리 중 오류 발생", e);
			cLog.userLog(userId, ipAddress, " 로그인 처리 오류: " + e.getMessage());
			model.addAttribute("params", com.showMessageAndRedirect("로그인 처리 중 오류가 발생했습니다.", "/", "GET"));
			return "/common/messageRedirect";
		}
	}

	// 로그아웃 처리
	@RequestMapping(path = "/logout")
	public String logout(HttpServletRequest request, Model model) {
		HttpSession session = request.getSession();
		String userId = (String) session.getAttribute("memberId");
		String sessionId = (String) session.getAttribute("sessionId");
		
		if (userId != null && sessionId != null) {
			try {
				userService.logout(sessionId, userId);
				cLog.userLog(userId, com.getIp(request), " 로그아웃");
			} catch (Exception e) {
				logger.error("로그아웃 처리 중 오류 발생", e);
			}
		}
		
		session.invalidate();
		return "redirect:/Login";
	}

	@RequestMapping(path = "/index", method = RequestMethod.GET)
	public ModelAndView sample(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		// RootPath 유효성 확인
		if (!Common.isRootPathValid()) {
			logger.warn("RootPath가 유효하지 않아 설정 화면으로 이동합니다: {}", Common.getRootPath());
			mv.setViewName("Setting");
			return mv;
		}

		// 버전 정보 추가
		mv.addObject("appVersion", VersionUtil.getVersion());
		
		// admin 권한 확인을 위한 사용자 ID 추가
		String memberId = (String) session.getAttribute("memberId");
		mv.addObject("memberId", memberId);
		mv.addObject("isAdmin", "admin".equals(memberId));

		return mv;
	}

	@RequestMapping(path = "/index/setting")
	public String setting(HttpServletRequest request, Model model) {

		File propfile = new File(Common.system_properties);

		FileWriter fw;
		try {

			String propStr = com.FileRead(propfile);
			fw = new FileWriter(propfile);
			fw.write(propStr.replaceAll("Root.*", "Root=" + request.getParameter("path").replace("\\", "/")));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Common.Setproperties();

		return "redirect:/index";
	}

	@RequestMapping(path = "/index2")
	public ModelAndView index2(HttpServletRequest request, ModelAndView mv) {

		return mv;
	}



	@RequestMapping(path = "/Dashboard")
	public ModelAndView dashboard(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		String memberId = (String) session.getAttribute("memberId");
		if (memberId == null || !"admin".equals(memberId)) {
			mv.setViewName("redirect:/index");
			return mv;
		}
		mv.setViewName("Dashboard");
		return mv;
	}

	@RequestMapping(value = "/userRemove")
	public String userRemove(HttpServletRequest request) {

		HttpSession session = request.getSession();
		// System.out.println("logout");
		logger.info("User logout: {}", session.getAttribute("memberId"));
		session.invalidate();

		return "redirect:/index";
	}

}
