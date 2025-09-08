# 🌐 브라우저 자동화 테스트 가이드

## 📋 개요

이 프로젝트에는 Selenium WebDriver를 사용한 브라우저 자동화 테스트가 포함되어 있습니다. 웹 UI의 주요 기능들을 자동으로 테스트하여 수동 테스트의 부담을 줄이고 일관된 테스트 결과를 제공합니다.

## 🚀 빠른 시작

### 1. 사전 요구사항

- **Java 8 이상**
- **Maven 3.6 이상**
- **Chrome 브라우저** (최신 버전)
- **ChromeDriver** (자동 설치됨)
- **톰캣 서버** (localhost:8080에서 실행 중)

### 2. 테스트 실행

```bash
# 전체 브라우저 자동화 테스트 실행
./run-browser-tests.sh

# 특정 테스트만 실행
mvn test -Dtest=BrowserAutomationTest#testLogin

# 모든 테스트 실행 (API + 브라우저)
./run-all-tests.sh
```

### 3. 테스트 결과 확인

```bash
# HTML 리포트 생성
./generate-test-report.sh

# 브라우저에서 리포트 열기
open test-reports/browser-test-report.html
```

## 🧪 테스트 항목

### 현재 구현된 테스트들

1. **로그인 테스트** (`testLogin`)
   - 관리자 계정으로 로그인 기능 테스트
   - 로그인 페이지 접근 및 인증 확인

2. **SQL 템플릿 관리 페이지 접근 테스트** (`testSqlTemplatePageAccess`)
   - SQL 템플릿 관리 페이지 로드 확인
   - 카테고리 및 템플릿 목록 표시 확인

3. **SQL 템플릿 상세 조회 테스트** (`testSqlTemplateDetail`)
   - 템플릿 선택 시 상세 정보 로드 확인
   - 폼 필드 데이터 바인딩 확인

4. **새 템플릿 생성 테스트** (`testCreateNewTemplate`)
   - 새 템플릿 생성 전체 플로우 테스트
   - 템플릿 저장 및 목록 반영 확인

5. **대시보드 페이지 접근 테스트** (`testDashboardAccess`)
   - 대시보드 페이지 로드 확인
   - 차트 컨테이너 표시 확인

6. **연결 관리 페이지 접근 테스트** (`testConnectionManagementAccess`)
   - 연결 관리 페이지 로드 확인
   - 연결 목록 테이블 표시 확인

## ⚙️ 설정 및 커스터마이징

### 테스트 설정 변경

`src/test/java/kr/Windmill/BrowserAutomationTest.java` 파일에서 다음 설정을 변경할 수 있습니다:

```java
private static final String BASE_URL = "http://localhost:8080";  // 테스트 대상 URL
private static final String ADMIN_ID = "admin";                  // 테스트 계정 ID
private static final String ADMIN_PASSWORD = "1234";             // 테스트 계정 비밀번호
```

### 브라우저 옵션 변경

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless");        // 헤드리스 모드 (브라우저 창 숨김)
options.addArguments("--no-sandbox");      // 샌드박스 비활성화
options.addArguments("--disable-gpu");     // GPU 비활성화
options.addArguments("--window-size=1920,1080");  // 창 크기 설정
```

### 새로운 테스트 추가

새로운 테스트 메서드를 추가하려면:

```java
@Test
public void testNewFeature() {
    System.out.println("=== 새 기능 테스트 시작 ===");
    
    try {
        // 1. 로그인
        performLogin();
        
        // 2. 테스트할 페이지로 이동
        driver.get(BASE_URL + "/NewPage");
        
        // 3. 테스트 로직 구현
        // ...
        
        System.out.println("=== 새 기능 테스트 완료 ===");
    } catch (Exception e) {
        System.err.println("새 기능 테스트 실패: " + e.getMessage());
        takeScreenshot("new_feature_failure");
        throw e;
    }
}
```

## 📊 테스트 결과 및 리포트

### 자동 생성되는 파일들

- `test-results/browser-test-results.txt` - 테스트 실행 로그
- `test-reports/browser-test-report.html` - HTML 형태의 상세 리포트
- `screenshots/` - 테스트 실패 시 촬영된 스크린샷

### 리포트 내용

- 테스트 통계 (총 테스트 수, 성공/실패 수, 성공률)
- 각 테스트별 상세 결과
- 테스트 환경 정보
- 실패한 테스트의 스크린샷

## 🔧 문제 해결

### 일반적인 문제들

1. **ChromeDriver 버전 불일치**
   ```bash
   # ChromeDriver 재설치
   brew uninstall chromedriver
   brew install chromedriver
   ```

2. **헤드리스 모드에서 테스트 실패**
   - `--headless` 옵션을 제거하여 브라우저 창을 표시
   - 테스트 실행 과정을 시각적으로 확인

3. **페이지 로드 타임아웃**
   ```java
   // 타임아웃 시간 증가
   driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
   ```

4. **요소를 찾을 수 없는 경우**
   ```java
   // 더 긴 대기 시간 설정
   WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
   ```

### 로그 확인

테스트 실행 중 상세한 로그를 확인하려면:

```bash
# Maven 디버그 모드로 실행
mvn test -Dtest=BrowserAutomationTest -X

# 특정 테스트의 상세 로그
mvn test -Dtest=BrowserAutomationTest#testLogin -Dmaven.surefire.debug
```

## 📈 성능 최적화

### 테스트 실행 속도 향상

1. **불필요한 대기 시간 제거**
   ```java
   // Thread.sleep() 대신 WebDriverWait 사용
   WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("elementId")));
   ```

2. **병렬 테스트 실행**
   ```bash
   # Maven Surefire 플러그인으로 병렬 실행
   mvn test -DforkCount=2 -DreuseForks=true
   ```

3. **브라우저 재사용**
   ```java
   // @BeforeClass와 @AfterClass 사용으로 브라우저 인스턴스 재사용
   ```

## 🔄 CI/CD 통합

### GitHub Actions 예시

```yaml
name: Browser Automation Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 8
      uses: actions/setup-java@v2
      with:
        java-version: '8'
        distribution: 'adopt'
    - name: Install Chrome
      run: |
        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
        sudo sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list'
        sudo apt-get update
        sudo apt-get install -y google-chrome-stable
    - name: Run browser tests
      run: ./run-browser-tests.sh
```

## 📚 추가 리소스

- [Selenium WebDriver 공식 문서](https://selenium-python.readthedocs.io/)
- [JUnit 4 사용법](https://junit.org/junit4/)
- [Maven Surefire 플러그인](https://maven.apache.org/surefire/maven-surefire-plugin/)

## 🤝 기여하기

새로운 테스트를 추가하거나 기존 테스트를 개선하려면:

1. 테스트 코드 작성
2. 로컬에서 테스트 실행 및 검증
3. 커밋 및 푸시
4. 풀 리퀘스트 생성

---

**💡 팁**: 테스트를 작성할 때는 실제 사용자 시나리오를 기반으로 하여 의미 있는 테스트를 만드는 것이 중요합니다.
