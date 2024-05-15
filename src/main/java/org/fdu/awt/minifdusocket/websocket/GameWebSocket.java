package org.fdu.awt.minifdusocket.websocket;


import com.alibaba.fastjson2.JSONArray;
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
    /**
     * key: userId, value: session
     */
    private static final ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    /**
     * key: userId, value: userData
     */
    private static final ConcurrentHashMap<Long, UserData> userDataMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            sessionPool.put(userId, session);
            userDataMap.put(userId, new UserData(userId));
            log.info("【GameWebSocket】有新的连接，总数为:{}", webSockets.size());
        } catch (Exception e) {
            log.error("【GameWebSocket】连接时出错", e);
        }
    }

    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            userDataMap.remove(this.userId);
            log.info("【GameWebSocket】连接断开，总数为:{}", webSockets.size());
            broadcastDeletePlayer(userId);
        } catch (Exception e) {
            log.error("【GameWebSocket】关闭时出错", e);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            log.info("【GameWebSocket】收到客户端消息:{}", message);
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
                    log.error("【GameWebSocket】未知消息类型:{}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("【GameWebSocket】消息格式错误:{}", message, e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误,原因:{}", error.getMessage());
    }


    private void handleInitMessage(JSONObject jsonObject) {
        Long userId = jsonObject.getLong("userId");
        UserData userData = userDataMap.get(userId);
        if (userData != null) {
            userData.updateUserData(jsonObject);
            userData.setAction("Idle");
        }
    }

    private void handleUpdateMessage(JSONObject jsonObject) {
        Long userId = jsonObject.getLong("userId");
        UserData userData = userDataMap.get(userId);
        if (userData != null) {
            userData.updateUserData(jsonObject);
            userData.setAction(jsonObject.getString("action"));
        }
    }

    private void broadcastDeletePlayer(Long userId) {
        JSONObject json = new JSONObject();
        json.put("type", "deletePlayer");
        json.put("id", userId);
        sendMessageToAll(json.toJSONString());
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

        /**
         * 使用 jsonObject 更新 userData
         */
        public void updateUserData(JSONObject jsonObject) {
            setModel(jsonObject.getString("model"));
            setColour(jsonObject.getString("colour"));
            setX(jsonObject.getDouble("x"));
            setY(jsonObject.getDouble("y"));
            setZ(jsonObject.getDouble("z"));
            setHeading(jsonObject.getDouble("heading"));
            setPb(jsonObject.getDouble("pb"));
        }

        public JSONObject toJsonObject() {
            UserData userData = this;
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
            return userJson;
        }
    }

    // 定时任务，每40ms广播一次
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
        JSONArray jsonArray = new JSONArray();
        for (UserData userData : userDataMap.values()) {
            if (userData.getModel() != null) {
                jsonArray.add(userData.toJsonObject());
            }
        }
        json.put("data", jsonArray);
        sendMessageToAll(json.toJSONString());
    }
}
