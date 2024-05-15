package org.fdu.awt.minifdusocket.service;

import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;

import java.util.List;

public interface IHistoryMessageService {

    void save(MessageSendReq messageSendReq);

    //获取历史聊天信息
    List<MessageShowResp> getHistoryMessages(Long localId, Long remoteId);
}