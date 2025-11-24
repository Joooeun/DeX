package kr.Windmill.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ManualController {

	private static final Logger logger = LoggerFactory.getLogger(ManualController.class);

	@RequestMapping(path = "/Manual", method = RequestMethod.GET)
	public ModelAndView Manual(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		mv.setViewName("Manual");
		return mv;
	}
}
