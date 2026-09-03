package com.github.wz.webshell.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.wz.webshell.utils.WebShellUtils;
import com.github.wz.webshell.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResult<JSONObject> userInfo(String token) {
        ApiResult<JSONObject> res = ApiResult.builder();
        JSONObject json = new JSONObject();
        json.put("roles", "admin");
        json.put("name", "zmzhou");
        return res.data(json);
    }
}
