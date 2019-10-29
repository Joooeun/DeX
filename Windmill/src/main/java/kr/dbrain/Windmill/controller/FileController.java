package kr.dbrain.Windmill.controller;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import kr.dbrain.Windmill.util.Common;

@Controller
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	Common com = new Common();

	@RequestMapping(path = "/FILE", method = RequestMethod.GET)
	public ModelAndView FILEf(HttpServletRequest request, ModelAndView mv, HttpSession session) {

		return mv;
	}

	@ResponseBody
	@RequestMapping(path = "/FILE/readfile")
	public Map<String, String> readfile(HttpServletRequest request, Model model, HttpSession session1) throws ClassNotFoundException, JSchException {

		Map<String, String> map = com.ConnectionConf(request.getParameter("Connection"));

		// 1. JSch 객체를 생성한다.
		JSch jsch = new JSch();
		// 2. 세션 객체를 생성한다(사용자 이름, 접속할 호스트, 포트를 인자로 전달한다.)
		Session session = jsch.getSession(map.get("USER"), map.get("IP"), Integer.parseInt(map.get("PORT")));
		// 4. 세션과 관련된 정보를 설정한다.
		session.setConfig("StrictHostKeyChecking", "no");
		// 4. 패스워드를 설정한다.
		session.setPassword(map.get("PW"));
		// 5. 접속한다.
		session.connect();

		// 6. sftp 채널을 연다.
		Channel channel = session.openChannel("sftp");
		// 7. 채널에 연결한다.
		channel.connect();
		// 8. 채널을 FTP용 채널 객체로 캐스팅한다.
		ChannelSftp sftpChannel = (ChannelSftp) channel;

		String fileName = request.getParameter("FilePath");
		BufferedReader br;
		String filestr = "";

		map.clear();
		try {
			// Change to output directory
			String cdDir = fileName.substring(0, fileName.lastIndexOf("/") + 1);
			sftpChannel.cd(cdDir);

			File file = new File(fileName);
			br = new BufferedReader(new InputStreamReader(sftpChannel.get(file.getName())));

			String line = "";
			while ((line = br.readLine()) != null) {
				filestr += line + "\r\n";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		map.put("result", filestr);

		if (session.isConnected()) {
			sftpChannel.disconnect();
			channel.disconnect();
			session.disconnect();
		}

		return map;

	}

}
