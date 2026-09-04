package com.github.wz.web.shell.handler;

import com.github.wz.web.shell.service.WebShellService;
import com.github.wz.web.shell.utils.WebShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import javax.annotation.Resource;


@Slf4j
@Component
public class WebShellWebSocketHandler implements WebSocketHandler {
    @Resource
    private WebShellService webShellService;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) {
        log.info("==> WebSocket连接建立 uuid={}, sessionId={}", WebShellUtils.getUuid(webSocketSession), webSocketSession.getId());
        webShellService.initConnection(webSocketSession);
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) {
        if (webSocketMessage instanceof TextMessage) {
            log.info("收到文本消息 uuid={}, payload={}", WebShellUtils.getUuid(webSocketSession), ((TextMessage) webSocketMessage).getPayload());
            webShellService.recvHandle(((TextMessage) webSocketMessage).getPayload(), webSocketSession);
        } else if (webSocketMessage instanceof BinaryMessage) {
            log.info("BinaryMessage:{}", webSocketMessage);
        } else if (webSocketMessage instanceof PongMessage) {
            log.info("PongMessage:{}", webSocketMessage);
        } else {
            log.error("Unexpected WebSocket message type: " + webSocketMessage);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) {
        log.error("!!! WebSocket传输错误 uuid={}, sessionId={}, open={}, error={}",
                WebShellUtils.getUuid(webSocketSession),
                webSocketSession.getId(),
                webSocketSession.isOpen(),
                throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) {
        log.info("==> WebSocket连接关闭 uuid={}, sessionId={}, status={}, reason={}",
                WebShellUtils.getUuid(webSocketSession),
                webSocketSession.getId(),
                closeStatus.getCode(),
                closeStatus.getReason());
        webShellService.close(webSocketSession);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}