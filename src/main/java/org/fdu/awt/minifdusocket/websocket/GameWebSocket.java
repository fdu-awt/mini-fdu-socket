package org.fdu.awt.minifdusocket.websocket;


import com.alibaba.fastjson2.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Game synchronous <br/>
 * 包括：玩家位置信息、玩家模型信息、玩家动作信息的同步 <br/>
 *
 * @author ZMark
 * @date 2024/5/15 上午11:02
 */
@Component
@Slf4j
@ServerEndpoint(value = "/game/{userId}")
public class GameWebSocket {
    private Session session;
    private Long userId;
    private static final Set<GameWebSocket> webSockets = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, UserData> userDataMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSockets.add(this);
        sessionPool.put(userId, session);
        userDataMap.put(userId, new UserData(userId));
        log.info("【LocationWebSocket】有新的连接，总数为:{}", webSockets.size());
        sendId(userId);
    }

    @OnClose
    public void onClose() {
        webSockets.remove(this);
        sessionPool.remove(this.userId);
        userDataMap.remove(this.userId);
        log.info("【LocationWebSocket】连接断开，总数为:{}", webSockets.size());
        broadcastDeletePlayer(userId);
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【LocationWebSocket】收到客户端消息:{}", message);
        try {
            JSONObject jsonObject = JSONObject.parseObject(message);
            String type = jsonObject.getString("type");

            switch (type) {
                case "init":
                    handleInitMessage(jsonObject);
                    break;
                case "update":
                    handleUpdateMessage(jsonObject);
                    break;
                default:
                    log.error("【LocationWebSocket】未知消息类型:{}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("【LocationWebSocket】消息格式错误:{}", message, e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误,原因:{}", error.getMessage());
    }

    private void sendId(Long userId) {
        JSONObject json = new JSONObject();
        json.put("type", "setId");
        json.put("id", userId);
        sendMessageToOne(userId, json.toJSONString());
    }

    private void handleInitMessage(JSONObject jsonObject) {
        Long userId = jsonObject.getLong("userId");
        UserData userData = userDataMap.get(userId);
        if (userData != null) {
            setUserData(jsonObject, userData);
            userData.setAction("Idle");
        }
    }

    private void setUserData(JSONObject jsonObject, UserData userData) {
        userData.setModel(jsonObject.getString("model"));
        userData.setColour(jsonObject.getString("colour"));
        userData.setX(jsonObject.getDouble("x"));
        userData.setY(jsonObject.getDouble("y"));
        userData.setZ(jsonObject.getDouble("z"));
        userData.setHeading(jsonObject.getDouble("heading"));
        userData.setPb(jsonObject.getDouble("pb"));
    }

    private void handleUpdateMessage(JSONObject jsonObject) {
        Long userId = jsonObject.getLong("userId");
        UserData userData = userDataMap.get(userId);
        if (userData != null) {
            setUserData(jsonObject, userData);
            userData.setAction(jsonObject.getString("action"));
        }
    }

    private void handleChatMessage(JSONObject jsonObject) {
        Long remoteId = jsonObject.getLong("remoteId");
        String message = jsonObject.getString("message");
        log.info("chat message: {} {}", remoteId, message);
        sendMessageToOne(remoteId, jsonObject.toJSONString());
    }

    private void broadcastDeletePlayer(Long userId) {
        JSONObject json = new JSONObject();
        json.put("type", "deletePlayer");
        json.put("id", userId);
        sendMessageToAll(json.toJSONString());
    }

    public static void sendMessageToOne(Long userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    public static void sendMessageToAll(String message) {
        for (GameWebSocket webSocket : webSockets) {
            if (webSocket.session.isOpen()) {
                webSocket.session.getAsyncRemote().sendText(message);
            }
        }
    }

    @Data
    private static class UserData {
        private Long userId;
        private String model;
        private String colour;
        private double x;
        private double y;
        private double z;
        private double heading;
        private double pb;
        private String action;

        public UserData(Long userId) {
            this.userId = userId;
        }
    }

    static {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(40);
                    broadcastRemoteData();
                } catch (InterruptedException e) {
                    log.error("定时任务出错", e);
                }
            }
        }).start();
    }

    private static void broadcastRemoteData() {
        JSONObject json = new JSONObject();
        json.put("type", "remoteData");
        for (UserData userData : userDataMap.values()) {
            if (userData.getModel() != null) {
                JSONObject userJson = new JSONObject();
                userJson.put("id", userData.getUserId());
                userJson.put("model", userData.getModel());
                userJson.put("colour", userData.getColour());
                userJson.put("x", userData.getX());
                userJson.put("y", userData.getY());
                userJson.put("z", userData.getZ());
                userJson.put("heading", userData.getHeading());
                userJson.put("pb", userData.getPb());
                userJson.put("action", userData.getAction());
                json.put(userData.getUserId().toString(), userJson);
            }
        }
        sendMessageToAll(json.toJSONString());
    }
}
