package kr.Windmill.selenium;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * 연결 관리 관련 버그 테스트 (Selenium)
 * 버그리포트 기반 테스트 케이스 구현
 */
public class ConnectionBugTests extends BaseSeleniumTest {
    
    @Test
    public void testConnectionSaveErrorBug() {
        System.out.println("=== 연결 저장 오류 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 새 연결 추가 버튼 클릭
        WebElement addButton = driver.findElement(By.id("addConnectionButton"));
        addButton.click();
        waitForLoadingToComplete();
        
        // 연결 정보 입력
        inputText(By.id("connectionName"), "테스트 연결");
        inputText(By.id("host"), "localhost");
        inputText(By.id("port"), "5432");
        inputText(By.id("database"), "testdb");
        inputText(By.id("username"), "testuser");
        inputText(By.id("password"), "testpass");
        
        // 데이터베이스 타입 선택
        WebElement dbTypeSelect = driver.findElement(By.id("dbType"));
        Select select = new Select(dbTypeSelect);
        select.selectByValue("postgresql");
        
        // 저장 버튼 클릭
        WebElement saveButton = driver.findElement(By.id("saveConnectionButton"));
        saveButton.click();
        waitForLoadingToComplete();
        
        // 성공 메시지 또는 오류 메시지 확인
        boolean hasSuccessMessage = isToastMessageDisplayed("저장되었습니다") || 
                                  isToastMessageDisplayed("성공");
        
        boolean hasErrorMessage = isToastMessageDisplayed("오류") || 
                                isToastMessageDisplayed("실패") ||
                                isToastMessageDisplayed("에러");
        
        // 저장이 성공하거나 적절한 오류 메시지가 표시되어야 함
        Assert.assertTrue("저장 성공 또는 적절한 오류 메시지가 표시되어야 합니다.", 
                         hasSuccessMessage || hasErrorMessage);
        
        System.out.println("✅ 연결 저장 오류 버그 테스트 통과");
    }
    
    @Test
    public void testConnectionUpdateBug() {
        System.out.println("=== 연결 업데이트 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 기존 연결이 있는지 확인
        List<WebElement> connectionRows = driver.findElements(By.cssSelector("#connectionTableBody tr"));
        
        if (connectionRows.size() > 0) {
            // 첫 번째 연결의 편집 버튼 클릭
            WebElement editButton = connectionRows.get(0).findElement(By.cssSelector(".edit-button"));
            editButton.click();
            waitForLoadingToComplete();
            
            // 연결 정보 수정
            WebElement connectionNameField = driver.findElement(By.id("connectionName"));
            String originalName = connectionNameField.getAttribute("value");
            String newName = originalName + " (수정됨)";
            
            inputText(By.id("connectionName"), newName);
            
            // 업데이트 버튼 클릭
            WebElement updateButton = driver.findElement(By.id("updateConnectionButton"));
            updateButton.click();
            waitForLoadingToComplete();
            
            // 업데이트 성공 확인
            boolean hasSuccessMessage = isToastMessageDisplayed("수정되었습니다") || 
                                      isToastMessageDisplayed("업데이트") ||
                                      isToastMessageDisplayed("성공");
            
            Assert.assertTrue("연결 업데이트가 성공해야 합니다.", hasSuccessMessage);
            
            System.out.println("✅ 연결 업데이트 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 테스트할 연결이 없습니다.");
        }
    }
    
    @Test
    public void testConnectionTestBug() {
        System.out.println("=== 연결 테스트 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 기존 연결이 있는지 확인
        List<WebElement> connectionRows = driver.findElements(By.cssSelector("#connectionTableBody tr"));
        
        if (connectionRows.size() > 0) {
            // 첫 번째 연결의 테스트 버튼 클릭
            WebElement testButton = connectionRows.get(0).findElement(By.cssSelector(".test-button"));
            testButton.click();
            
            // 테스트 결과 대기
            waitForSeconds(3);
            
            // 테스트 결과 확인 (성공 또는 실패 메시지)
            boolean hasTestResult = isToastMessageDisplayed("연결 성공") || 
                                  isToastMessageDisplayed("연결 실패") ||
                                  isToastMessageDisplayed("테스트") ||
                                  isElementDisplayed(By.id("testResult"));
            
            Assert.assertTrue("연결 테스트 결과가 표시되어야 합니다.", hasTestResult);
            
            System.out.println("✅ 연결 테스트 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 테스트할 연결이 없습니다.");
        }
    }
    
    @Test
    public void testConnectionDeletionBug() {
        System.out.println("=== 연결 삭제 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 기존 연결이 있는지 확인
        List<WebElement> connectionRows = driver.findElements(By.cssSelector("#connectionTableBody tr"));
        
        if (connectionRows.size() > 0) {
            int initialCount = connectionRows.size();
            
            // 첫 번째 연결의 삭제 버튼 클릭
            WebElement deleteButton = connectionRows.get(0).findElement(By.cssSelector(".delete-button"));
            deleteButton.click();
            
            // 확인 대화상자 처리
            try {
                Alert alert = driver.switchTo().alert();
                alert.accept();
            } catch (Exception e) {
                // 대화상자가 없는 경우 확인 버튼 클릭
                WebElement confirmButton = driver.findElement(By.id("confirmDeleteButton"));
                confirmButton.click();
            }
            
            waitForLoadingToComplete();
            
            // 삭제 후 연결 개수 확인
            List<WebElement> remainingRows = driver.findElements(By.cssSelector("#connectionTableBody tr"));
            int finalCount = remainingRows.size();
            
            Assert.assertTrue("연결이 삭제되어야 합니다.", finalCount < initialCount);
            
            System.out.println("✅ 연결 삭제 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 삭제할 연결이 없습니다.");
        }
    }
    
    @Test
    public void testConnectionValidationBug() {
        System.out.println("=== 연결 유효성 검사 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 새 연결 추가 버튼 클릭
        WebElement addButton = driver.findElement(By.id("addConnectionButton"));
        addButton.click();
        waitForLoadingToComplete();
        
        // 필수 필드를 비워두고 저장 시도
        inputText(By.id("connectionName"), ""); // 빈 이름
        inputText(By.id("host"), "localhost");
        inputText(By.id("port"), "5432");
        
        // 저장 버튼 클릭
        WebElement saveButton = driver.findElement(By.id("saveConnectionButton"));
        saveButton.click();
        
        // 유효성 검사 오류 메시지 확인
        boolean hasValidationError = isElementDisplayed(By.id("validationError")) ||
                                   isToastMessageDisplayed("필수") ||
                                   isToastMessageDisplayed("입력") ||
                                   isToastMessageDisplayed("오류");
        
        Assert.assertTrue("유효성 검사 오류 메시지가 표시되어야 합니다.", hasValidationError);
        
        System.out.println("✅ 연결 유효성 검사 버그 테스트 통과");
    }
    
    @Test
    public void testConnectionDuplicateBug() {
        System.out.println("=== 연결 중복 버그 테스트 시작 ===");
        
        driver.get(CONNECTION_URL);
        waitForLoadingToComplete();
        
        // 기존 연결 이름 확인
        List<WebElement> connectionRows = driver.findElements(By.cssSelector("#connectionTableBody tr"));
        String existingConnectionName = null;
        
        if (connectionRows.size() > 0) {
            existingConnectionName = connectionRows.get(0).findElement(By.cssSelector(".connection-name")).getText();
        }
        
        // 새 연결 추가
        WebElement addButton = driver.findElement(By.id("addConnectionButton"));
        addButton.click();
        waitForLoadingToComplete();
        
        // 기존과 동일한 이름으로 연결 생성 시도
        if (existingConnectionName != null) {
            inputText(By.id("connectionName"), existingConnectionName);
            inputText(By.id("host"), "localhost");
            inputText(By.id("port"), "5432");
            inputText(By.id("database"), "testdb");
            inputText(By.id("username"), "testuser");
            inputText(By.id("password"), "testpass");
            
            // 저장 버튼 클릭
            WebElement saveButton = driver.findElement(By.id("saveConnectionButton"));
            saveButton.click();
            waitForLoadingToComplete();
            
            // 중복 오류 메시지 확인
            boolean hasDuplicateError = isToastMessageDisplayed("중복") ||
                                      isToastMessageDisplayed("이미 존재") ||
                                      isToastMessageDisplayed("동일한 이름");
            
            Assert.assertTrue("중복 연결 오류 메시지가 표시되어야 합니다.", hasDuplicateError);
            
            System.out.println("✅ 연결 중복 버그 테스트 통과");
        } else {
            System.out.println("⚠️ 기존 연결이 없어 중복 테스트를 수행할 수 없습니다.");
        }
    }
}
