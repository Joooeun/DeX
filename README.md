# DeX Project

- **Java**: JDK 1.8
- **Maven**: 3.6.0
- **Tomcat**: 9.0
- **데이터베이스**: DB2


#시스템 루트 디렉토리 설정
<profile>
    <id>dev</id>
    <properties>
        <system.root.path>/your/custom/path/Menu</system.root.path>
    </properties>
</profile>


# 데이터베이스 설정

DEX_SCHEMA.sql

# Tomcat context.xml 설정

<Resource name="jdbc/appdb"
          auth="Container"
          type="javax.sql.DataSource"
          username="your_username"
          password="your_password"
          driverClassName="com.ibm.db2.jcc.DB2Driver"
          url="jdbc:db2://your_host:50000/your_database"
          maxTotal="20"
          maxIdle="10"
          maxWaitMillis="-1"/>
