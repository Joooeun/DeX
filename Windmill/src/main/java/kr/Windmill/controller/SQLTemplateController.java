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
import kr.Windmill.service.SqlContentService;
import kr.Windmill.service.PermissionService;
import kr.Windmill.service.SQLExecuteService;
import kr.Windmill.util.Common;
import kr.Windmill.dto.SqlTemplateExecuteDto;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.jdbc.core.JdbcTemplate;

@Controller
public class SQLTemplateController {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLTemplateController.class);
    
    @Autowired
    private SqlTemplateService sqlTemplateService;
    
    @Autowired
    private SqlContentService sqlContentService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SQLExecuteService sqlExecuteService;
    
    @Autowired
    private Common com;
    
    @RequestMapping(path = "/SQLTemplate", method = RequestMethod.GET)
    public ModelAndView sqlTemplateMain(HttpServletRequest request, ModelAndView mv, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        String templateId = request.getParameter("templateId");
        
        logger.info("SQLTemplate 접근 - userId: {}, templateId: {}", userId, templateId);
        
        // templateId가 있으면 특정 템플릿 실행 페이지로 이동
        if (templateId != null && !templateId.trim().isEmpty()) {
            logger.info("템플릿 실행 페이지로 이동: {}", templateId);
            return executeSqlTemplate(request, mv, session, templateId);
        }
        
        // 관리자 권한 확인
        if (!permissionService.isAdmin(userId)) {
            // 관리자가 아니면 접근 차단
            mv.setViewName("redirect:/index");
            return mv;
        }
        
        mv.setViewName("SQLTemplate");
        return mv;
    }
    
    /**
     * 특정 SQL 템플릿 실행 페이지
     */
    private ModelAndView executeSqlTemplate(HttpServletRequest request, ModelAndView mv, HttpSession session, String templateId) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            // 템플릿 정보 조회
            Map<String, Object> templateResult = sqlTemplateService.getSqlTemplateDetail(templateId);
            
            if (templateResult == null || !(Boolean) templateResult.get("success")) {
                mv.addObject("error", "템플릿을 찾을 수 없습니다.");
                mv.setViewName("error");
                return mv;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> templateInfo = (Map<String, Object>) templateResult.get("data");
            
            // 템플릿 정보를 모델에 추가
            mv.addObject("templateId", templateId);
            mv.addObject("templateName", templateInfo.get("sqlName"));
            mv.addObject("templateDescription", templateInfo.get("sqlDesc"));
            mv.addObject("sqlContent", templateInfo.get("sqlContent"));
            mv.addObject("limit", templateInfo.get("executionLimit"));
            mv.addObject("refreshtimeout", templateInfo.get("refreshTimeout"));
            mv.addObject("newline", templateInfo.get("newline"));
            
            // DownloadEnable 설정 (IP 기반)
            String clientIp = request.getRemoteAddr();
            boolean downloadEnable = com.getIp(request).matches(com.DownloadIP);
            mv.addObject("DownloadEnable", downloadEnable);
            
            // 파라미터 정보 조회
            Map<String, Object> paramResult = sqlTemplateService.getTemplateParameters(templateId);
            if (paramResult.get("success").equals(true)) {
                mv.addObject("parameters", paramResult.get("data"));
            }
            
            // 단축키 정보 조회
            Map<String, Object> shortcutResult = sqlTemplateService.getTemplateShortcuts(templateId);
            if (shortcutResult.get("success").equals(true)) {
                mv.addObject("ShortKey", shortcutResult.get("data"));
            }
            
            // 사용자 ID 추가
            mv.addObject("memberId", userId);
            
            // Path 변수 추가 (템플릿 ID 사용)
            mv.addObject("Path", templateId);
            
            // 접근 가능한 DB 연결 정보 조회
            List<Map<String, Object>> connections = sqlTemplateService.getAccessibleConnections(templateId, userId);
            mv.addObject("connections", connections);
            
            mv.setViewName("SQLExecute");
            return mv;
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 실행 페이지 로드 실패", e);
            mv.addObject("error", "템플릿 로드 중 오류가 발생했습니다.");
            mv.setViewName("error");
            return mv;
        }
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
            String sqlContent = request.getParameter("sqlContent");
            String accessibleConnectionIds = request.getParameter("accessibleConnectionIds");
            String categoryIds = request.getParameter("categoryIds"); // 화면에서 categoryIds로 전달되는 카테고리 ID들
            String configContent = request.getParameter("configContent");
            String parameters = request.getParameter("parameters");
            String shortcuts = request.getParameter("shortcuts");
            String auditStr = request.getParameter("audit");
            
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
            
            // newline 파라미터 처리
            Boolean newline = false;
            String newlineStr = request.getParameter("newline");
            if (newlineStr != null && !newlineStr.trim().isEmpty()) {
                newline = Boolean.parseBoolean(newlineStr);
            }
            
            // audit 파라미터 처리
            Boolean audit = false;
            if (auditStr != null && !auditStr.trim().isEmpty()) {
                audit = Boolean.parseBoolean(auditStr);
            }
            
            return sqlTemplateService.saveSqlTemplate(sqlId, sqlName, sqlDesc, sqlVersion, sqlStatus, 
                                                     executionLimit, refreshTimeout, newline, audit, categoryIds, accessibleConnectionIds, sqlContent, configContent, parameters, shortcuts, userId);
            
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
    public Map<String, Object> testSqlTemplate(@ModelAttribute SqlTemplateExecuteDto executeDto, 
                                              HttpServletRequest request, 
                                              HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 세션 정보 설정
            executeDto.setMemberId(userId);
            executeDto.setIp(request.getRemoteAddr());
            
            // 필수 파라미터 검증
            if (executeDto.getTemplateId() == null || executeDto.getTemplateId().trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "템플릿 ID가 필요합니다.");
                return result;
            }
            
            // limit 기본값 설정 (테스트용으로 100으로 설정)
            if (executeDto.getLimit() == null) {
                executeDto.setLimit(100);
            }
            
            // 파라미터 파싱
            if (executeDto.getParameters() != null && !executeDto.getParameters().trim().isEmpty()) {
                try {
                    List<Map<String, Object>> paramList = com.getListFromString(executeDto.getParameters());
                    executeDto.setParameterList(paramList);
                } catch (Exception e) {
                    logger.warn("파라미터 JSON 파싱 실패: {}", e.getMessage());
                    result.put("success", false);
                    result.put("error", "파라미터 형식이 올바르지 않습니다.");
                    return result;
                }
            }
            
            // SQL 실행
            Map<String, List> executionResult = sqlExecuteService.executeTemplateSQL(executeDto);
            
            result.put("success", true);
            result.put("data", executionResult);
            
            return result;
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 테스트 실패: {}", e.getMessage(), e);
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

    // =====================================================
    // SQL 내용 관리 API
    // =====================================================

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/sql-contents")
    public Map<String, Object> getSqlContents(HttpServletRequest request, HttpSession session) {
        String templateId = request.getParameter("templateId");
        
        try {
            List<Map<String, Object>> contents = sqlContentService.getSqlContentsByTemplate(templateId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", contents);
            return result;
        } catch (Exception e) {
            logger.error("SQL 내용 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 내용 조회 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/sql-content/save")
    public Map<String, Object> saveSqlContent(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            String contentId = request.getParameter("contentId");
            String templateId = request.getParameter("templateId");
            String connectionId = request.getParameter("connectionId");
            String sqlContent = request.getParameter("sqlContent");
            
            return sqlContentService.saveSqlContent(contentId, templateId, connectionId, sqlContent, userId);
            
        } catch (Exception e) {
            logger.error("SQL 내용 저장 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 내용 저장 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/sql-content/delete")
    public Map<String, Object> deleteSqlContent(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            String contentId = request.getParameter("contentId");
            
            return sqlContentService.deleteSqlContent(contentId, userId);
            
        } catch (Exception e) {
            logger.error("SQL 내용 삭제 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 내용 삭제 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/sql-content/copy")
    public Map<String, Object> copySqlContent(HttpServletRequest request, HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        
        try {
            String sourceContentId = request.getParameter("sourceContentId");
            String targetConnectionId = request.getParameter("targetConnectionId");
            
            return sqlContentService.copySqlContent(sourceContentId, targetConnectionId, userId);
            
        } catch (Exception e) {
            logger.error("SQL 내용 복사 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "SQL 내용 복사 실패: " + e.getMessage());
            return result;
        }
    }

    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/db-connections")
    public Map<String, Object> getDbConnections(HttpServletRequest request, HttpSession session) {
        try {
            String sql = "SELECT CONNECTION_ID, DB_TYPE, HOST_IP, PORT, DATABASE_NAME, USERNAME, STATUS " +
                        "FROM DATABASE_CONNECTION WHERE STATUS = 'ACTIVE' ORDER BY DB_TYPE, CONNECTION_ID";
            List<Map<String, Object>> connections = jdbcTemplate.queryForList(sql);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", connections);
            return result;
        } catch (Exception e) {
            logger.error("DB 연결 목록 조회 실패", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "DB 연결 목록 조회 실패: " + e.getMessage());
            return result;
        }
    }
    
    @ResponseBody
    @RequestMapping(path = "/SQLTemplate/execute", method = RequestMethod.POST)
    public Map<String, Object> executeTemplate(@ModelAttribute SqlTemplateExecuteDto executeDto, 
                                              HttpServletRequest request, 
                                              HttpSession session) {
        String userId = (String) session.getAttribute("memberId");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 세션 정보 설정
            executeDto.setMemberId(userId);
            executeDto.setIp(request.getRemoteAddr());
            
            // 필수 파라미터 검증
            if (executeDto.getTemplateId() == null || executeDto.getTemplateId().trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "템플릿 ID가 필요합니다.");
                return result;
            }
            
            // limit 기본값 설정
            if (executeDto.getLimit() == null) {
                executeDto.setLimit(1000);
            }
            
            // 파라미터 파싱
            if (executeDto.getParameters() != null && !executeDto.getParameters().trim().isEmpty()) {
                try {
                    List<Map<String, Object>> paramList = com.getListFromString(executeDto.getParameters());
                    executeDto.setParameterList(paramList);
                } catch (Exception e) {
                    logger.warn("파라미터 JSON 파싱 실패: {}", e.getMessage());
                    result.put("success", false);
                    result.put("error", "파라미터 형식이 올바르지 않습니다.");
                    return result;
                }
            }
            
            // SQL 실행
            Map<String, List> executionResult = sqlExecuteService.executeTemplateSQL(executeDto);
            
            result.put("success", true);
            result.put("data", executionResult);
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 실행 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "SQL 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
}
