package com.github.wz.web.shell.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wz.web.shell.utils.EhCacheUtils;
import com.github.wz.web.shell.utils.SftpUtils;
import com.github.wz.web.shell.utils.WebShellUtils;
import com.github.wz.web.shell.vo.WebShellData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class RouterController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/sftp")
    public String sftp(String params, Model model) {
        String sessionId = WebShellUtils.getSessionId();
        log.info("[SFTP] sessionId={}, params length={}", sessionId, params != null ? params.length() : 0);

        WebShellData sshData = null;
        try {
            if (params != null && !params.isEmpty()) {
                sshData = objectMapper.readValue(params, WebShellData.class);
                log.info("[SFTP] parsed params: host={}, user={}", sshData.getHost(), sshData.getUsername());
                EhCacheUtils.put(sessionId, sshData);
                log.info("[SFTP] sshData cached with sessionId={}", sessionId);
            } else {
                log.info("[SFTP] params is empty, trying EhCache for sessionId={}", sessionId);
                sshData = EhCacheUtils.get(sessionId);
            }
        } catch (Exception e) {
            log.error("[SFTP] failed to parse params: {}", e.getMessage(), e);
            sshData = EhCacheUtils.get(sessionId);
            if (sshData == null) {
                log.error("[SFTP] also no cache fallback available");
            }
        }

        if (sshData != null) {
            log.info("[SFTP] attempting SSH login to {}:{} as {}", sshData.getHost(), sshData.getPort(), sshData.getUsername());
            SftpUtils sftpUtils = new SftpUtils(sshData);
            boolean login = false;
            try {
                login = sftpUtils.login();
                log.info("[SFTP] SSH login result: {}", login);
            } catch (Exception e) {
                log.error("[SFTP] SSH login failed: {}", e.getMessage(), e);
            } finally {
                try {
                    sftpUtils.logout();
                } catch (Exception ignored) {
                }
            }
            model.addAttribute("login", login);
            model.addAttribute("host", sshData.getHost());
        } else {
            log.warn("[SFTP] no sshData available at all, login will be false");
            model.addAttribute("login", false);
            model.addAttribute("host", "unknown");
        }
        return "sftp";
    }
}
