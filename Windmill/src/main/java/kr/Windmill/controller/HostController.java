package kr.Windmill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import kr.Windmill.service.HostService;
import java.util.Map;

/**
 * 호스트 관리 컨트롤러
 */
@Controller
public class HostController {
    
    @Autowired
    private HostService hostService;
    
    /**
     * 호스트 목록 조회
     */
    @ResponseBody
    @RequestMapping(value = "/Host/list", method = RequestMethod.GET)
    public Map<String, Object> getHostList() {
        return hostService.getHostList();
    }
}
