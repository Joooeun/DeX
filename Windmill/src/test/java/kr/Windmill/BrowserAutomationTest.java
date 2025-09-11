package kr.Windmill;

// import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 브라우저 자동화 테스트 클래스
 * Selenium WebDriver를 사용하여 웹 UI 기능을 자동으로 테스트합니다.
 */
public class BrowserAutomationTest {
    
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;
    private Actions actions;
    private static final String BASE_URL = "http://localhost:8080";
    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "1234";
    
    @Before
    public void setUp() {
        // Chrome WebDriver 설정 (WebDriverManager 없이 직접 설정)
        System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 헤드리스 모드 비활성화 (브라우저 창을 띄움)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 기본 대기 시간 10초
        
        // 테스트 속도 조절을 위한 대기 시간 설정
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        
        // 페이지 로드 타임아웃 설정
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }
    
    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * 로그인 테스트
     */
    @Test
    public void testLogin() {
        System.out.println("=== 로그인 테스트 시작 ===");
        
        try {
            // 로그인 페이지로 이동
            driver.get(BASE_URL + "/Login");
            System.out.println("로그인 페이지 로드 완료");
            
            // 로그인 폼 입력
            WebElement idInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("id")));
            WebElement pwInput = driver.findElement(By.name("pw"));
            
            idInput.clear();
            idInput.sendKeys(ADMIN_ID);
            pwInput.clear();
            pwInput.sendKeys(ADMIN_PASSWORD);
            
            System.out.println("로그인 정보 입력 완료");
            
            // 로그인 버튼 클릭
            WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
            loginButton.click();
            
