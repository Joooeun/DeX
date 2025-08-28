# **Windmill 프로젝트 완전 테스트 시나리오**

## **📋 프로젝트 정보**
- **프로젝트명**: Windmill (DeX)
- **프레임워크**: Spring MVC 4.3.16.RELEASE
- **빌드 도구**: Maven
- **Java 버전**: 1.8
- **패키지 루트**: kr.Windmill
- **데이터베이스**: DB2 (JNDI), SQLite/DuckDB (로컬)
- **빌드 명령**: `./mvnw -q -DskipTests package`
- **로컬 실행**: `mvn spring-boot:run` 또는 Tomcat 배포
- **프로필**: dev, prod
- **환경 변수**: `JAVA_HOME`, `CATALINA_HOME`, `DB2_HOME`

---

## **(A) 기능 목록 요약**

| 기능ID | 모듈 | 기능명 | 주요 클래스/메서드 | 외부연동 | 비고 |
|--------|------|--------|-------------------|----------|------|
| F-0001 | 인증 | 로그인/로그아웃 | LoginController.login() | - | 세션 기반 인증 |
| F-0002 | 인증 | IP 제한 검증 | LoginController.login() | - | 허용 IP 체크 |
| F-0003 | 인증 | 세션 관리 | LoginInterceptor.preHandle() | - | 인터셉터 기반 |
| F-0004 | 인증 | 임시 비밀번호 처리 | LoginController.login() | - | 비밀번호 변경 강제 |
| F-0005 | SQL | SQL 파일 실행 | SQLController.executeSQL() | DB2 | 파일 기반 SQL |
| F-0006 | SQL | 파라미터 바인딩 | SQLExecuteService.executeSQL() | - | ${param} 형태 |
| F-0007 | SQL | SQL 타입 감지 | SQLExecuteService.detectSqlType() | - | SELECT/UPDATE/CALL |
| F-0008 | SQL | 프로시저 실행 | SQLExecuteService.callprocedure() | DB2 | CallableStatement |
| F-0009 | SQL | 쿼리 실행 | SQLExecuteService.excutequery() | DB2 | PreparedStatement |
| F-0010 | SQL | 업데이트 실행 | SQLExecuteService.processUpdateSQL() | DB2 | executeUpdate |
| F-0011 | SQL | Properties 파싱 | SQLController.executeSQL() | - | 파라미터 정의 |
| F-0012 | 연결 | DB 연결 관리 | ConnectionController.list() | DB2 | JNDI DataSource |
| F-0013 | 연결 | 연결 테스트 | ConnectionController.test() | DB2 | 연결 상태 확인 |
| F-0014 | 연결 | 권한별 연결 필터링 | ConnectionController.list() | - | 사용자별 권한 |
| F-0015 | 파일 | SFTP 파일 업로드 | FileController.upload() | SFTP | JSch 라이브러리 |
| F-0016 | 파일 | SFTP 파일 다운로드 | FileController.download() | SFTP | 원격 파일 접근 |
| F-0017 | 파일 | 파일 목록 조회 | FileController.list() | SFTP | 원격 디렉토리 |
| F-0018 | 파일 | 로컬 파일 읽기 | Common.FileRead() | - | 텍스트 파일 |
| F-0019 | 사용자 | 사용자 목록 조회 | UserController.list() | - | 파일 기반 |
| F-0020 | 사용자 | 사용자 정보 조회 | UserController.detail() | - | 개별 사용자 |
| F-0021 | 사용자 | 사용자 정보 저장 | UserController.save() | - | 파일 쓰기 |
| F-0022 | 사용자 | 권한 관리 | UserController.save() | - | 관리자 전용 |
| F-0023 | 로깅 | 실행 로그 기록 | Log.log_start() | - | 파일 로그 |
| F-0024 | 로깅 | 종료 로그 기록 | Log.log_end() | - | 실행 시간 측정 |
| F-0025 | 로깅 | DB 로그 저장 | Log.log_DB() | DB2 | DEXLOG 테이블 |
| F-0026 | 로깅 | 사용자 활동 로그 | Log.userLog() | - | 로그인/로그아웃 |
| F-0027 | 암호화 | AES-256 암호화 | Crypto.crypt() | - | 비밀번호 암호화 |
| F-0028 | 암호화 | AES-256 복호화 | Crypto.deCrypt() | - | 비밀번호 복호화 |
| F-0029 | 설정 | 시스템 설정 로드 | Common.Setproperties() | - | system.properties |
| F-0030 | 설정 | JNDI DataSource 설정 | AppConfig.dataSource() | DB2 | 컨테이너 관리 |
| F-0031 | 설정 | MyBatis 설정 | AppConfig.sqlSessionFactory() | - | SQL 매핑 |
| F-0032 | 설정 | MVC 설정 | WebMvcConfig | - | 뷰 리졸버, 인터셉터 |
| F-0033 | 설정 | 인터셉터 등록 | WebMvcConfig.addInterceptors() | - | 로그인 체크 |
| F-0034 | 공통 | IP 주소 조회 | Common.getIp() | - | 클라이언트 IP |
| F-0035 | 공통 | 파일 목록 조회 | Common.getfiles() | - | 디렉토리 스캔 |
| F-0036 | 공통 | JSON 처리 | Common.parseJson() | - | JSON 파싱 |
| F-0037 | 공통 | 메시지 리다이렉트 | Common.showMessageAndRedirect() | - | 에러 처리 |

