### context.xml 수정
`~/apache-tomcat-9.0.73/conf/context.xml` 파일에 다음 내용 추가:

```xml
<Context>
    <!-- 기존 내용... -->
    
    <!-- DB2 DataSource -->
    <Resource name="jdbc/appdb" 
              auth="Container" 
              type="javax.sql.DataSource"
              maxTotal="20" 
              maxIdle="10" 
              maxWaitMillis="-1"
              username="db2inst1" 
              password="password" 
              driverClassName="com.ibm.db2.jcc.DB2Driver"
              url="jdbc:db2://localhost:50000/SAMPLE"
              validationQuery="SELECT 1 FROM SYSIBM.SYSDUMMY1"
              testOnBorrow="true"
              testOnReturn="false"
              testWhileIdle="true"
              timeBetweenEvictionRunsMillis="30000"
              minEvictableIdleTimeMillis="60000"/>
</Context>

### 연결 풀 크기 변경
```xml
maxTotal="50"    <!-- 최대 연결 수 -->
maxIdle="20"     <!-- 유휴 연결 수 -->
```
