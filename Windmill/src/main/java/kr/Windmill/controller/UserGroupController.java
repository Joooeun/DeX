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
import kr.Windmill.service.UserGroupService;
import kr.Windmill.util.Common;
import kr.Windmill.util.Log;

@Controller
@RequestMapping("/UserGroup")
public class UserGroupController {

	private static final Logger logger = LoggerFactory.getLogger(UserGroupController.class);
	private final Common com;
	private final Log cLog;

	@Autowired
	public UserGroupController(Common common, Log log, UserGroupService userGroupService) {
		this.com = common;
		this.cLog = log;
		this.userGroupService = userGroupService;
	}

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private PermissionService permissionService;

	// 사용자 그룹 관리 화면
	@RequestMapping(path = "", method = RequestMethod.GET)
	public ModelAndView UserGroup(HttpServletRequest request, ModelAndView mv, HttpSession session) {
		try {
			String userId = (String) session.getAttribute("memberId");

			// 관리자 권한 확인
			boolean isAdmin = permissionService.isAdmin(userId);

			if (!isAdmin) {
				mv.setViewName("redirect:/index");
				return mv;
			}

			mv.setViewName("UserGroup");
			return mv;
		} catch (Exception e) {
			mv.setViewName("redirect:/index");
			return mv;
		}
	}