---

## **(B) 기능-테스트 타입 매트릭스**

| 기능ID | Unit | Integration | API | DB | Security | Perf | Concurrency | i18n | 기타 |
|--------|------|-------------|-----|----|----------|------|-------------|------|------|
| F-0001 | ✓ | ✓ | ✓ | - | ✓ | - | ✓ | - | Session |
| F-0002 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | IP Filter |
| F-0003 | ✓ | ✓ | ✓ | - | ✓ | - | ✓ | - | Interceptor |
| F-0004 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | Password |
| F-0005 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | SQL Exec |
| F-0006 | ✓ | ✓ | ✓ | ✓ | ✓ | - | - | - | Parameter |
| F-0007 | ✓ | ✓ | - | - | - | - | - | - | SQL Parse |
| F-0008 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | Procedure |
| F-0009 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | Query |
| F-0010 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | Update |
| F-0011 | ✓ | ✓ | - | - | - | - | - | - | Properties |
| F-0012 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - | Connection |
| F-0013 | ✓ | ✓ | ✓ | ✓ | ✓ | - | - | - | Connection Test |
| F-0014 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | Authorization |
| F-0015 | ✓ | ✓ | ✓ | - | ✓ | ✓ | - | - | SFTP |
| F-0016 | ✓ | ✓ | ✓ | - | ✓ | ✓ | - | - | SFTP |
| F-0017 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | SFTP |
| F-0018 | ✓ | ✓ | - | - | ✓ | - | - | - | File I/O |
| F-0019 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | User Mgmt |
| F-0020 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | User Detail |
| F-0021 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | File Write |
| F-0022 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | Authorization |
| F-0023 | ✓ | ✓ | - | - | - | - | ✓ | - | Logging |
| F-0024 | ✓ | ✓ | - | - | - | - | ✓ | - | Logging |
| F-0025 | ✓ | ✓ | - | ✓ | - | - | ✓ | - | DB Log |
| F-0026 | ✓ | ✓ | - | - | - | - | ✓ | - | Activity Log |
| F-0027 | ✓ | ✓ | - | - | ✓ | - | - | - | Encryption |
| F-0028 | ✓ | ✓ | - | - | ✓ | - | - | - | Decryption |
| F-0029 | ✓ | ✓ | - | - | - | - | - | - | Configuration |
| F-0030 | ✓ | ✓ | - | ✓ | - | - | - | - | JNDI |
| F-0031 | ✓ | ✓ | - | ✓ | - | - | - | - | MyBatis |
| F-0032 | ✓ | ✓ | - | - | - | - | - | - | MVC Config |
| F-0033 | ✓ | ✓ | ✓ | - | ✓ | - | - | - | Interceptor |
| F-0034 | ✓ | ✓ | - | - | - | - | - | - | IP Utils |
| F-0035 | ✓ | ✓ | - | - | - | - | - | - | File Utils |
| F-0036 | ✓ | ✓ | - | - | - | - | - | - | JSON Utils |
| F-0037 | ✓ | ✓ | - | - | - | - | - | - | Error Handling |

