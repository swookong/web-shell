package com.github.wz.webshell.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {
        private String[] allowedHeaders = {"*"};
        private String[] allowedMethods = {"POST", "GET", "PUT", "DELETE", "OPTIONS", "HEAD"};
        private String[] allowedOriginPatterns = {"*"};
        private boolean allowCredentials = true;
        private long maxAge = 1800L;
}
