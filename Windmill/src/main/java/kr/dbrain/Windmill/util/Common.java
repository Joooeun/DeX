package kr.dbrain.Windmill.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Common {
	private static final Logger logger = LoggerFactory.getLogger(Common.class);

	public static String system_properties = "";
	public static String ConnectionPath = "";
	public static String srcPath = "";
	public static String tempPath = "";

	public Common() {
		system_properties = getClass().getResource("").getPath().replaceAll("(WEB-INF).*", "$1") + File.separator + "system.properties";
		Setproperties();
	}

	public static void Setproperties() {

		logger.debug("[DEBUG : system_properties]" + system_properties);

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
		logger.debug("[DEBUG : ConnectionPath]" + ConnectionPath);
		logger.debug("[DEBUG : srcPath]" + srcPath);

	}

	public Map<String, String> ConnectionConf(String ConnectionName) {
		Map<String, String> map = new HashMap<>();

		map.put("ConnectionName", ConnectionName);

		try {
			String propFile = ConnectionPath + ConnectionName;
			Properties props = new Properties();
			FileInputStream fis;

			fis = new FileInputStream(propFile + ".properties");

			props.load(new java.io.BufferedInputStream(fis));

			map.put("IP", props.getProperty("IP"));
			map.put("PORT", props.getProperty("PORT"));
			map.put("USER", props.getProperty("USER"));
			map.put("PW", props.getProperty("PW"));

			if (props.getProperty("TYPE").equals("DB")) {
				map.put("DB", props.getProperty("DB"));
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public List<String> ConnectionnList(String type) {

		logger.debug("[DEBUG]" + type);
		List<String> dblist = new ArrayList<>();

		try {
			// System.out.println("[debug]" + ConnectionPath);
			File dirFile = new File(ConnectionPath);
			File[] fileList = dirFile.listFiles();
			Arrays.sort(fileList);
			for (File tempFile : fileList) {
				if (tempFile.isFile() && tempFile.getName().substring(tempFile.getName().indexOf(".")).equals(".properties")) {
					FileInputStream fis;

					fis = new FileInputStream(tempFile);

					Properties props = new Properties();

					logger.debug("[DEBUG]" + tempFile.getPath());
					props.load(new java.io.BufferedInputStream(fis));

					if (props.getProperty("TYPE").equals(type)) {
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
