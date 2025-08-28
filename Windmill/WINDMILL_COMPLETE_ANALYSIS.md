# **Windmill 프로젝트 완전 분석 문서**

## **📋 목차**
1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처 구조](#아키텍처-구조)
3. [패키지 및 클래스 분석](#패키지-및-클래스-분석)
4. [외부 라이브러리 및 프레임워크](#외부-라이브러리-및-프레임워크)
5. [설정 파일 분석](#설정-파일-분석)
6. [실행 방법 및 초기 설정](#실행-방법-및-초기-설정)
7. [기능 설명서](#기능-설명서)
8. [사용자 매뉴얼](#사용자-매뉴얼)

---

## **🏗️ 프로젝트 개요**

### **프로젝트 정보**
- **프로젝트명**: Windmill (DeX)
- **버전**: 0.0.1-SNAPSHOT
- **패키징**: WAR (Web Application Archive)
- **Java 버전**: 1.8
- **프레임워크**: Spring MVC 4.3.16.RELEASE
- **데이터베이스**: DB2, SQLite (DuckDB)
- **빌드 도구**: Maven

### **주요 기능**
- **SQL 실행 및 관리**: 파일 기반 SQL 템플릿 실행
- **데이터베이스 연결 관리**: 다중 DB 연결 지원
- **사용자 관리**: 로그인, 권한 관리
- **파일 관리**: SFTP 파일 업로드/다운로드
- **로그 관리**: 실행 로그 및 감사 로그
- **대시보드**: 시스템 상태 모니터링

---

## **🏛️ 아키텍처 구조**

### **전체 시스템 구조도**
```
┌─────────────────────────────────────────────────────────────┐
│                    Windmill Web Application                 │
├─────────────────────────────────────────────────────────────┤
│  Presentation Layer (JSP + JavaScript)                      │
│  ├── index.jsp (메인 대시보드)                              │
│  ├── SQL.jsp (SQL 실행 화면)                                │
│  ├── Connection.jsp (DB 연결 관리)                          │
│  ├── Login.jsp (로그인 화면)                                │
│  └── common/ (공통 컴포넌트)                                │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer (Spring MVC)                              │
│  ├── LoginController (로그인 처리)                          │
│  ├── SQLController (SQL 실행 처리)                          │
│  ├── ConnectionController (연결 관리)                       │
│  ├── FileController (파일 관리)                             │
│  └── UserController (사용자 관리)                           │
├─────────────────────────────────────────────────────────────┤
│  Service Layer (비즈니스 로직)                              │
│  ├── SQLExecuteService (SQL 실행 엔진)                      │
│  ├── ConnectionDTO (연결 정보 DTO)                          │
│  └── LogInfoDTO (로그 정보 DTO)                             │
├─────────────────────────────────────────────────────────────┤
│  Utility Layer (공통 유틸리티)                              │
│  ├── Common (공통 기능)                                     │
│  ├── Log (로깅 시스템)                                      │
│  └── Crypto (암호화)                                        │
├─────────────────────────────────────────────────────────────┤
│  Configuration Layer (설정)                                 │
│  ├── AppConfig (Spring 설정)                                │
│  ├── WebMvcConfig (MVC 설정)                                │
│  └── LoginInterceptor (인증 인터셉터)                      │
├─────────────────────────────────────────────────────────────┤
│  Data Access Layer                                          │
│  ├── JNDI DataSource (DB2 연결)                             │
│  ├── SQLite (DuckDB - 로컬 데이터)                          │
│  └── File System (SQL 템플릿, 설정 파일)                    │
└─────────────────────────────────────────────────────────────┘
```

### **패키지 구조**
```
kr.Windmill/
├── config/                    # Spring 설정 클래스들
│   ├── AppConfig.java         # 애플리케이션 설정
│   ├── WebMvcConfig.java      # MVC 설정
│   └── LoginInterceptor.java  # 로그인 인터셉터
├── controller/                # Spring MVC 컨트롤러
│   ├── LoginController.java   # 로그인 처리
│   ├── SQLController.java     # SQL 실행 처리
│   ├── ConnectionController.java # DB 연결 관리
│   ├── FileController.java    # 파일 관리
│   └── UserController.java    # 사용자 관리
├── service/                   # 비즈니스 로직 서비스
│   ├── SQLExecuteService.java # SQL 실행 엔진
│   ├── ConnectionDTO.java     # 연결 정보 DTO
│   ├── LogInfoDTO.java        # 로그 정보 DTO
│   ├── SampleService.java     # 샘플 서비스
│   └── SampleServiceImpl.java # 샘플 서비스 구현
├── util/                      # 유틸리티 클래스
│   ├── Common.java            # 공통 기능
│   ├── Log.java               # 로깅 시스템
│   └── Crypto.java            # 암호화 유틸리티
└── mapper/                    # MyBatis 매퍼 (미구현)
```

---

## **📦 패키지 및 클래스 분석**

### **1. config 패키지**

#### **AppConfig.java**
**역할**: Spring 애플리케이션의 핵심 설정 클래스
**주요 기능**:
- JNDI DataSource 설정 (DB2 연결)
- MyBatis SqlSessionFactory 설정
- 트랜잭션 매니저 설정
- 컴포넌트 스캔 설정

**핵심 메서드**:
```java
@Bean
public DataSource dataSource() {
    // JNDI를 통한 DB2 DataSource 조회
}

@Bean
public SqlSessionFactoryBean sqlSessionFactory() {
    // MyBatis 설정 (언더스코어 → 카멜케이스 자동 변환)
}
```

#### **WebMvcConfig.java**
**역할**: Spring MVC 웹 설정
**주요 기능**:
- 뷰 리졸버 설정 (JSP)
- 정적 리소스 핸들러 설정
- 인터셉터 등록 (로그인 체크)

**핵심 설정**:
```java
@Override
public void configureViewResolvers(ViewResolverRegistry registry) {
    registry.jsp().prefix("/WEB-INF/views/");
}

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginInterceptor)
           .addPathPatterns("/**")
           .excludePathPatterns("/Login", "/index/login", "/Setting");
}
```

#### **LoginInterceptor.java**
**역할**: 로그인 인증 인터셉터
**주요 기능**:
- 세션 기반 로그인 체크
- AJAX 요청 처리
- 로그인 실패 시 리다이렉트

**핵심 로직**:
```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    HttpSession session = request.getSession();
    String memberId = (String) session.getAttribute("memberId");
    
    if (memberId != null) {
        return true; // 로그인 성공
    } else {
        // 로그인 실패 처리
        if (isAjaxRequest(request)) {
            response.setHeader("SESSION_EXPIRED", "true");
        } else {
            // 로그인 페이지로 리다이렉트
        }
        return false;
    }
}
```

### **2. controller 패키지**

#### **LoginController.java**
**역할**: 사용자 로그인/로그아웃 처리
**주요 기능**:
- 사용자 인증 (파일 기반 사용자 정보)
- IP 제한 기능
- 임시 비밀번호 처리
- 세션 관리

**핵심 메서드**:
```java
@RequestMapping(path = "/index/login")
public String login(HttpServletRequest request, Model model, HttpServletResponse response) {
    // 1. 사용자 목록 조회
    List<Map<String, String>> userList = com.UserList();
    
    // 2. 사용자 ID 검증
    if (userIds.contains(request.getParameter("id"))) {
        Map<String, String> map = com.UserConf(request.getParameter("id"));
        
        // 3. IP 제한 검증
        if (!map.get("IP").equals("") && !map.get("IP").equals(com.getIp(request))) {
            // IP 불일치 처리
        }
        
        // 4. 비밀번호 검증
        if (map.get("PW").equals(request.getParameter("pw"))) {
            session.setAttribute("memberId", request.getParameter("id"));
            return "redirect:/index";
        }
    }
}
```

#### **SQLController.java**
**역할**: SQL 실행 및 관리
**주요 기능**:
- SQL 파일 실행
- 파라미터 처리
- 결과 데이터 반환
- 로깅 처리

**핵심 메서드**:
```java
@RequestMapping(path = "/SQL/execute", method = RequestMethod.POST)
@ResponseBody
public Map<String, Object> executeSQL(HttpServletRequest request, @ModelAttribute LogInfoDTO data) {
    // 1. SQL 파일 읽기
    String sql = com.FileRead(new File(data.getPath()));
    
    // 2. 파라미터 처리
    data.setParamList(com.getListFromString(data.getParams()));
    
    // 3. SQL 실행
    Map<String, List> result = sqlExecuteService.executeSQL(data);
    
    // 4. 결과 반환
    return result;
}
```

#### **ConnectionController.java**
**역할**: 데이터베이스 연결 관리
**주요 기능**:
- 연결 목록 조회
- 연결 테스트
- 연결 상태 모니터링

#### **FileController.java**
**역할**: 파일 업로드/다운로드 관리
**주요 기능**:
- SFTP 파일 업로드
- 파일 다운로드
- 파일 목록 조회

#### **UserController.java**
**역할**: 사용자 관리
**주요 기능**:
- 사용자 목록 조회
- 사용자 생성/수정/삭제
- 권한 관리

### **3. service 패키지**

#### **SQLExecuteService.java**
**역할**: SQL 실행 엔진 (핵심 서비스)
**주요 기능**:
- SQL 타입 감지 (SELECT, INSERT, UPDATE, DELETE, CALL)
- 파라미터 바인딩
- 결과 데이터 처리
- 로깅 및 감사

**핵심 메서드**:
```java
public Map<String, List> executeSQL(LogInfoDto data) throws Exception {
    // 1. 실행 시작 시간 설정
    data.setStart(Instant.now());
    
    // 2. SQL 타입 감지
    SqlType sqlType = detectSqlType(sql);
    
    // 3. SQL 타입별 실행
    switch (sqlType) {
        case CALL:
            result = callprocedure(sql, data, mapping);
            break;
        case EXECUTE:
            result = excutequery(sql, data, data.getLimit(), mapping);
            break;
        default:
            result = processUpdateSQL(data, mapping, sql);
    }
    
    // 4. 로깅 처리
    cLog.log_end(data, " sql 실행 종료 : 성공");
    cLog.log_DB(data);
}
```

**SQL 타입별 처리**:
- **CALL**: 프로시저 실행 (CallableStatement)
- **SELECT**: 쿼리 실행 (PreparedStatement)
- **UPDATE/INSERT/DELETE**: 업데이트 실행 (PreparedStatement)

#### **ConnectionDTO.java**
**역할**: 데이터베이스 연결 정보 DTO
**주요 필드**:
- connectionId: 연결 ID
- connectionName: 연결명
- url: 데이터베이스 URL
- username: 사용자명
- password: 비밀번호
- driverClassName: 드라이버 클래스명

#### **LogInfoDTO.java**
**역할**: SQL 실행 로그 정보 DTO
**주요 필드**:
- id: 사용자 ID
- ip: 클라이언트 IP
- connectionId: DB 연결 ID
- sql: 실행할 SQL
- params: 파라미터 정보
- start/end: 실행 시작/종료 시간
- audit: 감사 로그 여부

### **4. util 패키지**

#### **Common.java**
**역할**: 공통 유틸리티 클래스 (핵심 유틸리티)
**주요 기능**:
- 시스템 설정 관리
- 파일 읽기/쓰기
- 사용자 정보 관리
- IP 주소 조회
- JSON 처리

**핵심 메서드**:
```java
// 시스템 설정 로드
public static void Setproperties() {
    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(system_properties);
    props.load(fis);
    
    RootPath = props.getProperty("Root") + File.separator;
    ConnectionPath = props.getProperty("Root") + File.separator + "Connection" + File.separator;
    SrcPath = props.getProperty("Root") + File.separator + "src" + File.separator;
    Timeout = Integer.parseInt(props.getProperty("Timeout"));
}

// 사용자 목록 조회
public List<Map<String, String>> UserList() throws IOException {
    // 파일 기반 사용자 정보 읽기
}

// 파일 읽기
public String FileRead(File file) throws IOException {
    // 파일 내용 읽기
}
```

#### **Log.java**
**역할**: 로깅 시스템
**주요 기능**:
- 파일 로그 기록
- 데이터베이스 로그 저장
- 사용자 활동 로그
- 에러 로그

**핵심 메서드**:
```java
// SQL 실행 시작 로그
public void log_start(LogInfoDto data, String msg) {
    // 파일에 실행 시작 로그 기록
}

// SQL 실행 종료 로그
public void log_end(LogInfoDto data, String msg) {
    // 파일에 실행 종료 로그 기록
}

// 데이터베이스 로그 저장
public void log_DB(LogInfoDto data) {
    // DEXLOG 테이블에 로그 저장
}
```

#### **Crypto.java**
**역할**: 암호화 유틸리티
**주요 기능**:
- AES-256 암호화/복호화
- 비밀번호 암호화

---

## **📚 외부 라이브러리 및 프레임워크**

### **Spring Framework 4.3.16.RELEASE**
- **spring-webmvc**: MVC 프레임워크
- **spring-jdbc**: JDBC 지원
- **spring-context**: IoC 컨테이너

### **MyBatis 3.4.6**
- **mybatis**: SQL 매핑 프레임워크
- **mybatis-spring**: Spring 연동

### **로깅 프레임워크**
- **slf4j-api 1.7.25**: 로깅 API
- **logback-classic 1.2.3**: 로깅 구현체

### **JSON 처리**
- **jackson-databind 2.12.3**: JSON 직렬화/역직렬화
- **json-simple 1.1.1**: 간단한 JSON 처리

### **파일 처리**
- **commons-fileupload 1.2.1**: 파일 업로드
- **commons-io 1.4**: 파일 I/O 유틸리티

### **데이터베이스 드라이버**
- **com.ibm.db2:jcc 11.5.0.0**: DB2 JDBC 드라이버

### **기타 유틸리티**
- **commons-lang3 3.5**: Apache Commons Lang
- **lombok 1.18.4**: 코드 생성 도구
- **jsch 0.1.55**: SFTP 클라이언트

---

## **⚙️ 설정 파일 분석**

### **1. Maven 설정 (pom.xml)**
**주요 설정**:
- Java 1.8 사용
- WAR 패키징
- Spring Framework 4.3.16.RELEASE
- MyBatis 3.4.6
- DB2 JDBC 드라이버

### **2. Web 설정 (web.xml)**
**주요 설정**:
- Spring ContextLoaderListener
- DispatcherServlet 설정
- 문자 인코딩 필터 (UTF-8)
- JNDI DataSource 참조
- JSP 설정

### **3. 로깅 설정 (logback.xml)**
**주요 설정**:
- 콘솔 로그 출력
- 파일 로그 출력 (RollingFileAppender)
- 로그 레벨: INFO
- 로그 파일 크기: 10MB
- 로그 패턴 설정

### **4. 시스템 설정 (system.properties)**
**주요 설정**:
- Root: 애플리케이션 루트 경로
- Timeout: 세션 타임아웃 (분)
- LogDB: 로그 데이터베이스
- DownloadIP: 다운로드 허용 IP

---

## **🚀 실행 방법 및 초기 설정**

### **1. 개발 환경 요구사항**
- **Java**: JDK 1.8 이상
- **Maven**: 3.6.0 이상
- **서버**: Apache Tomcat 9.0 이상
- **데이터베이스**: DB2 (메인), SQLite/DuckDB (로컬)

### **2. 초기 설정**

#### **2.1. 데이터베이스 설정**
**DB2 설정 (context.xml)**:
```xml
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
```

#### **2.2. 시스템 설정 파일 생성**
**system.properties**:
```properties
Root=/path/to/windmill/root
Timeout=15
LogDB=your_log_database
DownloadIP=your_allowed_ip
```

#### **2.3. 디렉토리 구조 생성**
```
Windmill_Root/
├── Connection/          # DB 연결 설정 파일
├── src/                # SQL 템플릿 파일
├── user/               # 사용자 설정 파일
├── temp/               # 임시 파일
├── log/                # 로그 파일
└── jdbc/               # JDBC 드라이버
```

### **3. 빌드 및 배포**

#### **3.1. Maven 빌드**
```bash
# 프로젝트 디렉토리로 이동
cd Windmill

# Maven 빌드
mvn clean package

# WAR 파일 생성 확인
ls target/Windmill-0.0.1-SNAPSHOT.war
```

#### **3.2. Tomcat 배포**
```bash
# WAR 파일을 Tomcat webapps 디렉토리로 복사
cp target/Windmill-0.0.1-SNAPSHOT.war $TOMCAT_HOME/webapps/

# Tomcat 시작
$TOMCAT_HOME/bin/startup.sh

# 로그 확인
tail -f $TOMCAT_HOME/logs/catalina.out
```

### **4. 환경 변수 설정**
```bash
# Java 환경 변수
export JAVA_HOME=/path/to/jdk1.8
export PATH=$JAVA_HOME/bin:$PATH

# Maven 환경 변수
export MAVEN_HOME=/path/to/maven
export PATH=$MAVEN_HOME/bin:$PATH

# Tomcat 환경 변수
export CATALINA_HOME=/path/to/tomcat
export CATALINA_BASE=/path/to/tomcat
```

---

## **📖 기능 설명서**

### **1. SQL 실행 시스템**

#### **1.1. SQL 템플릿 관리**
- **파일 기반 SQL 템플릿**: `.sql` 파일로 SQL 저장
- **파라미터 지원**: `${parameter}` 형태의 파라미터 바인딩
- **Properties 설정**: `.properties` 파일로 파라미터 정의

**SQL 템플릿 예시**:
```sql
-- user_list.sql
SELECT * FROM USERS 
WHERE USER_ID = '${userId}' 
AND STATUS = '${status}'
```

**Properties 설정 예시**:
```properties
# user_list.properties
PARAM=userId&string&required
PARAM=status&string&required
```

#### **1.2. SQL 타입별 실행**
- **SELECT**: 데이터 조회 (ResultSet 처리)
- **INSERT/UPDATE/DELETE**: 데이터 변경 (executeUpdate)
- **CALL**: 프로시저 실행 (CallableStatement)

#### **1.3. 파라미터 처리**
- **문자열 타입**: 작은따옴표로 감싸기
- **숫자 타입**: 그대로 사용
- **필수 파라미터**: required 속성
- **기본값**: default 속성

### **2. 데이터베이스 연결 관리**

#### **2.1. 연결 설정**
- **JNDI DataSource**: DB2 연결
- **동적 연결**: 사용자별 연결 설정
- **연결 풀**: HikariCP 기반 연결 풀

#### **2.2. 연결 테스트**
- **연결 상태 확인**: ping 테스트
- **권한 확인**: 사용자 권한 검증
- **성능 모니터링**: 연결 시간 측정

### **3. 사용자 관리 시스템**

#### **3.1. 인증 시스템**
- **파일 기반 사용자**: 사용자 정보 파일 관리
- **IP 제한**: 허용된 IP에서만 접속
- **세션 관리**: 세션 타임아웃 설정

#### **3.2. 권한 관리**
- **사용자별 권한**: SQL 실행 권한
- **그룹 권한**: 사용자 그룹별 권한
- **감사 로그**: 사용자 활동 기록

### **4. 로깅 시스템**

#### **4.1. 실행 로그**
- **SQL 실행 로그**: 실행 시간, 결과, 에러
- **사용자 활동 로그**: 로그인, 로그아웃, 작업
- **시스템 로그**: 애플리케이션 상태

#### **4.2. 감사 로그**
- **DEXLOG 테이블**: 데이터베이스 로그 저장
- **파일 로그**: 텍스트 파일 로그
- **로그 레벨**: INFO, ERROR, DEBUG

### **5. 파일 관리 시스템**

#### **5.1. SFTP 파일 관리**
- **파일 업로드**: SFTP 서버로 파일 전송
- **파일 다운로드**: SFTP 서버에서 파일 다운로드
- **파일 목록**: 원격 디렉토리 파일 목록

#### **5.2. 로컬 파일 관리**
- **SQL 파일**: 템플릿 파일 관리
- **설정 파일**: 시스템 설정 파일
- **로그 파일**: 로그 파일 관리

---

## **📘 사용자 매뉴얼**

### **1. 로그인 및 인증**

#### **1.1. 로그인 방법**
1. 브라우저에서 `http://localhost:8080/Windmill` 접속
2. 사용자 ID와 비밀번호 입력
3. 로그인 버튼 클릭

#### **1.2. 로그아웃**
1. 우측 상단 사용자 메뉴 클릭
2. "로그아웃" 선택

#### **1.3. 비밀번호 변경**
1. 임시 비밀번호로 로그인 시 자동으로 비밀번호 변경 모달 표시
2. 새 비밀번호 입력
3. 비밀번호 변경 버튼 클릭

### **2. SQL 실행**

#### **2.1. SQL 파일 실행**
1. 좌측 메뉴에서 "SQL" 선택
2. SQL 파일 목록에서 실행할 파일 선택
3. 파라미터 입력 (필요한 경우)
4. "실행" 버튼 클릭

#### **2.2. 직접 SQL 실행**
1. "SQL" 메뉴에서 "새 쿼리" 선택
2. SQL 문장 입력
3. "실행" 버튼 클릭

#### **2.3. 파라미터 사용**
1. SQL에서 `${parameter}` 형태로 파라미터 정의
2. 실행 시 파라미터 값 입력
3. 자동으로 SQL에 바인딩

**파라미터 예시**:
```sql
SELECT * FROM USERS WHERE USER_ID = '${userId}'
```
실행 시 `userId` 파라미터에 값 입력

### **3. 데이터베이스 연결 관리**

#### **3.1. 연결 목록 조회**
1. "연결 관리" 메뉴 선택
2. 등록된 연결 목록 확인

#### **3.2. 새 연결 추가**
1. "연결 추가" 버튼 클릭
2. 연결 정보 입력:
   - 연결명
   - 데이터베이스 URL
   - 사용자명
   - 비밀번호
   - 드라이버 클래스명
3. "저장" 버튼 클릭

#### **3.3. 연결 테스트**
1. 연결 목록에서 테스트할 연결 선택
2. "테스트" 버튼 클릭
3. 연결 상태 확인

### **4. 파일 관리**

#### **4.1. SFTP 파일 업로드**
1. "파일 관리" 메뉴 선택
2. "파일 업로드" 탭 선택
3. SFTP 서버 정보 입력
4. 업로드할 파일 선택
5. "업로드" 버튼 클릭

#### **4.2. SFTP 파일 다운로드**
1. "파일 관리" 메뉴 선택
2. "파일 다운로드" 탭 선택
3. SFTP 서버 정보 입력
4. 다운로드할 파일 선택
5. "다운로드" 버튼 클릭

### **5. 사용자 관리**

#### **5.1. 사용자 목록 조회**
1. "사용자 관리" 메뉴 선택
2. 등록된 사용자 목록 확인

#### **5.2. 새 사용자 추가**
1. "사용자 추가" 버튼 클릭
2. 사용자 정보 입력:
   - 사용자 ID
   - 사용자명
   - 비밀번호
   - 허용 IP (선택사항)
3. "저장" 버튼 클릭

#### **5.3. 사용자 권한 설정**
1. 사용자 목록에서 권한 설정할 사용자 선택
2. "권한 설정" 버튼 클릭
3. 권한 항목 선택/해제
4. "저장" 버튼 클릭

### **6. 로그 조회**

#### **6.1. 실행 로그 조회**
1. "로그 관리" 메뉴 선택
2. "실행 로그" 탭 선택
3. 조회 조건 설정:
   - 날짜 범위
   - 사용자
   - SQL 타입
4. "조회" 버튼 클릭

#### **6.2. 사용자 활동 로그 조회**
1. "로그 관리" 메뉴 선택
2. "활동 로그" 탭 선택
3. 조회 조건 설정
4. "조회" 버튼 클릭

### **7. 대시보드**

#### **7.1. 시스템 상태 확인**
1. 메인 페이지에서 시스템 상태 카드 확인
2. 데이터베이스 연결 상태
3. 활성 사용자 수
4. 시스템 리소스 사용량

#### **7.2. 차트 및 그래프**
1. SQL 실행 통계 차트
2. 사용자 활동 그래프
3. 시스템 성능 모니터링

### **8. 설정 관리**

#### **8.1. 시스템 설정**
1. "설정" 메뉴 선택
2. 시스템 설정 항목 수정:
   - 루트 경로
   - 세션 타임아웃
   - 로그 설정
3. "저장" 버튼 클릭

#### **8.2. 개인 설정**
1. 우측 상단 사용자 메뉴 클릭
2. "개인 설정" 선택
3. 개인 정보 수정
4. "저장" 버튼 클릭

### **9. 문제 해결**

#### **9.1. 로그인 실패**
- 사용자 ID와 비밀번호 확인
- IP 제한 확인
- 계정 잠금 상태 확인

#### **9.2. SQL 실행 실패**
- SQL 문법 확인
- 파라미터 값 확인
- 데이터베이스 연결 상태 확인
- 권한 확인

#### **9.3. 파일 업로드/다운로드 실패**
- SFTP 서버 연결 확인
- 파일 권한 확인
- 네트워크 연결 확인

#### **9.4. 성능 문제**
- 데이터베이스 연결 풀 설정 확인
- SQL 최적화
- 시스템 리소스 확인

---

## **🔧 고급 사용법**

### **1. SQL 템플릿 고급 기능**

#### **1.1. 동적 SQL**
```sql
SELECT * FROM USERS 
WHERE 1=1
${userId != '' ? "AND USER_ID = '" + userId + "'" : ""}
${status != '' ? "AND STATUS = '" + status + "'" : ""}
```

#### **1.2. 프로시저 호출**
```sql
CALL GET_USER_INFO(?, ?, ?)
```

#### **1.3. 배치 처리**
```sql
INSERT INTO USERS (USER_ID, USER_NAME) VALUES 
('user1', 'User 1'),
('user2', 'User 2'),
('user3', 'User 3')
```

### **2. 고급 로깅 설정**

#### **2.1. 로그 레벨 설정**
```xml
<!-- logback.xml -->
<logger name="kr.Windmill" level="DEBUG" additivity="false">
    <appender-ref ref="console" />
</logger>
```

#### **2.2. 로그 파일 설정**
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/windmill.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/windmill.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

### **3. 성능 최적화**

#### **3.1. 데이터베이스 연결 풀 최적화**
```xml
<!-- context.xml -->
<Resource name="jdbc/appdb" 
          maxTotal="50"
          maxIdle="20"
          minIdle="5"
          maxWaitMillis="30000"
          testOnBorrow="true"
          validationQuery="SELECT 1"/>
```

#### **3.2. SQL 최적화**
- 인덱스 활용
- 불필요한 컬럼 제거
- LIMIT 절 사용
- 파라미터 바인딩 활용

---

## **📋 체크리스트**

### **설치 및 설정 체크리스트**
- [ ] Java 1.8 설치 확인
- [ ] Maven 3.6.0 이상 설치 확인
- [ ] Tomcat 9.0 이상 설치 확인
- [ ] DB2 연결 설정 확인
- [ ] system.properties 파일 생성
- [ ] 디렉토리 구조 생성
- [ ] WAR 파일 빌드 성공
- [ ] Tomcat 배포 성공
- [ ] 애플리케이션 시작 성공
- [ ] 로그인 테스트 성공

### **기능 테스트 체크리스트**
- [ ] SQL 실행 기능 테스트
- [ ] 파라미터 바인딩 테스트
- [ ] 데이터베이스 연결 테스트
- [ ] 파일 업로드/다운로드 테스트
- [ ] 사용자 관리 기능 테스트
- [ ] 로그 조회 기능 테스트
- [ ] 권한 관리 기능 테스트

### **성능 테스트 체크리스트**
- [ ] 동시 사용자 테스트
- [ ] 대용량 데이터 처리 테스트
- [ ] 메모리 사용량 모니터링
- [ ] 응답 시간 측정
- [ ] 데이터베이스 연결 풀 테스트

---

**문서 버전**: 1.0  
**작성일**: 2025년 8월 13일  
**작성자**: Windmill 개발팀  
**최종 업데이트**: 2025년 8월 13일
