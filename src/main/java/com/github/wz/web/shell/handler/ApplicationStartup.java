package com.github.wz.web.shell.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Slf4j
@Component
public class ApplicationStartup implements CommandLineRunner {
    @Value("${server.port}")
    private int port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public void run(String... args) {
        String hostIp = "localhost";
        try {
            hostIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("获取本机IP失败，使用localhost：{}", e.getMessage());
        }
        log.info("项目启动成功！访问地址：{}", "http://" + hostIp + ":" + port + contextPath);
    }
}
