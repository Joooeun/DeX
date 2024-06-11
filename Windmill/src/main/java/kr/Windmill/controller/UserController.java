package kr.Windmill.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

import kr.Windmill.util.Common;

@Controller
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	Common com = new Common();

	@RequestMapping(path = "/User", method = RequestMethod.GET)
	public ModelAndView User(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		String memberId = (String) session.getAttribute("memberId");

		if (!memberId.equals("admin")) {

			mv.addObject("params", com.showMessageAndRedirect("권한이 없습니다.", "index", "GET"));
			mv.setViewName("common/messageRedirect");
			return mv;
		}

		List<Map<String, ?>> list = getfiles(Common.srcPath, 0);
		mv.addObject("MENU", list);

		return mv;
	}

	@RequestMapping(path = "/common/messageRedirect")
	public ModelAndView MessageRedirect(HttpServletRequest request, ModelAndView mv, HttpSession session) {
//		mv.addObject("Path", request.getParameter("Path"));
		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/User/detail")
	public Map<String, String> detail(HttpServletRequest request, Model model, HttpSession session) {

		Map<String, String> map = com.UserConf(request.getParameter("ID"));

		return map;
	}

	@ResponseBody
	@RequestMapping(path = "/User/list")
	public List<String> User_list(HttpServletRequest request, Model model, HttpSession session) {

		List<String> userList = com.UserList();

		return userList;
	}

	@ResponseBody
	@RequestMapping("/User/sessionCon")
	public void sessionCon(HttpServletRequest request, HttpSession session) {

		session.setAttribute("User", request.getParameter("User"));

		return;
	}

	@ResponseBody
	@RequestMapping(path = "/User/save")
	public void save(HttpServletRequest request, HttpSession session) {

		String propFile = com.UserPath + request.getParameter("file");
		File file = new File(propFile);

		try {
			String str = "#" + request.getParameter("ID") + "\n";
			FileWriter fw = new FileWriter(file);
			str += "IP=" + request.getParameter("IP") + "\n";
			str += "PW=" + request.getParameter("PW") + "\n";
			str += "MENU=" + request.getParameter("MENU") + "\n";
			str += "CONNECTION=" + request.getParameter("CONNECTION") + "\n";

			fw.write(str);
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

	@ResponseBody
	@RequestMapping(path = "/User/changePW")
	public void changePW(HttpServletRequest request, HttpSession session) {

		Map<String, String> map = com.UserConf(session.getAttribute("memberId").toString());

		String propFile = com.UserPath + session.getAttribute("memberId");
		File file = new File(propFile);

		try {

			String str = "#" + session.getAttribute("memberId") + "\n";
			FileWriter fw = new FileWriter(file);
			str += "IP=" + map.get("IP") + "\n";
			str += "PW=" + request.getParameter("PW") + "\n";
			str += "MENU=" + map.get("MENU") + "\n";
			str += "CONNECTION=" + map.get("CONNECTION") + "\n";

			fw.write(str);
			fw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

	public static List<Map<String, ?>> getfiles(String root, int depth) {

		List<Map<String, ?>> list = new ArrayList<>();

		File dirFile = new File(root);
		File[] fileList = dirFile.listFiles();
		Arrays.sort(fileList);
		for (File tempFile : fileList) {
			if (tempFile.isFile()) {

				if (tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".sql")) {
					Map<String, Object> element = new HashMap<>();
					element.put("Name", tempFile.getName());
					element.put("Path", tempFile.getPath());

					list.add(element);
				}

			} else if (tempFile.isDirectory()) {
				Map<String, Object> element = new HashMap<>();

				element.put("Name", tempFile.getName());
				element.put("Path", "Path" + depth);
				element.put("list", getfiles(tempFile.getPath(), depth + 1));

				list.add(element);
			}
		}

		return list;

	}

}
