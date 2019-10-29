package kr.dbrain.Windmill.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.dbrain.Windmill.util.Common;

@Controller
public class ConnectionController {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

	Common com = new Common();

	@RequestMapping(path = "/Connection", method = RequestMethod.GET)
	public ModelAndView Connection(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/detail")
	public Map<String, String> detail(HttpServletRequest request, Model model, HttpSession session) {

		Map<String, String> map = com.ConnectionConf(request.getParameter("DB"));

		return map;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/list")
	public List<String> Connection_list(HttpServletRequest request, Model model, HttpSession session) {

		List<String> dblist = com.ConnectionnList(request.getParameter("TYPE"));

		return dblist;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/sessionCon")
	public void sessionCon(HttpServletRequest request, HttpSession session) {

		session.setAttribute("Connection", request.getParameter("Connection"));

		return;
	}

}
