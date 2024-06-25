package kr.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import kr.Windmill.util.Common;

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
	public Map<String, String> detail(HttpServletRequest request, Model model, HttpSession session) throws IOException {

		Map<String, String> map = com.ConnectionConf(request.getParameter("DB"));

		return map;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/list")
	public List<String> Connection_list(HttpServletRequest request, Model model, HttpSession session) {

		List<String> dblist = com.ConnectionnList(request.getParameter("TYPE"));
		String id = (String) session.getAttribute("memberId");

		if (!id.equals("admin")) {
			Map<String, String> map = com.UserConf(id);
			List<String> strList = new ArrayList<>(Arrays.asList(map.get("CONNECTION").split(",")));

			
			return dblist.stream().filter(con ->
			strList.contains(con.split("\\.")[0])).collect(Collectors.toList());

		}

		return dblist;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/sessionCon")
	public void sessionCon(HttpServletRequest request, HttpSession session) {

		session.setAttribute("Connection", request.getParameter("Connection"));

		return;
	}

	@ResponseBody
	@RequestMapping(path = "/Connection/save")
	public void save(HttpServletRequest request, HttpSession session) {

		String propFile = com.ConnectionPath + request.getParameter("file");
		File file = new File(propFile + ".properties");

		try {
			String str = "#" + request.getParameter("TYPE") + "\n";
			FileWriter fw = new FileWriter(file);
			str += "TYPE=" + request.getParameter("TYPE") + "\n";
			str += "IP=" + request.getParameter("IP") + "\n";
			str += "PORT=" + request.getParameter("PORT") + "\n";
			if (request.getParameter("TYPE").equals("DB")) {
				str += "DB=" + request.getParameter("DB") + "\n";
			}
			str += "USER=" + request.getParameter("USER") + "\n";
			str += "PW=" + request.getParameter("PW") + "\n";
			str += "DBTYPE=" + request.getParameter("DBTYPE");

			fw.write(com.cryptStr(str));
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

}
