package com.github.wz.web.shell.controller;

import com.github.wz.web.shell.utils.WebShellUtils;
import com.github.wz.web.shell.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/")
@RestController
public class ElectronController {
    
    @PostMapping("/user/login")
    public ApiResult<Object> login(String username, String password) {
        String token = WebShellUtils.getSessionId();
        return ApiResult.builder().data(token);
    }

    @GetMapping("/user/info")
    public ApiResult<Map<String, Object>> userInfo(String token) {
        ApiResult<Map<String, Object>> res = ApiResult.builder();
        Map<String, Object> json = new HashMap<>();
        json.put("roles", "admin");
        json.put("name", "zmzhou");
        return res.data(json);
    }
}
