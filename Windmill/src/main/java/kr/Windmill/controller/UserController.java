package kr.Windmill.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.PermissionService;
import kr.Windmill.service.UserService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
@RequestMapping("/User")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
	private final Common com;
	private final Log cLog;

	@Autowired
	public UserController(Common common, Log log, UserService userService) {
		this.com = common;
		this.cLog = log;
		this.userService = userService;
	}

	@Autowired
	private UserService userService;

	@Autowired
	private PermissionService permissionService;

	// 사용자 관리 화면
	@RequestMapping(path = "", method = RequestMethod.GET)
	public ModelAndView User(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		try {
			String userId = (String) session.getAttribute("memberId");

			// 관리자 권한 확인
			boolean isAdmin = permissionService.isAdmin(userId);

			if (!isAdmin) {
				
				mv.setViewName("redirect:/index");
				return mv;
			}

			mv.setViewName("User");
			return mv;
		} catch (Exception e) {
			mv.setViewName("redirect:/index");
			return mv;
		}
	}

	// 사용자 목록 조회
	@ResponseBody
	@RequestMapping("/list")
	public Map<String, Object> getUserList(@RequestParam(required = false) String searchKeyword, 
										  @RequestParam(required = false) String groupFilter,
										  @RequestParam(defaultValue = "1") int page, 
										  @RequestParam(defaultValue = "5") int pageSize, 
										  HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String userId = (String) session.getAttribute("memberId");
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

			Map<String, Object> userData = userService.getUserList(searchKeyword, groupFilter, page, pageSize);
			result.put("success", true);
			result.put("data", userData.get("userList"));
			result.put("pagination", userData);

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
			String currentUserId = (String) session.getAttribute("memberId");
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
			String currentUserId = (String) session.getAttribute("memberId");
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
				// 그룹 할당 처리
				String groupId = (String) userData.get("groupId");
				if (groupId != null && !groupId.trim().isEmpty()) {
					userService.assignUserToGroup((String) userData.get("userId"), groupId, currentUserId);
				}
				
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
			String currentUserId = (String) session.getAttribute("memberId");
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
				// 그룹 할당 처리
				String groupId = (String) userData.get("groupId");
				if (groupId != null && !groupId.trim().isEmpty()) {
					userService.assignUserToGroup(userId, groupId, currentUserId);
				}
				
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
			String currentUserId = (String) session.getAttribute("memberId");
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
			String userId = (String) session.getAttribute("memberId");
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
			String currentUserId = (String) session.getAttribute("memberId");
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

	// SQL 템플릿 카테고리 권한 조회
	@ResponseBody
	@RequestMapping("/sqlTemplateCategoryPermissions")
	public Map<String, Object> getSqlTemplateCategoryPermissions(@RequestParam String userId, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			result.put("success", true);
			result.put("data", userService.getSqlTemplateCategoryPermissions(userId));

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "SQL 템플릿 카테고리 권한 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 연결 정보 권한 조회
	@ResponseBody
	@RequestMapping("/connectionPermissions")
	public Map<String, Object> getConnectionPermissions(@RequestParam String userId, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			result.put("success", true);
			result.put("data", userService.getConnectionPermissions(userId));

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "연결 정보 권한 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 권한 저장
	@ResponseBody
	@RequestMapping(value = "/savePermissions", method = RequestMethod.POST)
	public Map<String, Object> saveUserPermissions(@RequestBody Map<String, Object> requestData, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			String userId = (String) requestData.get("userId");
			Map<String, Object> permissions = (Map<String, Object>) requestData.get("permissions");

			boolean success = userService.saveUserPermissions(userId, permissions, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "권한이 저장되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "권한 저장에 실패했습니다.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "권한 저장 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 사용자 활동 로그 조회
	@ResponseBody
	@RequestMapping("/activityLogs")
	public Map<String, Object> getUserActivityLogs(@RequestParam String userId, @RequestParam(required = false) String dateRange, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			result.put("success", true);
			result.put("data", userService.getUserActivityLogs(userId, dateRange));

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "활동 로그 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 사용자 비밀번호 초기화
	@ResponseBody
	@RequestMapping(value = "/resetPassword", method = RequestMethod.POST)
	public Map<String, Object> resetUserPassword(@RequestParam String userId, @RequestParam(required = false) String defaultPassword, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			// 기본 비밀번호 설정 (제공되지 않은 경우 "1234" 사용)
			String password = (defaultPassword != null && !defaultPassword.trim().isEmpty()) ? defaultPassword : "1234";

			boolean success = userService.resetUserPassword(userId, password, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "비밀번호가 초기화되었습니다. (새 비밀번호: " + password + ")");
			} else {
				result.put("success", false);
				result.put("message", "비밀번호 초기화에 실패했습니다.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "비밀번호 초기화 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 사용자 계정 초기화
	@ResponseBody
	@RequestMapping(value = "/resetAccount", method = RequestMethod.POST)
	public Map<String, Object> resetUserAccount(@RequestParam String userId, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			boolean success = userService.resetUserAccount(userId, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "사용자 계정이 초기화되었습니다. (새 비밀번호: 1234)");
			} else {
				result.put("success", false);
				result.put("message", "계정 초기화에 실패했습니다.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "계정 초기화 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 사용자 계정 잠금 해제
	@ResponseBody
	@RequestMapping(value = "/unlockAccount", method = RequestMethod.POST)
	public Map<String, Object> unlockUserAccount(@RequestParam String userId, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			boolean success = userService.unlockUserAccount(userId, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "사용자 계정 잠금이 해제되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "계정 잠금 해제에 실패했습니다.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "계정 잠금 해제 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 사용자의 현재 그룹 조회
	@ResponseBody
	@RequestMapping("/currentGroup")
	public Map<String, Object> getCurrentUserGroup(@RequestParam String userId, HttpSession session) {
		Map<String, Object> result = new HashMap<>();

		try {
			String currentUserId = (String) session.getAttribute("memberId");
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

			result.put("success", true);
			result.put("data", userService.getCurrentUserGroup(userId));

		} catch (Exception e) {
			e.printStackTrace();
			result.put("success", false);
			result.put("message", "사용자 그룹 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
}
