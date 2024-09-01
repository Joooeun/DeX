package kr.Windmill.service;

import java.util.Properties;

public class ConnectionDTO {

	private Properties prop;

	private String dbtype;
	private String driver;
	private String jdbc;
	private String dbName;
	private String jar;
	

	public Properties getProp() {
		return prop;
	}

	public void setProp(Properties prop) {
		this.prop = prop;
	}

	public String getDbtype() {
		return dbtype;
	}

	public void setDbtype(String dbtype) {
		this.dbtype = dbtype;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getJdbc() {
		return jdbc;
	}

	public void setJdbc(String jdbc) {
		this.jdbc = jdbc;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getJar() {
		return jar;
	}

	public void setJar(String jar) {
		this.jar = jar;
	}
	
	

}
