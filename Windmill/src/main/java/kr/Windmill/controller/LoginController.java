package kr.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

@Controller
public class LoginController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
	Common com = new Common();
	Log cLog = new Log();

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
		System.out.println("Timeout : " + Common.Timeout + " min");
		session.setMaxInactiveInterval(Common.Timeout * 60);

		try {
			List<Map<String, String>> userList = com.UserList();

			List<String> userIds = userList.stream().map(user -> user.get("id")).collect(Collectors.toList());

			if (userIds.contains(request.getParameter("id"))) {

				System.out.println("id : " + request.getParameter("id"));

				Map<String, String> map = com.UserConf(request.getParameter("id"));

				if (!map.get("IP").equals("") && !map.get("IP").equals(com.getIp(request))) {

					logger.info(request.getParameter("id") + " 로그인 실패.. 접속 ip : " + com.getIp(request));
					model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
					return "/common/messageRedirect";
				}

				if (map.get("TEMPPW").equals("true") && map.get("PW").equals(request.getParameter("pw"))) {

					session.setAttribute("memberId", request.getParameter("id"));
					session.setAttribute("changePW", true);
					cLog.userLog(request.getParameter("id"), com.getIp(request), " 로그인 성공, 비밀번호 변경 필요");

					response.setHeader("Cache-Control", "no-cache, no-store");
					response.setHeader("Pragma", "no-cache");
					response.setDateHeader("Expires", 0);

					return "redirect:/index";

				} else if (map.get("PW").equals(request.getParameter("pw"))) {

					session.setAttribute("memberId", request.getParameter("id"));
					session.setAttribute("changePW", false);
					cLog.userLog(request.getParameter("id"), com.getIp(request), " 로그인 성공");

					response.setHeader("Cache-Control", "no-cache, no-store");
					response.setHeader("Pragma", "no-cache");
					response.setDateHeader("Expires", 0);

					return "redirect:/index";

				} else {
					cLog.userLog(request.getParameter("id"), com.getIp(request), " 로그인 실패 / 입력 : " + request.getParameter("pw"));
					model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
					return "/common/messageRedirect";
				}

			} else {

				cLog.userLog(request.getParameter("id"), com.getIp(request), " 로그인 실패..");
				model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
				return "/common/messageRedirect";
			}
		} catch (Exception e) {

			model.addAttribute("params", com.showMessageAndRedirect("계정정보를 불러오는데 실패했습니다.", "/", "GET"));
			return "/common/messageRedirect";
		}

//		AES256Cipher a256 = AES256Cipher.getInstance();
//
//		if (a256.AES_Encode(request.getParameter("id")).equals("r57xendzrXD8pJMPJx9DMg==")
//				&& a256.AES_Encode(request.getParameter("pw")).equals("hAZgHLTCL9SSLBqsCMMm/g==")) {
//			session.setAttribute("memberId", request.getParameter("id"));
//			logger.info(request.getParameter("id") + " login." + getIp(request));
//		} else {
//			logger.info("faild login.." + a256.AES_Encode(request.getParameter("id")) + " / "
//					+ a256.AES_Encode(request.getParameter("pw")));
//		}

//		System.out.println("session : " + session.getAttribute("memberId"));

	}

	@RequestMapping(path = "/index", method = RequestMethod.GET)
	public ModelAndView sample(HttpServletRequest request, ModelAndView mv) {

		if (!new File(Common.RootPath).exists()) {
			logger.info(Common.RootPath + " 경로가 존재하지 않습니다.");
			mv.setViewName("Setting");
			return mv;
		}

		String path = Common.ConnectionPath;
		File folder = new File(path);

		if (!folder.exists()) {
			try {
				logger.info("Connection 폴더생성여부 : " + folder.mkdirs());
			} catch (Exception e) {
				e.getStackTrace();
			}
		}

		path = Common.SrcPath;
		folder = new File(path);

		if (!folder.exists()) {
			try {
				logger.info("src 폴더생성여부 : " + folder.mkdirs());
			} catch (Exception e) {
				e.getStackTrace();
			}
		}

		path = Common.UserPath;
		folder = new File(path);

		if (!folder.exists()) {
			try {
				logger.info("user 폴더생성여부 : " + folder.mkdirs());
			} catch (Exception e) {
				e.getStackTrace();
			}
		}

		path = Common.UserPath;
		folder = new File(path);

		if (!folder.exists()) {
			try {
				logger.info("user 폴더생성여부 : " + folder.mkdirs());
			} catch (Exception e) {
				e.getStackTrace();
			}
		}

		mv.addObject("sqllist", com.getfiles(Common.SrcPath, 0));
		
		// 버전 정보 추가
		mv.addObject("appVersion", VersionUtil.getVersion());

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

	@RequestMapping(path = "/index3")
	public ModelAndView index3(HttpServletRequest request, ModelAndView mv) {

		return mv;
	}

	@RequestMapping(value = "/userRemove")
	public String userRemove(HttpServletRequest request) {

		System.out.println("logout");

		HttpSession session = request.getSession();
		session.invalidate();

		return "redirect:/index";
	}

}
