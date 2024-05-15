package org.fdu.awt.minifdusocket.websocket;

import com.alibaba.fastjson2.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;
import org.fdu.awt.minifdusocket.service.impl.HistoryMessageService;
import org.fdu.awt.minifdusocket.utils.SpringContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
@ServerEndpoint(value = "/chat/{userId}")
public class ChatWebSocket {
    private final HistoryMessageService historyMessageService;

    /**
     * 无参构造函数，必须有
     * Jakarta WebSocket 规范要求WebSocket端点实例的创建过程能够处理无参构造函数 <br/>
     */
    public ChatWebSocket() {
        // 手动获取HistoryMessageService实例
        this.historyMessageService = SpringContext.getBean(HistoryMessageService.class);
    }

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    /**
     * 用户ID
     */
    private Long userId;
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    //虽然@Component默认是单例模式的，但springboot还是会为每个websocket连接初始化一个bean，所以可以用一个静态set保存起来。
    //  注：底下WebSocket是当前类名
    private static final CopyOnWriteArraySet<ChatWebSocket> webSockets = new CopyOnWriteArraySet<>();
    // 用来存在线连接用户信息
    private static final ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();

    /**
     * 链接成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            sessionPool.put(userId, session);
            log.info("【websocket消息】有新的连接，总数为:{}", webSockets.size());
        } catch (Exception e) {
            log.error("【websocket消息】连接时出错", e);
        }
    }

    /**
     * 链接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            log.info("【websocket消息】连接断开，总数为:{}", webSockets.size());
        } catch (Exception e) {
            log.error("【websocket消息】连接断开时出错", e);
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 消息
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            log.info("【websocket消息】收到客户端消息:{}", message);
            //这里继续加type（从而来判断收到的前端具体的socket信息）
            // 假设客户端发送的是一个 JSON 字符串，包含 remoteId 和 message
            JSONObject jsonObject = JSONObject.parseObject(message);
            String type = jsonObject.getString("type");
            if (type.equals("chat")) {
                Long remoteId = jsonObject.getLong("remoteId");
                String textMessage = jsonObject.getString("message");
                log.info("【websocket消息】收到客户端消息:{}", textMessage);
                historyMessageService.save(new MessageSendReq(userId, remoteId, textMessage));
                sendOneMessage(remoteId, textMessage);
            } else {
                log.error("【websocket消息】未知消息类型:{}", type);
            }
        } catch (Exception e) {
            log.error("【websocket消息】消息格式错误:{}", message, e);
        }
    }

    /**
     * 发送错误时的处理
     *
     * @param session session
     * @param error   错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误,原因:{}", error.getMessage());
    }


    /**
     * 此为广播消息
     */
    public void sendAllMessage(String message) {
        log.info("【websocket消息】广播消息:{}", message);
        for (ChatWebSocket webSocket : webSockets) {
            try {
                if (webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("【websocket消息】广播消息出错", e);
            }
        }
    }

    /**
     * 此为单点消息
     */
    public static void sendOneMessage(Long userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            try {
                log.info("【websocket消息】 单点消息:{}", message);
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("【websocket消息】 单点消息出错", e);
            }
        }
    }

    /**
     * 此为单点消息(多人)
     */
    public void sendMoreMessage(String[] userIds, String message) {
        for (String userId : userIds) {
            Session session = sessionPool.get(Long.valueOf(userId));
            if (session != null && session.isOpen()) {
                try {
                    log.info("【websocket消息】 多人单点消息:{}", message);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    log.error("【websocket消息】 多人单点消息出错", e);
                }
            }
        }
    }
}