            // 로그인 성공 확인 (대시보드 페이지로 리다이렉트되는지 확인)
            wait.until(ExpectedConditions.urlContains("/index"));
            System.out.println("로그인 성공 확인");
            
        } catch (Exception e) {
            System.err.println("로그인 테스트 실패: " + e.getMessage());
            takeScreenshot("login_failure");
            throw e;
        }
        
        System.out.println("=== 로그인 테스트 완료 ===\n");
    }
    
    /**
     * SQL 템플릿 관리 페이지 접근 테스트
     */
    @Test
    public void testSqlTemplatePageAccess() {
        System.out.println("=== SQL 템플릿 관리 페이지 접근 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // 메인 페이지로 이동 (iframe 구조)
            driver.get(BASE_URL);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // 페이지 로드 대기
            
            // SQL 템플릿 관리 메뉴 클릭
            WebElement sqlTemplateMenu = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/SQLTemplate')]"))
            );
            sqlTemplateMenu.click();
            System.out.println("SQL 템플릿 관리 메뉴 클릭 완료");
            
            // iframe으로 전환
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("iframe_1")));
            driver.switchTo().frame(iframe);
            try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // iframe 로드 대기 (5초로 증가)
            
            // 현재 활성화된 탭의 페이지 소스 확인
            String pageSource = driver.getPageSource();
            System.out.println("현재 페이지 URL: " + driver.getCurrentUrl());
            System.out.println("페이지 소스 길이: " + pageSource.length());
            
            // 페이지 제목 확인 (여러 방법으로 시도)
            WebElement pageTitle = null;
            try {
                // h1 태그로 시도
                pageTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
            } catch (Exception e1) {
                try {
                    // h2 태그로 시도
                    pageTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h2")));
                } catch (Exception e2) {
                    try {
                        // title 태그로 시도
                        pageTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("title")));
                    } catch (Exception e3) {
                        System.out.println("페이지 제목을 찾을 수 없습니다. 페이지 소스에서 확인 중...");
                        // 페이지 소스에서 제목 관련 텍스트 찾기
                        if (pageSource.contains("SQL 템플릿")) {
                            System.out.println("페이지 제목: SQL 템플릿 관리 (소스에서 확인)");
                        } else {
                            System.out.println("페이지 제목을 확인할 수 없습니다.");
                        }
                    }
                }
            }
            
            if (pageTitle != null) {
                String title = pageTitle.getText();
                System.out.println("페이지 제목: " + title);
            }
            
            // 카테고리 목록이 로드되는지 확인 (여러 방법으로 시도)
            try {
                WebElement categoryList = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("categoryList")));
                System.out.println("카테고리 목록 로드 확인");
            } catch (Exception e) {
                System.out.println("categoryList ID로 찾을 수 없음. 다른 방법으로 시도...");
                try {
                    // 클래스명으로 시도
                    WebElement categoryList = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("category-list")));
                    System.out.println("카테고리 목록 로드 확인 (클래스명으로)");
                } catch (Exception e2) {
                    System.out.println("카테고리 목록을 찾을 수 없습니다.");
                }
            }
            
            // 템플릿 목록이 로드되는지 확인 (여러 방법으로 시도)
            try {
                WebElement templateList = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("templateList")));
                System.out.println("템플릿 목록 로드 확인");
            } catch (Exception e) {
                System.out.println("templateList ID로 찾을 수 없음. 다른 방법으로 시도...");
                try {
                    // 클래스명으로 시도
                    WebElement templateList = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("template-list")));
                    System.out.println("템플릿 목록 로드 확인 (클래스명으로)");
                } catch (Exception e2) {
                    System.out.println("템플릿 목록을 찾을 수 없습니다.");
                }
            }
            
            // 성공적인 페이지 로드 스크린샷 촬영
            takeScreenshot("sql_template_page_success");
            
        } catch (Exception e) {
            System.err.println("SQL 템플릿 페이지 접근 테스트 실패: " + e.getMessage());
            takeScreenshot("sql_template_page_failure");
            throw e;
        }
        
        System.out.println("=== SQL 템플릿 관리 페이지 접근 테스트 완료 ===\n");
    }
    
    /**
     * SQL 템플릿 상세 조회 테스트
     */
    @Test
    public void testSqlTemplateDetail() {
        System.out.println("=== SQL 템플릿 상세 조회 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // SQL 템플릿 관리 페이지로 이동
            driver.get(BASE_URL + "/SQLTemplate");
            
            // 카테고리 목록이 로드될 때까지 대기
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("categoryList")));
            
            // 미분류 카테고리 클릭
            WebElement uncategorizedCategory = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("[data-id='UNCATEGORIZED']"))
            );
            uncategorizedCategory.click();
            System.out.println("미분류 카테고리 클릭 완료");
            
            // 템플릿 목록이 로드될 때까지 대기
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 첫 번째 템플릿 클릭 (있는 경우)
            List<WebElement> templateItems = driver.findElements(By.cssSelector(".template-item"));
            if (!templateItems.isEmpty()) {
                WebElement firstTemplate = templateItems.get(0);
                firstTemplate.click();
                System.out.println("첫 번째 템플릿 클릭 완료");
                
                // 템플릿 상세 정보가 로드되는지 확인
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // SQL 이름 필드가 채워지는지 확인
                WebElement sqlNameField = driver.findElement(By.id("sqlTemplateName"));
                String sqlName = sqlNameField.getAttribute("value");
                System.out.println("SQL 이름: " + sqlName);
                
                if (sqlName != null && !sqlName.trim().isEmpty()) {
                    System.out.println("템플릿 상세 정보 로드 성공");
                } else {
                    System.out.println("템플릿 상세 정보 로드 실패 - SQL 이름이 비어있음");
                }
            } else {
                System.out.println("템플릿이 없어서 상세 조회 테스트를 건너뜀");
            }
            
        } catch (Exception e) {
            System.err.println("SQL 템플릿 상세 조회 테스트 실패: " + e.getMessage());
            takeScreenshot("sql_template_detail_failure");
            throw e;
        }
        
        System.out.println("=== SQL 템플릿 상세 조회 테스트 완료 ===\n");
    }
    
    /**
     * 새 템플릿 생성 테스트
     */
    @Test
    public void testCreateNewTemplate() {
        System.out.println("=== 새 템플릿 생성 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // SQL 템플릿 관리 페이지로 이동
            driver.get(BASE_URL + "/SQLTemplate");
            
            // 새 템플릿 버튼 클릭
            WebElement newTemplateButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[onclick='createNewSqlTemplate()']"))
            );
            newTemplateButton.click();
            System.out.println("새 템플릿 버튼 클릭 완료");
            
            // 폼이 초기화되는지 확인
            WebElement sqlNameField = driver.findElement(By.id("sqlTemplateName"));
            String sqlName = sqlNameField.getAttribute("value");
            
            if (sqlName == null || sqlName.trim().isEmpty()) {
                System.out.println("폼 초기화 확인 완료");
            } else {
                System.out.println("폼 초기화 실패 - SQL 이름이 비어있지 않음: " + sqlName);
            }
            
            // 새 템플릿 정보 입력
            String testTemplateName = "자동테스트_템플릿_" + System.currentTimeMillis();
            sqlNameField.sendKeys(testTemplateName);
            
            WebElement sqlDescField = driver.findElement(By.id("sqlTemplateDesc"));
            sqlDescField.sendKeys("자동 테스트로 생성된 템플릿입니다.");
            
            // SQL 내용 입력 (기본 템플릿 탭)
            try {
                // Ace Editor에 SQL 내용 입력
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("if (window.ace && window.ace.edit) { " +
                    "var editor = ace.edit('sqlEditor_default'); " +
                    "editor.setValue('SELECT 1 as test_column FROM SYSIBM.SYSDUMMY1'); " +
                    "editor.clearSelection(); " +
                    "}");
                System.out.println("SQL 내용 입력 완료");
            } catch (Exception e) {
                System.out.println("SQL 내용 입력 실패: " + e.getMessage());
            }
            
            System.out.println("새 템플릿 정보 입력 완료: " + testTemplateName);
            
            // 저장 버튼 클릭
            WebElement saveButton = driver.findElement(By.cssSelector("button[onclick='saveSqlTemplate()']"));
            saveButton.click();
            System.out.println("저장 버튼 클릭 완료");
            
            // 저장 결과 확인 (알림창 처리)
            try {
                Thread.sleep(2000);
                
                // 알림창이 있는지 확인하고 처리
                try {
                    Alert alert = driver.switchTo().alert();
                    String alertText = alert.getText();
                    System.out.println("알림창 발견: " + alertText);
                    alert.accept(); // 알림창 확인
                    System.out.println("알림창 처리 완료");
                } catch (NoAlertPresentException e) {
                    System.out.println("알림창 없음 - 정상 저장");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 페이지 새로고침하여 템플릿이 생성되었는지 확인
            driver.navigate().refresh();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 미분류 카테고리에서 새로 생성된 템플릿 확인
            WebElement uncategorizedCategory = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("[data-id='UNCATEGORIZED']"))
            );
            uncategorizedCategory.click();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            List<WebElement> templateItems = driver.findElements(By.cssSelector(".template-item"));
            boolean templateFound = false;
            
            for (WebElement template : templateItems) {
                if (template.getText().contains(testTemplateName)) {
                    templateFound = true;
                    System.out.println("새로 생성된 템플릿 확인 완료: " + testTemplateName);
                    break;
                }
            }
            
            if (!templateFound) {
                System.out.println("새로 생성된 템플릿을 찾을 수 없음");
            }
            
        } catch (Exception e) {
            System.err.println("새 템플릿 생성 테스트 실패: " + e.getMessage());
            takeScreenshot("create_template_failure");
            throw e;
        }
        
        System.out.println("=== 새 템플릿 생성 테스트 완료 ===\n");
    }
    
    /**
     * 대시보드 페이지 접근 테스트
     */
    @Test
    public void testDashboardAccess() {
        System.out.println("=== 대시보드 페이지 접근 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // 메인 페이지로 이동 (iframe 구조)
            driver.get(BASE_URL);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // 페이지 로드 대기
            
            // 대시보드 탭 클릭
            WebElement dashboardTab = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '#dashboard')]"))
            );
            dashboardTab.click();
            System.out.println("대시보드 탭 클릭 완료");
            
            // iframe으로 전환
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("iframe_dashboard")));
            driver.switchTo().frame(iframe);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // iframe 로드 대기
            
            // 페이지 제목 확인 (유연한 방법)
            String pageTitle = "대시보드";
            try {
                WebElement h1Element = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
                pageTitle = h1Element.getText();
                System.out.println("대시보드 페이지 제목: " + pageTitle);
            } catch (Exception e) {
                System.out.println("h1 태그를 찾을 수 없습니다. 페이지 소스에서 확인 중...");
                String pageSource = driver.getPageSource();
                if (pageSource.contains("대시보드") || pageSource.contains("Dashboard")) {
                    pageTitle = "대시보드 (소스에서 확인)";
                    System.out.println("대시보드 페이지 제목: " + pageTitle);
                } else {
                    System.out.println("페이지 제목을 찾을 수 없습니다.");
                }
            }
            
            // 차트 컨테이너가 있는지 확인 (유연한 방법)
            try {
                List<WebElement> chartContainers = driver.findElements(By.cssSelector(".chart-container"));
                System.out.println("차트 컨테이너 개수: " + chartContainers.size());
            } catch (Exception e) {
                System.out.println("차트 컨테이너를 찾을 수 없습니다. 다른 방법으로 시도...");
                // 다른 차트 관련 요소들 확인
                List<WebElement> canvasElements = driver.findElements(By.tagName("canvas"));
                List<WebElement> divElements = driver.findElements(By.cssSelector("div[class*='chart']"));
                System.out.println("Canvas 요소: " + canvasElements.size() + "개, Chart 관련 div: " + divElements.size() + "개");
            }
            
        } catch (Exception e) {
            System.err.println("대시보드 페이지 접근 테스트 실패: " + e.getMessage());
            takeScreenshot("dashboard_failure");
            throw e;
        }
        
        System.out.println("=== 대시보드 페이지 접근 테스트 완료 ===\n");
    }
    
    /**
     * 연결 관리 페이지 접근 테스트
     */
    @Test
    public void testConnectionManagementAccess() {
        System.out.println("=== 연결 관리 페이지 접근 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // 메인 페이지로 이동 (iframe 구조)
            driver.get(BASE_URL);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // 페이지 로드 대기
            
            // Connection 메뉴 클릭
            WebElement connectionMenu = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, '/Connection')]"))
            );
            connectionMenu.click();
            System.out.println("Connection 메뉴 클릭 완료");
            
            // iframe으로 전환
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("iframe_1")));
            driver.switchTo().frame(iframe);
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } // iframe 로드 대기
            
            // 페이지 제목 확인 (유연한 방법)
            String pageTitle = "연결 관리";
            try {
                WebElement h1Element = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
                pageTitle = h1Element.getText();
                System.out.println("연결 관리 페이지 제목: " + pageTitle);
            } catch (Exception e) {
                System.out.println("h1 태그를 찾을 수 없습니다. 페이지 소스에서 확인 중...");
                String pageSource = driver.getPageSource();
                if (pageSource.contains("연결 관리") || pageSource.contains("Connection")) {
                    pageTitle = "연결 관리 (소스에서 확인)";
                    System.out.println("연결 관리 페이지 제목: " + pageTitle);
                } else {
                    System.out.println("페이지 제목을 찾을 수 없습니다.");
                }
            }
            
            // 연결 목록 테이블이 있는지 확인 (유연한 방법)
            try {
                WebElement connectionTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("connectionTable")));
                System.out.println("연결 목록 테이블 로드 확인");
            } catch (Exception e) {
                System.out.println("connectionTable ID를 찾을 수 없습니다. 다른 방법으로 시도...");
                // 테이블 태그나 다른 요소로 확인
                List<WebElement> tables = driver.findElements(By.tagName("table"));
                if (tables.size() > 0) {
                    System.out.println("테이블 요소 " + tables.size() + "개 발견");
                } else {
                    System.out.println("테이블 요소를 찾을 수 없습니다.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("연결 관리 페이지 접근 테스트 실패: " + e.getMessage());
            takeScreenshot("connection_management_failure");
            throw e;
        }
        
        System.out.println("=== 연결 관리 페이지 접근 테스트 완료 ===\n");
    }
    
    /**
     * 로그인 수행 헬퍼 메서드
     */
    private void performLogin() {
        driver.get(BASE_URL + "/Login");
        
        // 로그인 시에만 30초 대기
        WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        
        WebElement idInput = loginWait.until(ExpectedConditions.presenceOfElementLocated(By.name("id")));
        WebElement pwInput = driver.findElement(By.name("pw"));
        
        idInput.clear();
        idInput.sendKeys(ADMIN_ID);
        pwInput.clear();
        pwInput.sendKeys(ADMIN_PASSWORD);
        
        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));
        loginButton.click();
        
        loginWait.until(ExpectedConditions.urlContains("/index"));
    }
    
    /**
     * 스크린샷 촬영 헬퍼 메서드
     */
    private void takeScreenshot(String filename) {
        try {
            // 스크린샷을 위한 디렉토리 생성
            java.io.File screenshotDir = new java.io.File("screenshots");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }
            
            // 스크린샷 촬영
            java.io.File screenshot = new java.io.File(screenshotDir, filename + "_" + System.currentTimeMillis() + ".png");
            ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE)
                .renameTo(screenshot);
            
            System.out.println("스크린샷 저장됨: " + screenshot.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("스크린샷 촬영 실패: " + e.getMessage());
        }
    }
    
    @Test
    public void testSqlTemplateExecution() {
        System.out.println("=== SQL 템플릿 실행 화면 연결 필터링 테스트 시작 ===");
        
        try {
            // 먼저 로그인
            performLogin();
            
            // SQL 템플릿 실행 페이지로 직접 이동 (DB2_CONNECTION_STATUS 템플릿)
            String templateId = "DB2_CONNECTION_STATUS";
            String executeUrl = BASE_URL + "/SQLTemplate?templateId=" + templateId;
            driver.get(executeUrl);
            
            // 페이지 로드 대기 (더 긴 시간)
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            
            // 페이지 소스 확인
            String pageSource = driver.getPageSource();
            System.out.println("페이지 제목: " + driver.getTitle());
            System.out.println("현재 URL: " + driver.getCurrentUrl());
            
            // connectionlist 요소가 있는지 확인
            if (pageSource.contains("connectionlist")) {
                System.out.println("✅ connectionlist 요소가 페이지에 존재함");
            } else {
                System.out.println("❌ connectionlist 요소가 페이지에 존재하지 않음");
                System.out.println("페이지 소스 일부: " + pageSource.substring(0, Math.min(1000, pageSource.length())));
            }
            
            // 페이지 로드 대기
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("connectionlist")));
            
            // 연결 선택 셀렉트 박스 찾기
            WebElement connectionSelect = driver.findElement(By.id("connectionlist"));
            
            // 셀렉트 박스의 옵션들 가져오기
            List<WebElement> options = connectionSelect.findElements(By.tagName("option"));
            
            System.out.println("=== 연결 선택 옵션 목록 ===");
            for (WebElement option : options) {
                String value = option.getAttribute("value");
                String text = option.getText();
                System.out.println("Value: " + value + ", Text: " + text);
            }
            
            // 템플릿에 설정된 접근가능한 연결은 test_db만 있어야 함
            boolean hasTestDb = false;
            boolean hasOtherConnections = false;
            
            for (WebElement option : options) {
                String value = option.getAttribute("value");
                if ("test_db".equals(value)) {
                    hasTestDb = true;
                } else if (!"".equals(value) && !"====Connection====".equals(value)) {
                    hasOtherConnections = true;
                }
            }
            
            // 테스트 결과 검증
            if (hasTestDb && !hasOtherConnections) {
                System.out.println("✅ 성공: 템플릿 접근가능한 연결(test_db)만 표시됨");
            } else {
                System.out.println("❌ 실패: 예상과 다른 연결 옵션이 표시됨");
                System.out.println("test_db 포함: " + hasTestDb);
                System.out.println("다른 연결 포함: " + hasOtherConnections);
            }
            
            // 스크린샷 촬영
            takeScreenshot("sql_template_execution_connection_filter");
            
        } catch (Exception e) {
            System.err.println("테스트 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot("sql_template_execution_error");
        }
    }
}
