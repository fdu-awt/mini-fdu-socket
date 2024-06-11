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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private boolean isTheInitiator = false;
    private boolean isBusy = false;
    private Timestamp startTime;
    private static final ConcurrentHashMap<Long, VideoChatWebSocket> socketPool = new ConcurrentHashMap<>();

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
        socketPool.put(userId, this);
        log.info("【VideoChatWebSocket】用户 {} 连接，总连接数: {}", userId, socketPool.size());
    }

    @OnClose
    public void onClose() {
        socketPool.remove(this.userId);
        log.info("【VideoChatWebSocket】用户 {} 断开连接，总连接数: {}", userId, socketPool.size());
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.info("【VideoChatWebSocket】收到用户 {} 的message: {}", userId, message);
            JSONObject data = JSONObject.parseObject(message);
            String type = data.getString("type");
            switch (type) {
                case "video-invite":
                    handleVideoInvite(data);
                    break;
                case "video-accept":
                    handleVideoAccept(data);
                    break;
                case "video-reject":
                    handleVideoReject(data);
                    break;
                case "video-processing":
                    handleVideoProcessing(data);
                    break;
                case "video-end":
                    handleVideoEnd(data);
                    break;
                default:
                    log.error("【websocket消息】未知消息类型:{}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("【VideoChatWebSocket】处理消息时出错: {}", message, e);
        }
    }

    private void handleVideoInvite(JSONObject data) {
        Long toId = data.getLong("toId");
        VideoChatWebSocket toSocket = socketPool.get(toId);
        // 对方不在线
        if (toSocket == null || toSocket.session == null || !toSocket.session.isOpen()) {
            this.sendRejectMessage(userId, toId, "offline");
            historyMessageService.videoChatOffLine(userId, toId, new Timestamp(System.currentTimeMillis()));
            return;
        }
        // 对方正在通话中
        if (toSocket.isBusy) {
            this.sendRejectMessage(userId, toId, "busy");
            historyMessageService.videoChatBusy(userId, toId, new Timestamp(System.currentTimeMillis()));
            return;
        }
        // 对方在线且空闲
        this.sendInviteMessage(toId, userId);
        // 标记自己为发起者
        this.isTheInitiator = true;
        this.isBusy = true;
        this.startTime = new Timestamp(System.currentTimeMillis());
    }

    private void handleVideoAccept(JSONObject data) {
        Long toId = data.getLong("toId");
        // 告知对方接受了邀请
        this.sendAcceptMessage(toId, userId);
        // 标记自己为接受者
        this.isTheInitiator = false;
        this.isBusy = true;
        this.startTime = new Timestamp(System.currentTimeMillis());
    }

    private void handleVideoReject(JSONObject data) {
        Long toId = data.getLong("toId");
        this.sendRejectMessage(toId, userId, "reject");
        // 存入数据库：拒绝由接受者存储
        if (this.isTheInitiator) {
            log.error("【VideoChatWebSocket】逻辑错误，发起者拒绝了自己的邀请");
        } else {
            historyMessageService.videoChatReject(toId, userId, new Timestamp(System.currentTimeMillis()));
        }
        this.isBusy = false;
        this.startTime = null;
    }

    private void handleVideoProcessing(JSONObject data) {
        // 只做转发
        Long toId = data.getLong("toId");

        if (Objects.equals(toId, this.userId)) {
            log.error("【VideoChatWebSocket】逻辑错误，转发消息的目标是自己");
            return;
        }
        this.sendProcessingMessage(toId, userId, data.getJSONObject("forwardData"));
    }

    private void handleVideoEnd(JSONObject data) {
        Long toId = data.getLong("toId");
        this.sendEndMessage(toId, userId);
        VideoChatWebSocket peerSocket = socketPool.get(toId);
        if (peerSocket != null) {
            peerSocket.isTheInitiator = false;
            peerSocket.isBusy = false;
            peerSocket.startTime = null;
        }
        // 视频结束由 发起结束者 存储
        historyMessageService.videoChatEnd(userId, toId, startTime, new Timestamp(System.currentTimeMillis()));
        this.isTheInitiator = false;
        this.isBusy = false;
        this.startTime = null;
    }

    private void sendRejectMessage(Long toId, Long fromId, String reason) {
        JSONObject message = new JSONObject();
        message.put("type", "video-reject");
        message.put("fromId", fromId);
        message.put("toId", toId);
        message.put("reason", reason);
        sendOneMessage(toId, message.toJSONString());
    }

    private void sendInviteMessage(Long toId, Long fromId) {
        JSONObject message = new JSONObject();
        message.put("type", "video-invite");
        message.put("fromId", fromId);
        message.put("toId", toId);
        sendOneMessage(toId, message.toJSONString());
    }

    private void sendAcceptMessage(Long toId, Long fromId) {
        JSONObject message = new JSONObject();
        message.put("type", "video-accept");
        message.put("fromId", fromId);
        message.put("toId", toId);
        sendOneMessage(toId, message.toJSONString());
    }

    private void sendEndMessage(Long toId, Long fromId) {
        JSONObject message = new JSONObject();
        message.put("type", "video-end");
        message.put("fromId", fromId);
        message.put("toId", toId);
        sendOneMessage(toId, message.toJSONString());
    }

    private void sendProcessingMessage(Long toId, Long fromId, JSONObject forwardData) {
        JSONObject message = new JSONObject();
        message.put("type", "video-processing");
        message.put("fromId", fromId);
        message.put("toId", toId);
        message.put("forwardData", forwardData);
        sendOneMessage(toId, message.toJSONString());
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
        Session session = Optional.ofNullable(socketPool.get(userId))
                .map(socket -> socket.session).orElse(null);
        if (session != null && session.isOpen()) {
            try {
                log.info("【VideoChatWebSocket】发送消息给用户 {}: {}", userId, message);
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("【VideoChatWebSocket】发送消息时出错: {}", message, e);
            }
        }
    }
}
