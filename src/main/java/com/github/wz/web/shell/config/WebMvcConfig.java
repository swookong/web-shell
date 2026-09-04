package com.github.wz.web.shell.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private CorsConfig corsConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Add more mappings... 可以添加多个mapping
        registry.addMapping("/**")
                // 服务器支持的所有头信息字段
                .allowedHeaders(corsConfig.getAllowedHeaders())
                // 服务器支持的所有跨域请求的方法
                .allowedMethods(corsConfig.getAllowedMethods())
                // 是否允许发送Cookie
                .allowCredentials(corsConfig.isAllowCredentials())
                // 指定本次请求的有效期
                .maxAge(corsConfig.getMaxAge())
                // 设置允许跨域请求的域名
                .allowedOriginPatterns(corsConfig.getAllowedOriginPatterns());
    }
}