---

## **(C) 상세 시나리오: 인증 모듈**

### **로그인/로그아웃 (F-0001)**

| 필드 | 내용 |
|------|------|
| TC ID | TC-F-0001-01 |
| 연관 | LoginController.login(), LoginController.userRemove() |
| 전제 | 1) 사용자 정보 파일 존재 2) 세션 초기화 3) DB 연결 정상 |
| 입력 | 정상: id="admin", pw="password" / 경계: id="", pw="" / 에러: id="invalid", pw="wrong" |
| 절차 | 1) POST /index/login 요청 2) 사용자 ID 검증 3) 비밀번호 검증 4) 세션 설정 5) 리다이렉트 |
| 예상 결과 | 정상: 302 리다이렉트 / 에러: 에러 메시지 표시 |
| 검증 포인트 | 1) 세션에 memberId 설정 2) 로그 파일 기록 3) 응답 상태 코드 4) 리다이렉트 URL |
| 타입/우선순위 | Integration, P0 |
| 테스트 데이터 | Testcontainers: 사용자 정보 파일, WireMock: 세션 모킹 |
| 커버리지 태그 | 정상경로/예외경로/경계값/세션관리 |

### **IP 제한 검증 (F-0002)**

| 필드 | 내용 |
|------|------|
| TC ID | TC-F-0002-01 |
| 연관 | LoginController.login() |
| 전제 | 1) 사용자 정보에 IP 제한 설정 2) 허용 IP: 192.168.1.100 |
| 입력 | 정상: IP="192.168.1.100" / 에러: IP="192.168.1.200" |
| 절차 | 1) POST /index/login 요청 2) 사용자 정보 조회 3) IP 제한 확인 4) IP 검증 |
| 예상 결과 | 정상: 로그인 성공 / 에러: "계정정보가 올바르지 않습니다" |
| 검증 포인트 | 1) IP 제한 로직 실행 2) 에러 메시지 3) 로그 기록 4) 세션 미설정 |
| 타입/우선순위 | Unit, P0 |
| 테스트 데이터 | MockHttpServletRequest, 사용자 정보 파일 |
| 커버리지 태그 | IP검증/권한제한/에러처리/보안 |

---

## **권장 구현 스택**

### **Unit Test**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SQL 실행 서비스 테스트")
class SQLExecuteServiceTest {
    
    @Mock
    private Common common;
    
    @Mock
    private Log log;
    
    @InjectMocks
    private SQLExecuteService sqlExecuteService;
    
    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM users", "UPDATE users SET name='test'", "CALL procedure()"})
    @DisplayName("SQL 타입 감지 테스트")
    void testDetectSqlType(String sql) {
        // 테스트 구현
    }
}
```

### **Integration Test**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("SQL 실행 통합 테스트")
class SQLExecuteIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("SQL 실행 통합 테스트")
    void testExecuteSQL() {
        // 테스트 구현
    }
}
```

---

## **품질 게이트**

### **커버리지 목표**
- **라인 커버리지**: ≥ 70%
- **분기 커버리지**: ≥ 60%
- **메서드 커버리지**: ≥ 80%

### **고위험 기능 (P0) 커버리지**
- **정상 경로**: 100% 커버
- **에러 경로**: 100% 커버
- **경계값**: 100% 커버
- **동시성**: 100% 커버

### **테스트 실행 명령**
```bash
# 전체 테스트 실행
mvn test

# 커버리지 포함 테스트
mvn test jacoco:report

# 특정 모듈 테스트
mvn test -Dtest=SQLExecuteServiceTest

# 통합 테스트만 실행
mvn test -Dtest=*IntegrationTest
```

---

**문서 버전**: 1.0  
**작성일**: 2025년 8월 13일  
**작성자**: Windmill 개발팀  
**최종 업데이트**: 2025년 8월 13일
