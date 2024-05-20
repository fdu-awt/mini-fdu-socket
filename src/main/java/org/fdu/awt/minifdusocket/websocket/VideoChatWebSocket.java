package org.fdu.awt.minifdusocket.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Video Chat WebSocket Service <br/>
 * 用于实现视频聊天功能的 WebSocket 服务
 */
@Component
@Slf4j
@ServerEndpoint(value = "/video-chat/{userId}")
public class VideoChatWebSocket {
    private Session session;
    private Long userId;
    private static final Set<VideoChatWebSocket> webSockets = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSockets.add(this);
        log.info("【VideoChatWebSocket】用户 {} 连接，总连接数: {}", userId, webSockets.size());
    }

    @OnClose
    public void onClose() {
        webSockets.remove(this);
        log.info("【VideoChatWebSocket】用户 {} 断开连接，总连接数: {}", userId, webSockets.size());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【VideoChatWebSocket】收到用户 {} 的message: {}", userId, message);
        broadcastMessageToOthers(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户 {} 错误, 原因: {}", userId, error.getMessage());
    }

    private void broadcastMessageToOthers(String message) {
        for (VideoChatWebSocket endpoint : webSockets) {
            try {
                if (endpoint.session.isOpen() && !endpoint.userId.equals(userId)) {
                    endpoint.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("【VideoChatWebSocket】广播消息时出错: {}", message, e);
            }
        }
    }
}
