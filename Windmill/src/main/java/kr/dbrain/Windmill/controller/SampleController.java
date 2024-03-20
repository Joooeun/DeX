package kr.dbrain.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import kr.dbrain.Windmill.service.SampleService;
import kr.dbrain.Windmill.util.Common;

@Controller
public class SampleController {

	private static final Logger logger = LoggerFactory.getLogger(SampleController.class);
	Common com = new Common();

	@Autowired
	SampleService sampleService;

	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String sample() {
		return "redirect:/index";
	}

	@RequestMapping(path = "/Login")
	public ModelAndView login(HttpServletRequest request, ModelAndView mv) {

		return mv;
	}

	@RequestMapping(path = "/index/login")
	public String login(HttpServletRequest request, Model model)
			throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

		HttpSession session = request.getSession();
		System.out.println("Timeout : " + Common.Timeout + " min");
		session.setMaxInactiveInterval(Common.Timeout * 5);

		List<String> userList = com.UserList();

		if (userList.contains(request.getParameter("id"))) {

			Map<String, String> map = com.UserConf(request.getParameter("id"));

//			if (map.get("IP").equals(getIp(request))) {

			if (map.get("PW").equals(request.getParameter("pw"))) {

				session.setAttribute("memberId", request.getParameter("id"));
				logger.info(request.getParameter("id") + " 로그인 성공. 접속 ip : " + com.getIp(request));
				return "redirect:/index";

			} else {
				logger.info(request.getParameter("id") + " 로그인 실패. 접속 ip : " + com.getIp(request));
				model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
				return "/common/messageRedirect";
			}

//			} else {
//				logger.info(request.getParameter("id") + " 로그인 실패.. 접속 ip : " + getIp(request));
//				model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
//				return "/common/messageRedirect";
//			}

		} else {

			logger.info(request.getParameter("id") + " 로그인 실패... 접속 ip : " + com.getIp(request));
			model.addAttribute("params", com.showMessageAndRedirect("계정정보가 올바르지 않습니다.", "/", "GET"));
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
		if (!new File(Common.ConnectionPath).exists()) {
			mv.setViewName("Setting");
			return mv;
		}

		mv.addObject("sqllist", SQLController.getfiles(Common.srcPath, 0));

		return mv;
	}

	@RequestMapping(path = "/index/setting")
	public String setting(HttpServletRequest request, Model model) {

		System.out.println(Common.system_properties);
		File propfile = new File(Common.system_properties);

		FileWriter fw;
		try {
			fw = new FileWriter(propfile);

			fw.write("Root=" + request.getParameter("path").replace("\\", "/"));
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
