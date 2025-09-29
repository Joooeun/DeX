package kr.Windmill.controller;

import kr.Windmill.service.SystemConfigService;
import kr.Windmill.service.DashboardSchedulerService;
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
import java.util.HashMap;
import java.util.Map;

@Controller
public class SystemConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private DashboardSchedulerService dashboardSchedulerService;
    
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
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            // 각 설정값 업데이트
            boolean sessionTimeoutUpdated = systemConfigService.updateConfigValue("SESSION_TIMEOUT", sessionTimeout);
            boolean downloadIpUpdated = systemConfigService.updateConfigValue("DOWNLOAD_IP_PATTERN", downloadIpPattern);
            boolean noticeContentUpdated = systemConfigService.updateConfigValue("NOTICE_CONTENT", noticeContent);
            boolean noticeEnabledUpdated = systemConfigService.updateConfigValue("NOTICE_ENABLED", noticeEnabled);
            
            if (sessionTimeoutUpdated && downloadIpUpdated && noticeContentUpdated && noticeEnabledUpdated) {
                result.put("success", true);
                result.put("message", "환경설정이 성공적으로 저장되었습니다.");
                logger.info("환경설정 저장 완료 - 사용자: {}", memberId);
            } else {
                result.put("success", false);
                result.put("message", "일부 설정값 저장에 실패했습니다.");
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
     * 차트 설정 저장 API
     */
    @RequestMapping(value = "/SystemConfig/saveChartConfig", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveChartConfig(@RequestParam String chartConfig, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            // 차트 설정 저장
            systemConfigService.saveDashboardChartConfig(chartConfig);
            
            // 스케줄러 갱신
            dashboardSchedulerService.refreshSchedulers();
            
            result.put("success", true);
            result.put("message", "차트 설정이 저장되었습니다.");
            logger.info("차트 설정 저장 완료 - 사용자: {}", memberId);
        } catch (Exception e) {
            logger.error("차트 설정 저장 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "차트 설정 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 차트 설정 조회 API
     */
    @RequestMapping(value = "/SystemConfig/getChartConfig", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getChartConfig(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            String chartConfig = systemConfigService.getDashboardChartConfig();
            result.put("success", true);
            result.put("chartConfig", chartConfig);
        } catch (Exception e) {
            logger.error("차트 설정 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "차트 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 차트 에러 상태 리셋 API
     */
    @RequestMapping(value = "/SystemConfig/resetChartErrors", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> resetChartErrors(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        String memberId = (String) session.getAttribute("memberId");
        if (memberId == null || !permissionService.isAdmin(memberId)) {
            result.put("success", false);
            result.put("message", "권한이 없습니다.");
            return result;
        }
        
        try {
            dashboardSchedulerService.resetAllErrorStatus();
            
            result.put("success", true);
            result.put("message", "모든 차트의 에러 상태가 리셋되었습니다.");
            logger.info("차트 에러 상태 리셋 완료 - 사용자: {}", memberId);
        } catch (Exception e) {
            logger.error("차트 에러 상태 리셋 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "에러 상태 리셋 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
}
