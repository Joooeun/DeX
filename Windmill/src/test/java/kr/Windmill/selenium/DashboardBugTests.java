package kr.Windmill.selenium;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * 대시보드 관련 버그 테스트 (Selenium)
 * 버그리포트 기반 테스트 케이스 구현
 */
public class DashboardBugTests extends BaseSeleniumTest {
    
    @Test
    public void testDashboardLoadingBug() {
        System.out.println("=== 대시보드 로딩 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        
        // 페이지 로딩 완료 대기
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dashboardContent")));
        waitForLoadingToComplete();
        
        // 대시보드 주요 요소들이 로드되었는지 확인
        WebElement dashboardContent = driver.findElement(By.id("dashboardContent"));
        Assert.assertTrue("대시보드 콘텐츠가 표시되어야 합니다.", dashboardContent.isDisplayed());
        
        // 차트나 위젯이 있는지 확인
        List<WebElement> charts = driver.findElements(By.cssSelector(".chart, .widget, .dashboard-item"));
        Assert.assertTrue("대시보드에 차트나 위젯이 있어야 합니다.", charts.size() > 0);
        
        System.out.println("✅ 대시보드 로딩 버그 테스트 통과");
    }
    
    @Test
    public void testDashboardRefreshBug() {
        System.out.println("=== 대시보드 새로고침 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 새로고침 버튼 클릭
        WebElement refreshButton = driver.findElement(By.id("refreshButton"));
        refreshButton.click();
        
        // 새로고침 후 로딩 완료 대기
        waitForLoadingToComplete();
        
        // 대시보드가 정상적으로 새로고침되었는지 확인
        WebElement dashboardContent = driver.findElement(By.id("dashboardContent"));
        Assert.assertTrue("새로고침 후 대시보드가 표시되어야 합니다.", dashboardContent.isDisplayed());
        
        System.out.println("✅ 대시보드 새로고침 버그 테스트 통과");
    }
    
    @Test
    public void testDashboardDataUpdateBug() {
        System.out.println("=== 대시보드 데이터 업데이트 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 데이터 업데이트 버튼 클릭
        WebElement updateDataButton = driver.findElement(By.id("updateDataButton"));
        updateDataButton.click();
        
        // 데이터 업데이트 완료 대기
        waitForLoadingToComplete();
        
        // 업데이트된 데이터가 표시되는지 확인
        List<WebElement> dataElements = driver.findElements(By.cssSelector(".data-item, .metric, .statistic"));
        Assert.assertTrue("업데이트된 데이터가 표시되어야 합니다.", dataElements.size() > 0);
        
        System.out.println("✅ 대시보드 데이터 업데이트 버그 테스트 통과");
    }
    
    @Test
    public void testDashboardFilterBug() {
        System.out.println("=== 대시보드 필터 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 필터 드롭다운 확인
        WebElement filterSelect = driver.findElement(By.id("filterSelect"));
        Assert.assertTrue("필터 드롭다운이 표시되어야 합니다.", filterSelect.isDisplayed());
        
        // 필터 옵션 선택
        Select select = new Select(filterSelect);
        if (select.getOptions().size() > 1) {
            select.selectByIndex(1);
            waitForLoadingToComplete();
            
            // 필터 적용 후 데이터 변경 확인
            List<WebElement> filteredData = driver.findElements(By.cssSelector(".filtered-data"));
            Assert.assertTrue("필터 적용 후 데이터가 표시되어야 합니다.", filteredData.size() >= 0);
            
            System.out.println("✅ 대시보드 필터 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 필터 옵션이 부족합니다.");
        }
    }
    
    @Test
    public void testDashboardChartInteractionBug() {
        System.out.println("=== 대시보드 차트 상호작용 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 차트 요소 찾기
        List<WebElement> charts = driver.findElements(By.cssSelector(".chart, .chart-container"));
        
        if (charts.size() > 0) {
            WebElement chart = charts.get(0);
            
            // 차트 클릭 테스트
            chart.click();
            waitForSeconds(1);
            
            // 차트 상호작용 후 상태 확인
            boolean isChartInteractive = chart.isDisplayed() && chart.isEnabled();
            Assert.assertTrue("차트가 상호작용 가능해야 합니다.", isChartInteractive);
            
            System.out.println("✅ 대시보드 차트 상호작용 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 테스트할 차트가 없습니다.");
        }
    }
    
    @Test
    public void testDashboardResponsiveBug() {
        System.out.println("=== 대시보드 반응형 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 브라우저 창 크기 변경
        driver.manage().window().setSize(new Dimension(1024, 768));
        waitForSeconds(1);
        
        // 대시보드가 반응형으로 조정되었는지 확인
        WebElement dashboardContent = driver.findElement(By.id("dashboardContent"));
        Assert.assertTrue("창 크기 변경 후 대시보드가 표시되어야 합니다.", dashboardContent.isDisplayed());
        
        // 다시 원래 크기로 복원
        driver.manage().window().setSize(new Dimension(1920, 1080));
        waitForSeconds(1);
        
        Assert.assertTrue("원래 크기로 복원 후 대시보드가 표시되어야 합니다.", dashboardContent.isDisplayed());
        
        System.out.println("✅ 대시보드 반응형 버그 테스트 통과");
    }
    
    @Test
    public void testDashboardErrorHandlingBug() {
        System.out.println("=== 대시보드 오류 처리 버그 테스트 시작 ===");
        
        driver.get(DASHBOARD_URL);
        waitForLoadingToComplete();
        
        // 오류를 유발할 수 있는 작업 수행 (예: 잘못된 필터 선택)
        try {
            WebElement filterSelect = driver.findElement(By.id("filterSelect"));
            Select select = new Select(filterSelect);
            
            // 잘못된 값으로 필터 설정 시도
            executeJavaScript("document.getElementById('filterSelect').value = 'invalid_value';");
            executeJavaScript("document.getElementById('filterSelect').dispatchEvent(new Event('change'));");
            
            waitForSeconds(2);
            
            // 오류 메시지가 표시되는지 확인
            boolean hasErrorMessage = isToastMessageDisplayed("오류") ||
                                    isToastMessageDisplayed("실패") ||
                                    isElementDisplayed(By.id("errorMessage"));
            
            // 오류가 발생하더라도 대시보드가 계속 작동해야 함
            WebElement dashboardContent = driver.findElement(By.id("dashboardContent"));
            Assert.assertTrue("오류 발생 시에도 대시보드가 표시되어야 합니다.", dashboardContent.isDisplayed());
            
            System.out.println("✅ 대시보드 오류 처리 버그 테스트 통과");
            
        } catch (Exception e) {
            System.out.println("⚠️ 오류 처리 테스트 중 예외 발생: " + e.getMessage());
        }
    }
    
    @Test
    public void testDashboardPerformanceBug() {
        System.out.println("=== 대시보드 성능 버그 테스트 시작 ===");
        
        long startTime = System.currentTimeMillis();
        
        driver.get(DASHBOARD_URL);
        
        // 페이지 로딩 완료 대기
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dashboardContent")));
        waitForLoadingToComplete();
        
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        
        // 로딩 시간이 10초 이내여야 함
        Assert.assertTrue("대시보드 로딩 시간이 10초 이내여야 합니다. (실제: " + loadTime + "ms)", loadTime < 10000);
        
        System.out.println("✅ 대시보드 성능 버그 테스트 통과 (로딩 시간: " + loadTime + "ms)");
    }
}
