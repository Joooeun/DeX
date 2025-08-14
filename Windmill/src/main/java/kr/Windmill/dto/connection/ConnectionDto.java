package kr.Windmill.dto.connection;

import java.util.Properties;

/**
 * 연결 설정 Dto 클래스
 */
public class ConnectionDto {

    private Properties prop;
    private String dbtype;
    private String driver;
    private String jdbc;
    private String dbName;
    private String jdbcDriverFile; // JDBC 드라이버 파일명

    // 생성자
    public ConnectionDto() {}

    public ConnectionDto(String dbtype, String driver, String jdbc) {
        this.dbtype = dbtype;
        this.driver = driver;
        this.jdbc = jdbc;
    }

    // Getter/Setter
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

    public String getJdbcDriverFile() {
        return jdbcDriverFile;
    }

    public void setJdbcDriverFile(String jdbcDriverFile) {
        this.jdbcDriverFile = jdbcDriverFile;
    }

    @Override
    public String toString() {
        return "ConnectionDto{" +
                "dbtype='" + dbtype + '\'' +
                ", driver='" + driver + '\'' +
                ", jdbc='" + jdbc + '\'' +
                ", dbName='" + dbName + '\'' +
                ", jdbcDriverFile='" + jdbcDriverFile + '\'' +
                '}';
    }
}
