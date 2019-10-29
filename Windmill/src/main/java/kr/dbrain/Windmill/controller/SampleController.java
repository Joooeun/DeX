package kr.dbrain.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

	@RequestMapping(path = "/index", method = RequestMethod.GET)
	public String sample(HttpServletRequest request, Model model) {
		if (!new java.io.File(Common.ConnectionPath).exists()) {
			return "Setting";
		}

		return "index";
	}

	@RequestMapping(path = "/index/setting")
	public String setting(HttpServletRequest request, Model model) {

		File propfile = new File(Common.system_properties);

		FileWriter fw;
		try {
			fw = new FileWriter(propfile);

			fw.write("Root=" + request.getParameter("path"));
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Common.Setproperties();

		return "redirect:/index";
	}

}
