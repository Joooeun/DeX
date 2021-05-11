package kr.dbrain.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
	public String login(HttpServletRequest request, Model model) {

		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(180);
		
		//System.out.println(request.getParameter("id")+" / "+Common.Id+" / "+request.getParameter("pw")+" / "+Common.Pw);

		if (request.getParameter("id").equals(Common.Id) && request.getParameter("pw").equals(Common.Pw)) {
			session.setAttribute("memberId", "member");
			System.out.println("로그인 됨.");
		}
		
		System.out.println("session : " + session.getAttribute("memberId"));

		return "redirect:/index";
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
