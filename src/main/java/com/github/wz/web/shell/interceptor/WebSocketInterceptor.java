package com.github.wz.web.shell.interceptor;

import com.github.wz.web.shell.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class WebSocketInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse,
                                   WebSocketHandler webSocketHandler, Map<String, Object> map) {
        if (serverHttpRequest instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest request = (ServletServerHttpRequest) serverHttpRequest;
            String uuid = UUID.randomUUID().toString().replace("-", "");
            map.put(Constants.USER_UUID_KEY, uuid);
            String sessionId = request.getServletRequest().getSession().getId();
            map.put(Constants.HTTP_SESSION_ID_KEY, sessionId);
            log.info("beforeHandshake uuid={}, httpSessionId={}", uuid, sessionId);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse,
                               WebSocketHandler webSocketHandler, Exception e) {
        log.info("afterHandshake");
    }
}