	// 그룹 목록 조회
	@ResponseBody
	@RequestMapping("/list")
	public Map<String, Object> getGroupList(HttpSession session) {
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

			result.put("success", true);
			result.put("data", userGroupService.getGroupList());

		} catch (Exception e) {
			logger.error("그룹 목록 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// SQL 템플릿 카테고리 목록 조회
	@ResponseBody
	@RequestMapping("/categories")
	public Map<String, Object> getCategories(HttpSession session) {
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

			result.put("success", true);
			result.put("data", permissionService.getAllCategories());

		} catch (Exception e) {
			logger.error("카테고리 목록 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "카테고리 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 그룹별 카테고리 권한 조회
	@ResponseBody
	@RequestMapping("/categoryPermissions")
	public Map<String, Object> getGroupCategoryPermissions(@RequestParam String groupId, HttpSession session) {
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

			result.put("success", true);
			result.put("data", permissionService.getGroupCategoryPermissions(groupId));

		} catch (Exception e) {
			logger.error("그룹 카테고리 권한 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 카테고리 권한 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 그룹에 카테고리 권한 부여 (단순화)
	@ResponseBody
	@RequestMapping(value = "/grantCategoryPermissions", method = RequestMethod.POST)
	public Map<String, Object> grantCategoryPermissions(@RequestBody Map<String, Object> requestData, HttpSession session) {
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

			String groupId = (String) requestData.get("groupId");
			@SuppressWarnings("unchecked")
			java.util.List<String> categoryIds = (java.util.List<String>) requestData.get("categoryIds");

			if (groupId == null || categoryIds == null) {
				result.put("success", false);
				result.put("message", "필수 파라미터가 누락되었습니다.");
				return result;
			}

			// 기존 권한 모두 해제
			permissionService.revokeAllCategoryPermissions(groupId);
			
			// 새로운 권한 부여
			boolean success = true;
			for (String categoryId : categoryIds) {
				if (!permissionService.grantSqlTemplateCategoryPermission(groupId, categoryId, userId)) {
					success = false;
					break;
				}
			}
			
			if (success) {
				result.put("success", true);
				result.put("message", "카테고리 권한이 성공적으로 저장되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "카테고리 권한 저장에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("카테고리 권한 저장 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "카테고리 권한 저장 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 그룹의 카테고리 권한 해제
	@ResponseBody
	@RequestMapping(value = "/revokeCategoryPermission", method = RequestMethod.POST)
	public Map<String, Object> revokeCategoryPermission(@RequestBody Map<String, Object> requestData, HttpSession session) {
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

			String groupId = (String) requestData.get("groupId");
			String categoryId = (String) requestData.get("categoryId");

			if (groupId == null || categoryId == null) {
				result.put("success", false);
				result.put("message", "필수 파라미터가 누락되었습니다.");
				return result;
			}

			boolean success = permissionService.revokeSqlTemplateCategoryPermission(groupId, categoryId);
			
			if (success) {
				result.put("success", true);
				result.put("message", "카테고리 권한이 성공적으로 해제되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "카테고리 권한 해제에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("카테고리 권한 해제 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "카테고리 권한 해제 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 상세 조회
	@ResponseBody
	@RequestMapping("/detail")
	public Map<String, Object> getGroupDetail(@RequestParam String groupId, HttpSession session) {
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

			Map<String, Object> groupDetail = userGroupService.getGroupDetail(groupId);
			if (groupDetail == null) {
				result.put("success", false);
				result.put("message", "그룹을 찾을 수 없습니다.");
				return result;
			}

			result.put("success", true);
			result.put("data", groupDetail);

		} catch (Exception e) {
			logger.error("그룹 상세 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 상세 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 생성
	@ResponseBody
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public Map<String, Object> createGroup(@RequestBody Map<String, Object> groupData, HttpSession session) {
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

			groupData.put("createdBy", currentUserId);

			// groupId가 없으면 자동 생성
			String groupId = (String) groupData.get("groupId");
			if (groupId == null || groupId.trim().isEmpty()) {
				groupId = "GROUP_" + System.currentTimeMillis();
				groupData.put("groupId", groupId);
			}
			
			boolean success = userGroupService.createGroup(groupData);
			if (success) {
				result.put("success", true);
				result.put("message", "그룹이 생성되었습니다.");
				Map<String, Object> data = new HashMap<>();
				data.put("groupId", groupId);
				result.put("data", data);
			} else {
				result.put("success", false);
				result.put("message", "그룹 생성에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("그룹 생성 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 생성 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 수정
	@ResponseBody
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public Map<String, Object> updateGroup(@RequestParam String groupId, @RequestBody Map<String, Object> groupData, HttpSession session) {
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

			groupData.put("modifiedBy", currentUserId);

			boolean success = userGroupService.updateGroup(groupId, groupData);
			if (success) {
				result.put("success", true);
				result.put("message", "그룹 정보가 수정되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "그룹 정보 수정에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("그룹 정보 수정 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 정보 수정 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 삭제
	@ResponseBody
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public Map<String, Object> deleteGroup(@RequestParam String groupId, HttpSession session) {
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

			boolean success = userGroupService.deleteGroup(groupId);
			if (success) {
				result.put("success", true);
				result.put("message", "그룹이 삭제되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "그룹 삭제에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("그룹 삭제 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 삭제 중 오류가 발생했습니다.");
		}

		return result;
	}



	// 사용 가능한 사용자 목록 조회
	@ResponseBody
	@RequestMapping("/availableUsers")
	public Map<String, Object> getAvailableUsers(@RequestParam String groupId, HttpSession session) {
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
			result.put("data", userGroupService.getAvailableUsers(groupId));

		} catch (Exception e) {
			logger.error("사용 가능한 사용자 목록 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "사용 가능한 사용자 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 멤버 목록 조회
	@ResponseBody
	@RequestMapping("/groupMembers")
	public Map<String, Object> getGroupMembers(@RequestParam String groupId, HttpSession session) {
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
			result.put("data", userGroupService.getGroupMembers(groupId));

		} catch (Exception e) {
			logger.error("그룹 멤버 목록 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "그룹 멤버 목록 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 사용자를 그룹에 추가
	@ResponseBody
	@RequestMapping(value = "/addMember", method = RequestMethod.POST)
	public Map<String, Object> addUserToGroup(@RequestParam String groupId, @RequestParam String userId, HttpSession session) {
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

			boolean success = userGroupService.addUserToGroup(groupId, userId, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "사용자가 그룹에 추가되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "사용자 추가에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("사용자 추가 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "사용자 추가 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 사용자를 그룹에서 제거
	@ResponseBody
	@RequestMapping(value = "/removeMember", method = RequestMethod.POST)
	public Map<String, Object> removeUserFromGroup(@RequestParam String groupId, @RequestParam String userId, HttpSession session) {
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

			boolean success = userGroupService.removeUserFromGroup(groupId, userId);
			if (success) {
				result.put("success", true);
				result.put("message", "사용자가 그룹에서 제거되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "사용자 제거에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("사용자 제거 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "사용자 제거 중 오류가 발생했습니다.");
		}

		return result;
	}

	// SQL 템플릿 카테고리 권한 조회
	@ResponseBody
	@RequestMapping("/sqlTemplateCategoryPermissions")
	public Map<String, Object> getSqlTemplateCategoryPermissions(@RequestParam String groupId, HttpSession session) {
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
			result.put("data", userGroupService.getSqlTemplateCategoryPermissions(groupId));

		} catch (Exception e) {
			logger.error("SQL 템플릿 카테고리 권한 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "SQL 템플릿 카테고리 권한 조회 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 연결 정보 권한 조회
	@ResponseBody
	@RequestMapping("/connectionPermissions")
	public Map<String, Object> getConnectionPermissions(@RequestParam String groupId, HttpSession session) {
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
			result.put("data", userGroupService.getConnectionPermissions(groupId));

		} catch (Exception e) {
			logger.error("연결 정보 권한 조회 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "연결 정보 권한 조회 중 오류가 발생했습니다.");
		}

		return result;
	}
	
	// 그룹에 연결정보 권한 부여 (단순화)
	@ResponseBody
	@RequestMapping(value = "/grantConnectionPermissions", method = RequestMethod.POST)
	public Map<String, Object> grantConnectionPermissions(@RequestBody Map<String, Object> requestData, HttpSession session) {
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

			String groupId = (String) requestData.get("groupId");
			@SuppressWarnings("unchecked")
			java.util.List<String> connectionIds = (java.util.List<String>) requestData.get("connectionIds");

			if (groupId == null || connectionIds == null) {
				result.put("success", false);
				result.put("message", "필수 파라미터가 누락되었습니다.");
				return result;
			}

			// 기존 권한 모두 해제
			permissionService.revokeAllConnectionPermissions(groupId);
			
			// 새로운 권한 부여
			boolean success = true;
			for (String connectionId : connectionIds) {
				if (!permissionService.grantConnectionPermission(groupId, connectionId, userId)) {
					success = false;
					break;
				}
			}
			
			if (success) {
				result.put("success", true);
				result.put("message", "연결정보 권한이 성공적으로 저장되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "연결정보 권한 저장에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("연결정보 권한 저장 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "연결정보 권한 저장 중 오류가 발생했습니다.");
		}

		return result;
	}

	// 그룹 권한 저장
	@ResponseBody
	@RequestMapping(value = "/savePermissions", method = RequestMethod.POST)
	public Map<String, Object> saveGroupPermissions(@RequestBody Map<String, Object> requestData, HttpSession session) {
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

			String groupId = (String) requestData.get("groupId");
			Map<String, Object> permissions = (Map<String, Object>) requestData.get("permissions");

			boolean success = userGroupService.saveGroupPermissions(groupId, permissions, currentUserId);
			if (success) {
				result.put("success", true);
				result.put("message", "권한이 저장되었습니다.");
			} else {
				result.put("success", false);
				result.put("message", "권한 저장에 실패했습니다.");
			}

		} catch (Exception e) {
			logger.error("권한 저장 중 오류 발생", e);
			result.put("success", false);
			result.put("message", "권한 저장 중 오류가 발생했습니다.");
		}

		return result;
	}
}
