package kr.Windmill.controller;

import kr.Windmill.util.UserMigrationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 데이터 마이그레이션을 위한 관리자 컨트롤러
 */
@Controller
@RequestMapping("/Migration")
public class MigrationController {
    
    @Autowired
    private UserMigrationUtil migrationUtil;
    
    // 마이그레이션 관리 화면
    @RequestMapping("")
    public String migrationMain() {
        return "Migration";
    }
    
    // 마이그레이션 상태 확인
    @ResponseBody
    @RequestMapping("/status")
    public Map<String, Object> checkStatus(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) session.getAttribute("memberId");
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인 (임시로 모든 로그인 사용자 허용)
            // TODO: 실제 권한 체크 로직 구현
            
            Map<String, Object> status = migrationUtil.checkMigrationStatus();
            result.put("success", true);
            result.put("data", status);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "상태 확인 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 백업 생성
    @ResponseBody
    @RequestMapping(value = "/backup", method = RequestMethod.POST)
    public Map<String, Object> createBackup(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) session.getAttribute("memberId");
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            boolean success = migrationUtil.createBackup();
            if (success) {
                result.put("success", true);
                result.put("message", "백업이 생성되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "백업 생성에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "백업 생성 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 마이그레이션 실행
    @ResponseBody
    @RequestMapping(value = "/execute", method = RequestMethod.POST)
    public Map<String, Object> executeMigration(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) session.getAttribute("memberId");
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 마이그레이션 실행
            Map<String, Object> migrationResult = migrationUtil.migrateUsersToDatabase();
            
            result.put("success", migrationResult.get("success"));
            result.put("message", migrationResult.get("message"));
            result.put("data", migrationResult);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "마이그레이션 중 오류가 발생했습니다.");
        }
        
        return result;
    }
}
