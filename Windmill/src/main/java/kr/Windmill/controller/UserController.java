package kr.Windmill.controller;

import kr.Windmill.service.UserService;
import kr.Windmill.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/User")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PermissionService permissionService;
    
    // 사용자 관리 화면
    @RequestMapping("")
    public String userMain() {
        return "User";
    }
    
    // 사용자 목록 조회
    @ResponseBody
    @RequestMapping("/list")
    public Map<String, Object> getUserList(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(userId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            result.put("success", true);
            result.put("data", userService.getUserList());
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "사용자 목록 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 사용자 상세 조회
    @ResponseBody
    @RequestMapping("/detail")
    public Map<String, Object> getUserDetail(@RequestParam String userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String currentUserId = (String) session.getAttribute("userId");
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(currentUserId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            Map<String, Object> userDetail = userService.getUserDetail(userId);
            if (userDetail == null) {
                result.put("success", false);
                result.put("message", "사용자를 찾을 수 없습니다.");
                return result;
            }
            
            result.put("success", true);
            result.put("data", userDetail);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "사용자 상세 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 사용자 생성
    @ResponseBody
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Map<String, Object> createUser(@RequestBody Map<String, Object> userData, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String currentUserId = (String) session.getAttribute("userId");
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(currentUserId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            userData.put("createdBy", currentUserId);
            
            boolean success = userService.createUser(userData);
            if (success) {
                result.put("success", true);
                result.put("message", "사용자가 생성되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "사용자 생성에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "사용자 생성 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 사용자 수정
    @ResponseBody
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Map<String, Object> updateUser(@RequestParam String userId, @RequestBody Map<String, Object> userData, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String currentUserId = (String) session.getAttribute("userId");
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(currentUserId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            userData.put("modifiedBy", currentUserId);
            
            boolean success = userService.updateUser(userId, userData);
            if (success) {
                result.put("success", true);
                result.put("message", "사용자 정보가 수정되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "사용자 정보 수정에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "사용자 정보 수정 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 사용자 삭제
    @ResponseBody
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Map<String, Object> deleteUser(@RequestParam String userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String currentUserId = (String) session.getAttribute("userId");
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(currentUserId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            // 자기 자신 삭제 방지
            if (userId.equals(currentUserId)) {
                result.put("success", false);
                result.put("message", "자기 자신은 삭제할 수 없습니다.");
                return result;
            }
            
            boolean success = userService.deleteUser(userId);
            if (success) {
                result.put("success", true);
                result.put("message", "사용자가 삭제되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "사용자 삭제에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "사용자 삭제 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 그룹 목록 조회
    @ResponseBody
    @RequestMapping("/groups")
    public Map<String, Object> getGroupList(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            result.put("success", true);
            result.put("data", userService.getGroupList());
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "그룹 목록 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    // 사용자 그룹 매핑
    @ResponseBody
    @RequestMapping(value = "/assignGroup", method = RequestMethod.POST)
    public Map<String, Object> assignUserToGroup(@RequestParam String userId, @RequestParam String groupId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String currentUserId = (String) session.getAttribute("userId");
            if (currentUserId == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            
            // 관리자 권한 확인
            if (!permissionService.isAdmin(currentUserId)) {
                result.put("success", false);
                result.put("message", "관리자 권한이 필요합니다.");
                return result;
            }
            
            boolean success = userService.assignUserToGroup(userId, groupId, currentUserId);
            if (success) {
                result.put("success", true);
                result.put("message", "그룹이 할당되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "그룹 할당에 실패했습니다.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "그룹 할당 중 오류가 발생했습니다.");
        }
        
        return result;
    }
}
