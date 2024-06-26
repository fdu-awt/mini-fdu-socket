package org.fdu.awt.minifdusocket.service;

import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;

import java.sql.Timestamp;
import java.util.List;

public interface IHistoryMessageService {
    void save(MessageSendReq messageSendReq);

    void videoChatEnd(Long localId, Long remoteId, Timestamp startTime, Timestamp endTime);

    void videoChatReject(Long initiatorId, Long recipientId, Timestamp startTime);

    void videoChatOffLine(Long initiatorId, Long recipientId, Timestamp startTime);

    void videoChatBusy(Long initiatorId, Long recipientId, Timestamp startTime);

    //获取历史聊天信息
    List<MessageShowResp> getHistoryMessages(Long localId, Long remoteId);

    void videoChatCancel(Long initiatorId, Long recipientId, Timestamp cancelTime);
}