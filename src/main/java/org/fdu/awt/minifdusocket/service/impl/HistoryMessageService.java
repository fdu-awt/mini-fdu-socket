package org.fdu.awt.minifdusocket.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;
import org.fdu.awt.minifdusocket.dao.HistoryMessageDAO;
import org.fdu.awt.minifdusocket.entity.HistoryMessage;
import org.fdu.awt.minifdusocket.service.IHistoryMessageService;
import org.fdu.awt.minifdusocket.utils.TimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class HistoryMessageService implements IHistoryMessageService {
    private static final String VIDEO_CHAT_END_MESSAGE = "视频通话时长: ";
    private final HistoryMessageDAO historyMessageDAO;

    @Autowired
    public HistoryMessageService(HistoryMessageDAO historyMessageDAO) {
        this.historyMessageDAO = historyMessageDAO;
    }

    @Override
    public void save(MessageSendReq messageSendReq) {
        historyMessageDAO.save(HistoryMessage.fromMessageSendReq(messageSendReq));
    }

    @Override
    public void videoChatEnd(Long localId, Long remoteId,
                             Timestamp startTime,
                             Timestamp endTime) {
        Long durationInSec = endTime.getTime() - startTime.getTime();
        historyMessageDAO.save(HistoryMessage.builder()
                .localId(localId)
                .remoteId(remoteId)
                .content(VIDEO_CHAT_END_MESSAGE + TimeFormatter.formatDuration(durationInSec))
                .timeStamp(endTime)
                .type("video")
                .build());
    }

    @Override
    public void videoChatReject(Long initiatorId, Long recipientId, Timestamp startTime) {
        historyMessageDAO.save(HistoryMessage.builder()
                .localId(initiatorId)
                .remoteId(recipientId)
                .content("已拒绝")
                .timeStamp(startTime)
                .type("video")
                .build());
    }

    @Override
    public List<MessageShowResp> getHistoryMessages(Long localId, Long remoteId) {
        List<HistoryMessage> selfHistoryMessages = historyMessageDAO.findByLocalIdAndRemoteId(localId, remoteId);
        List<HistoryMessage> remoteHistoryMessages = historyMessageDAO.findByLocalIdAndRemoteId(remoteId, localId);

        List<HistoryMessage> allMessages = Stream.of(selfHistoryMessages, remoteHistoryMessages)
                .flatMap(List::stream)
                .toList();

        return allMessages.stream()
                .sorted(Comparator.comparing(HistoryMessage::getTimeStamp))
                .map(message -> {
                    MessageShowResp resp = new MessageShowResp();
                    resp.setMessage(message.getContent());
                    resp.setTimestamp(message.getTimeStamp());
                    resp.setIfSelf(localId.equals(message.getLocalId()));
                    resp.setType(message.getType());
                    return resp;
                })
                .collect(Collectors.toList());
    }

}
