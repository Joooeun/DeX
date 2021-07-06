package kr.dbrain.Windmill.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import crypt.AES256Cipher;

public class Common {
	private static final Logger logger = LoggerFactory.getLogger(Common.class);

	public static String system_properties = "";
	public static String ConnectionPath = "";
	public static String srcPath = "";
	public static String tempPath = "";
	public static int Timeout = 180;

	public Common() {
		system_properties = getClass().getResource("").getPath().replaceAll("(WEB-INF).*", "$1") + File.separator + "system.properties";
		Setproperties();
	}

	public static void Setproperties() {

//		logger.debug("[DEBUG : system_properties]" + system_properties);

		Properties props = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(system_properties);
			props.load(new java.io.BufferedInputStream(fis));
		} catch (IOException e) {
			logger.error("[ERROR : system_properties]" + system_properties);
		}

		ConnectionPath = props.getProperty("Root") + File.separator + "Connection" + File.separator;
		srcPath = props.getProperty("Root") + File.separator + "src" + File.separator;
		tempPath = props.getProperty("Root") + File.separator + "temp" + File.separator;
		Timeout = Integer.parseInt(props.getProperty("Timeout") == null ? "60" : props.getProperty("Timeout"));
		logger.debug("[DEBUG : Timeout]" + props.getProperty("Timeout"));
//		logger.debug("[DEBUG : srcPath]" + srcPath);

	}

	public Map<String, String> ConnectionConf(String ConnectionName) {
		Map<String, String> map = new HashMap<>();

		map.put("ConnectionName", ConnectionName);

		try {
			String propFile = ConnectionPath + ConnectionName;
			Properties props = new Properties();

			String propStr = FileRead(new File(propFile + ".properties"));

			props.load(new ByteArrayInputStream(propStr.getBytes()));

			map.put("TYPE", props.getProperty("TYPE"));
			map.put("IP", props.getProperty("IP"));
			map.put("PORT", props.getProperty("PORT"));
			map.put("USER", props.getProperty("USER"));
			map.put("PW", props.getProperty("PW"));
			map.put("DB", props.getProperty("DB"));
			map.put("DBTYPE", props.getProperty("DBTYPE"));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public List<String> ConnectionnList(String type) {

//		logger.debug("[DEBUG]" + type);
		List<String> dblist = new ArrayList<>();

		try {
			// System.out.println("[debug]" + ConnectionPath);
			File dirFile = new File(ConnectionPath);
			File[] fileList = dirFile.listFiles();
			Arrays.sort(fileList);
			for (File tempFile : fileList) {
				if (tempFile.isFile() && tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".properties")) {

					String propStr = FileRead(tempFile);

					Properties props = new Properties();

//					logger.debug("[DEBUG]" + tempFile.getPath());
					props.load(new ByteArrayInputStream(propStr.getBytes()));

					if (props.getProperty("TYPE").equals(type) || type.equals("")) {
						String tempFileName = tempFile.getName();
						dblist.add(tempFileName);
					}

				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dblist;
	}

	public String FileRead(File file) throws IOException {
		String str = "";

		BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		String line = "";

		while ((line = bufReader.readLine()) != null) {
			str += line + "\r\n";
		}
		bufReader.close();

		AES256Cipher a256 = AES256Cipher.getInstance();

		try {
			str = a256.AES_Decode(str);
		} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
				| BadPaddingException e) {
			// TODO Auto-generated catch block
			logger.warn("임호화 에러 ");
		}
		return str;
	}

	public String getSystem_properties() {
		return system_properties;
	}

	public void setSystem_properties(String system_properties) {
		this.system_properties = system_properties;
	}

	public String getConnectionPath() {
		return ConnectionPath;
	}

	public void setConnectionPath(String connectionPath) {
		ConnectionPath = connectionPath;
	}

	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

}
