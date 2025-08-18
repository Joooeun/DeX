package kr.Windmill.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.Windmill.service.SqlTemplateService;
import kr.Windmill.service.PermissionService;

@Controller
public class SQLTemplateController {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLTemplateController.class);
    
    @Autowired
    private SqlTemplateService sqlTemplateService;
    
    @Autowired
    private PermissionService permissionService;
    
    @RequestMapping(path = "/SQLTemplate", method = RequestMethod.GET)
    public ModelAndView sqlTemplateMain(HttpServletRequest request, ModelAndView mv, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            // 관리자가 아니면 접근 차단
            mv.setViewName("redirect:/index");
            return mv;
        }
        
        mv.setViewName("SQLTemplate");
        return mv;
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/tree")
    public List<Map<String, Object>> getSqlTemplateTree(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            return null;
        }
        
        try {
            // DB 기반 트리 반환
            return sqlTemplateService.getFullMenuTree();
        } catch (Exception e) {
            logger.error("SQL 템플릿 트리 조회 실패", e);
            return null;
        }
    }
    
    // 사용자가 접근 가능한 카테고리 목록 조회 (관리자 전용)
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/categories")
    public Map<String, Object> getAuthorizedCategories(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            // 관리자는 모든 카테고리 접근 가능
            result.put("success", true);
            result.put("data", permissionService.getAllCategories());
        } catch (Exception e) {
            logger.error("카테고리 목록 조회 실패", e);
            result.put("success", false);
            result.put("message", "카테고리 목록 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/detail")
    public Map<String, Object> getSqlTemplateDetail(HttpServletRequest request, HttpSession session) {
        String sqlId = request.getParameter("sqlId");
        
        try {
            return sqlTemplateService.getSqlTemplateDetail(sqlId);
        } catch (Exception e) {
            logger.error("SQL 템플릿 상세 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 템플릿 조회 실패: " + e.getMessage());
            return result;
        }
    }
    
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/save")
    public Map<String, Object> saveSqlTemplate(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            String sqlId = request.getParameter("sqlId");
            String sqlName = request.getParameter("sqlName");
            String sqlDesc = request.getParameter("sqlDesc");
            String sqlVersionStr = request.getParameter("sqlVersion");
            String sqlStatus = request.getParameter("sqlStatus");
            String executionLimitStr = request.getParameter("executionLimit");
            String refreshTimeoutStr = request.getParameter("refreshTimeout");
            String sqlPath = request.getParameter("sqlPath");
            String sqlContent = request.getParameter("sqlContent");
            String configContent = request.getParameter("configContent");
            String parameters = request.getParameter("parameters");
            String shortcuts = request.getParameter("shortcuts");
            
            // 숫자 파라미터 변환
            Integer sqlVersion = null;
            Integer executionLimit = null;
            Integer refreshTimeout = null;
            
            if (sqlVersionStr != null && !sqlVersionStr.trim().isEmpty()) {
                sqlVersion = Integer.parseInt(sqlVersionStr);
            }
            if (executionLimitStr != null && !executionLimitStr.trim().isEmpty()) {
                executionLimit = Integer.parseInt(executionLimitStr);
            }
            if (refreshTimeoutStr != null && !refreshTimeoutStr.trim().isEmpty()) {
                refreshTimeout = Integer.parseInt(refreshTimeoutStr);
            }
            
            return sqlTemplateService.saveSqlTemplate(sqlId, sqlName, sqlDesc, sqlVersion, sqlStatus, 
                                                     executionLimit, refreshTimeout, sqlPath, sqlContent, configContent, parameters, shortcuts, userId);
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 저장 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 템플릿 저장 실패: " + e.getMessage());
            return result;
        }
    }
    
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/delete")
    public Map<String, Object> deleteSqlTemplate(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            String sqlId = request.getParameter("sqlId");
            
            if (!"admin".equals(userId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "관리자만 삭제할 수 있습니다.");
                return result;
            }
            
            return sqlTemplateService.deleteSqlTemplate(sqlId, userId);
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 삭제 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 템플릿 삭제 실패: " + e.getMessage());
            return result;
        }
    }
    
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/test")
    public Map<String, Object> testSqlTemplate(HttpServletRequest request, HttpSession session) {
        try {
            String sqlContent = request.getParameter("sqlContent");
            
            // 간단한 SQL 문법 검증
            boolean isValid = validateSqlSyntax(sqlContent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", isValid);
            
            if (!isValid) {
                result.put("error", "SQL 문법이 올바르지 않습니다.");
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 테스트 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 테스트 실패: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 간단한 SQL 문법 검증
     */
    private boolean validateSqlSyntax(String sqlContent) {
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            return false;
        }
        
        String sql = sqlContent.trim().toUpperCase();
        
        // 기본적인 SQL 키워드 검증
        boolean hasSelect = sql.contains("SELECT");
        boolean hasFrom = sql.contains("FROM");
        
        // SELECT 문인 경우
        if (hasSelect && hasFrom) {
            return true;
        }
        
        // INSERT, UPDATE, DELETE 문인 경우
        if (sql.contains("INSERT") || sql.contains("UPDATE") || sql.contains("DELETE")) {
            return true;
        }
        
        // CALL 문인 경우
        if (sql.contains("CALL")) {
            return true;
        }
        
        return false;
    }

    // =====================================================
    // 카테고리 관리 엔드포인트
    // =====================================================

    /**
     * 카테고리 목록 조회
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/list")
    public Map<String, Object> getCategoryList(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            List<Map<String, Object>> categories = sqlTemplateService.getCategories();
            result.put("success", true);
            result.put("data", categories);
        } catch (Exception e) {
            logger.error("카테고리 목록 조회 실패", e);
            result.put("success", false);
            result.put("message", "카테고리 목록 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 카테고리 생성
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/create", method = RequestMethod.POST)
    public Map<String, Object> createCategory(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String categoryName = request.getParameter("categoryName");
            String description = request.getParameter("description");
            
            return sqlTemplateService.createCategory(categoryName, description, userId);
        } catch (Exception e) {
            logger.error("카테고리 생성 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "카테고리 생성 실패: " + e.getMessage());
            return result;
        }
    }

    /**
     * 카테고리 수정
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/update", method = RequestMethod.POST)
    public Map<String, Object> updateCategory(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String categoryId = request.getParameter("categoryId");
            String categoryName = request.getParameter("categoryName");
            String description = request.getParameter("description");
            
            return sqlTemplateService.updateCategory(categoryId, categoryName, description, userId);
        } catch (Exception e) {
            logger.error("카테고리 수정 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "카테고리 수정 실패: " + e.getMessage());
            return result;
        }
    }

    /**
     * 카테고리 삭제
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/delete", method = RequestMethod.POST)
    public Map<String, Object> deleteCategory(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String categoryId = request.getParameter("categoryId");
            
            return sqlTemplateService.deleteCategory(categoryId, userId);
        } catch (Exception e) {
            logger.error("카테고리 삭제 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "카테고리 삭제 실패: " + e.getMessage());
            return result;
        }
    }

    /**
     * 카테고리별 템플릿 목록 조회
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/templates")
    public Map<String, Object> getTemplatesByCategory(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String categoryId = request.getParameter("categoryId");
            List<Map<String, Object>> templates = sqlTemplateService.getTemplatesByCategory(categoryId);
            
            result.put("success", true);
            result.put("data", templates);
        } catch (Exception e) {
            logger.error("카테고리별 템플릿 조회 실패", e);
            result.put("success", false);
            result.put("message", "템플릿 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 템플릿을 카테고리에 할당
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/assign", method = RequestMethod.POST)
    public Map<String, Object> assignTemplateToCategory(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String templateId = request.getParameter("templateId");
            String categoryId = request.getParameter("categoryId");
            
            return sqlTemplateService.assignTemplateToCategory(templateId, categoryId, userId);
        } catch (Exception e) {
            logger.error("템플릿 카테고리 할당 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "템플릿 할당 실패: " + e.getMessage());
            return result;
        }
    }

    /**
     * 카테고리 상세 정보 조회
     */
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/category/detail")
    public Map<String, Object> getCategoryDetail(HttpServletRequest request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("memberId");
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return result;
        }
        
        try {
            String categoryId = request.getParameter("categoryId");
            Map<String, Object> category = sqlTemplateService.getCategoryDetail(categoryId);
            
            if (category != null) {
                result.put("success", true);
                result.put("data", category);
            } else {
                result.put("success", false);
                result.put("message", "카테고리를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("카테고리 상세 조회 실패", e);
            result.put("success", false);
            result.put("message", "카테고리 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/parameters")
    public Map<String, Object> getTemplateParameters(HttpServletRequest request, HttpSession session) {
        String templateId = request.getParameter("templateId");
        
        try {
            return sqlTemplateService.getTemplateParameters(templateId);
        } catch (Exception e) {
            logger.error("템플릿 파라미터 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "파라미터 조회 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/shortcuts")
    public Map<String, Object> getTemplateShortcuts(HttpServletRequest request, HttpSession session) {
        String templateId = request.getParameter("templateId");
        
        try {
            return sqlTemplateService.getTemplateShortcuts(templateId);
        } catch (Exception e) {
            logger.error("템플릿 단축키 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "단축키 조회 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/list")
    public Map<String, Object> getTemplateList(HttpServletRequest request, HttpSession session) {
        try {
            return sqlTemplateService.getTemplateList();
        } catch (Exception e) {
            logger.error("템플릿 목록 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "템플릿 목록 조회 실패: " + e.getMessage());
            return result;
        }
    }
}
