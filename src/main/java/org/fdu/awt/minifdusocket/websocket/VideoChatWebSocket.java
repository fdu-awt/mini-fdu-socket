package org.fdu.awt.minifdusocket.websocket;

import com.alibaba.fastjson2.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.service.IHistoryMessageService;
import org.fdu.awt.minifdusocket.service.impl.HistoryMessageService;
import org.fdu.awt.minifdusocket.utils.SpringContext;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Video Chat WebSocket Service <br/>
 * 用于实现视频聊天功能的 WebSocket 服务
 */
@Component
@Slf4j
@ServerEndpoint(value = "/video-chat/{userId}")
public class VideoChatWebSocket {
    private final IHistoryMessageService historyMessageService;

    private Session session;
    private Long userId;

    private boolean isBusy = false;
    private Timestamp startTime;
    private static final CopyOnWriteArraySet<VideoChatWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();

    /**
     * 无参构造函数，必须有
     * Jakarta WebSocket 规范要求WebSocket端点实例的创建过程能够处理无参构造函数 <br/>
     */
    public VideoChatWebSocket() {
        // 手动获取HistoryMessageService实例
        this.historyMessageService = SpringContext.getBean(HistoryMessageService.class);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSockets.add(this);
        sessionPool.put(userId, session);
        log.info("【VideoChatWebSocket】用户 {} 连接，总连接数: {}", userId, webSockets.size());
    }

    @OnClose
    public void onClose() {
        webSockets.remove(this);
        sessionPool.remove(this.userId);
        log.info("【VideoChatWebSocket】用户 {} 断开连接，总连接数: {}", userId, webSockets.size());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【VideoChatWebSocket】收到用户 {} 的message: {}", userId, message);
        JSONObject jsonObject = JSONObject.parseObject(message);
        String type = jsonObject.getString("type");
        switch (type) {
            case "video-invite":
                handleVideoInvite(jsonObject);
                break;
            case "video-accept":
                handleVideoAccept(jsonObject);
                break;
            case "video-reject": // TODO 暂时无, 未来可能会用到
                handleVideoReject(jsonObject);
                break;
            case "video-processing":
                handleVideoProcessing(jsonObject);
                break;
            case "video-end":
                handleVideoEnd(jsonObject);
                break;
            default:
                log.error("【websocket消息】未知消息类型:{}", type);
                break;
        }
    }

    private void handleVideoInvite(JSONObject data) {
        Long toId = data.getLong("toId");
        if (isBusy) {
            this.reject(userId, toId);
            return;
        }
        this.isBusy = true;
        this.startTime = new Timestamp(System.currentTimeMillis());
        // 将消息转发给对方
        JSONObject forwardData = new JSONObject();
        forwardData.put("type", "video-invite");
        forwardData.put("fromId", userId);
        sendOneMessage(toId, forwardData.toJSONString());
    }

    private void handleVideoAccept(JSONObject data) {
        Long toId = data.getLong("toId");
        this.isBusy = true;
        this.startTime = new Timestamp(System.currentTimeMillis());
        this.accept(userId, toId);
    }

    private void handleVideoReject(JSONObject data) {
        Long toId = data.getLong("toId");
        this.isBusy = false;
        this.startTime = null;
        this.reject(userId, toId);
    }

    private void accept(Long localId, Long toId) {
        JSONObject forwardData = new JSONObject();
        forwardData.put("type", "video-accept");
        forwardData.put("fromId", localId);
        sendOneMessage(toId, forwardData.toJSONString());
    }

    private void reject(Long localId, Long toId) {
        // TODO 存入数据库：已拒绝
        JSONObject forwardData = new JSONObject();
        forwardData.put("type", "video-reject");
        forwardData.put("fromId", localId);
        sendOneMessage(toId, forwardData.toJSONString());
    }

    private void handleVideoProcessing(JSONObject data) {
        Long toId = data.getLong("toId");
        String forwardData = data.getString("forwardData");
        // 将消息转发给对方
        sendOneMessage(toId, forwardData);
    }

    private void handleVideoEnd(JSONObject data) {
        Long toId = data.getLong("toId");
        historyMessageService.videoChatEnd(userId, toId, startTime, new Timestamp(System.currentTimeMillis()));
        this.isBusy = false;
        this.startTime = null;
        JSONObject forwardData = new JSONObject();
        forwardData.put("type", "video-end");
        forwardData.put("fromId", userId);
        sendOneMessage(toId, forwardData.toJSONString());
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户 {} 错误, 原因: {}", userId, error.getMessage());
    }

    /**
     * 单点消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    private void sendOneMessage(Long userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("【VideoChatWebSocket】发送消息时出错: {}", message, e);
            }
        }
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
