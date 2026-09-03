package com.github.wz.webshell.handler;

import com.github.zmzhou.utils.ServerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationStartup implements CommandLineRunner {
        @Value("${server.port}")
    private int port;

        @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public void run(String... args) {
        log.info("项目启动成功！访问地址：{}", "http://" + ServerUtils.getHostIp() + ":" + port + contextPath);
    }
}
