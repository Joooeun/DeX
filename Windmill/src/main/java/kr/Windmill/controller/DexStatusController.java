package kr.Windmill.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.Windmill.dto.system.DexStatusDto;
import kr.Windmill.service.DexStatusService;

@Controller
public class DexStatusController {
    
    @Autowired
    private DexStatusService dexStatusService;
    
    /**
     * DEX 상태 조회
     */
    @RequestMapping(path = "/DexStatus/status", method = RequestMethod.POST)
    public @ResponseBody List<DexStatusDto> getDexStatus(HttpServletRequest request, HttpSession session) {
        return dexStatusService.getAllDexStatuses();
    }
    
    /**
     * 특정 DEX 상태 수동 새로고침
     */
    @RequestMapping(path = "/DexStatus/refresh", method = RequestMethod.POST)
    public @ResponseBody String refreshDexStatus(String statusName, HttpServletRequest request, HttpSession session) {
        try {
            dexStatusService.updateDexStatusManually(statusName);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
} 