package kr.Windmill.controller;

import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class SystemConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * 환경설정 관리 페이지
     */
    @RequestMapping(value = "/SystemConfig", method = RequestMethod.GET)
    public ModelAndView systemConfigMain(HttpServletRequest request, ModelAndView mv, HttpSession session) {
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            mv.setViewName("redirect:/Login");
            return mv;
        }
        
        // 현재 설정값들을 가져와서 모델에 추가
        Map<String, String> configValues = systemConfigService.getAllConfigValues();
        mv.addObject("configValues", configValues);
        mv.addObject("memberId", memberId);
        mv.addObject("isAdmin", permissionService.isAdmin(memberId));
        
        // 뷰 이름 설정
        mv.setViewName("SystemConfig");
        
        return mv;
    }
    
    /**
     * 환경설정 저장
     */
    @RequestMapping(value = "/SystemConfig/save", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveSystemConfig(
            @RequestParam("sessionTimeout") String sessionTimeout,
            @RequestParam("downloadIpPattern") String downloadIpPattern,
            @RequestParam("noticeContent") String noticeContent,
            @RequestParam(value = "noticeEnabled", defaultValue = "false") String noticeEnabled,
            @RequestParam(value = "dashboardCharts", defaultValue = "[]") String dashboardChartsJson,
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            // 각 설정값 업데이트 (실패해도 계속 진행)
            boolean sessionTimeoutUpdated = systemConfigService.updateConfigValue("SESSION_TIMEOUT", sessionTimeout);
            boolean downloadIpUpdated = systemConfigService.updateConfigValue("DOWNLOAD_IP_PATTERN", downloadIpPattern);
            boolean noticeContentUpdated = systemConfigService.updateConfigValue("NOTICE_CONTENT", noticeContent);
            boolean noticeEnabledUpdated = systemConfigService.updateConfigValue("NOTICE_ENABLED", noticeEnabled);
            
            // 대시보드 설정 업데이트 (실패해도 계속 진행)
            // 대시보드 설정 저장 (JSON 형태)
            boolean dashboardChartsUpdated = systemConfigService.updateConfigValue("DASHBOARD_CHARTS", dashboardChartsJson);
            
            // 기본 설정이 성공하면 성공으로 처리
            if (sessionTimeoutUpdated && downloadIpUpdated && noticeContentUpdated && noticeEnabledUpdated) {
                result.put("success", true);
                result.put("message", "환경설정이 성공적으로 저장되었습니다.");
                logger.info("환경설정 저장 완료 - 사용자: {}, 대시보드 차트 설정: {}", memberId, dashboardChartsUpdated ? "성공" : "실패");
            } else {
                result.put("success", false);
                result.put("message", "기본 설정값 저장에 실패했습니다.");
            }
            
        } catch (Exception e) {
            logger.error("환경설정 저장 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "환경설정 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 현재 설정값 조회 (AJAX)
     */
    @RequestMapping(value = "/SystemConfig/get", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getSystemConfig(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            Map<String, String> configValues = systemConfigService.getAllConfigValues();
            result.put("success", true);
            result.put("configValues", configValues);
        } catch (Exception e) {
            logger.error("환경설정 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "환경설정 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 공지사항 조회 (비로그인 사용자도 접근 가능)
     */
    @RequestMapping(value = "/SystemConfig/getNotice", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getNotice() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String noticeEnabled = systemConfigService.getConfigValue("NOTICE_ENABLED", "false");
            String noticeContent = systemConfigService.getConfigValue("NOTICE_CONTENT", "");
            
            result.put("success", true);
            result.put("noticeEnabled", noticeEnabled);
            result.put("noticeContent", noticeContent);
        } catch (Exception e) {
            logger.error("공지사항 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "공지사항 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 대시보드 설정 조회 (AJAX)
     */
    @RequestMapping(value = "/SystemConfig/getDashboardConfig", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getDashboardConfig(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            // 대시보드 차트 설정 조회 (JSON 형태)
            String dashboardChartsJson = systemConfigService.getConfigValue("DASHBOARD_CHARTS", "[]");
            
            // JSON 파싱하여 차트 목록 생성
            List<Map<String, String>> dashboardCharts = new ArrayList<>();
            try {
                ObjectMapper mapper = new ObjectMapper();
                dashboardCharts = mapper.readValue(dashboardChartsJson, new TypeReference<List<Map<String, String>>>() {});
            } catch (Exception e) {
                logger.warn("대시보드 차트 설정 JSON 파싱 실패, 빈 배열 사용: " + e.getMessage());
            }
            
            Map<String, Object> dashboardConfig = new HashMap<>();
            dashboardConfig.put("dashboardCharts", dashboardCharts);
            
            result.put("success", true);
            result.put("dashboardConfig", dashboardConfig);
        } catch (Exception e) {
            logger.error("대시보드 설정 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "대시보드 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
}
