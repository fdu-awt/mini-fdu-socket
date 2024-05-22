package org.fdu.awt.minifdusocket.service;

import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;

import java.sql.Timestamp;
import java.util.List;

public interface IHistoryMessageService {

    void save(MessageSendReq messageSendReq);

    /**
     * @param durationInSec 通话时长(秒)
     */
    void videoChatEnd(Long localId, Long remoteId, Long durationInSec, Timestamp startTime);

    //获取历史聊天信息
    List<MessageShowResp> getHistoryMessages(Long localId, Long remoteId);
}