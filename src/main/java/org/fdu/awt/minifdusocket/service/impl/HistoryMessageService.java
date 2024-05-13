package org.fdu.awt.minifdusocket.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.entity.HistoryMessage;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;
import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSaveReq;
import org.fdu.awt.minifdusocket.dao.HistoryMessageDAO;
import org.fdu.awt.minifdusocket.service.IHistoryMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class HistoryMessageService  implements IHistoryMessageService {

    private final HistoryMessageDAO historyMessageDAO;

    @Autowired
    public HistoryMessageService(HistoryMessageDAO historyMessageDAO) {this.historyMessageDAO = historyMessageDAO;}

    @Override
    public boolean save(MessageSaveReq messageSaveReq){
        try{
            historyMessageDAO.save(HistoryMessage.fromMessageSaveReq(messageSaveReq));
            return true;
        } catch (Exception e){
            //传入信息有误，外键约束不满足，即loacl_id和remote_id不是数据库中user表有的
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<MessageShowResp> getHistoryMessages(Long localId, Long remoteId) {
        List<HistoryMessage> selfHistoryMessages =  historyMessageDAO.findByLocalIdAndRemoteId(localId, remoteId);
        List<HistoryMessage> remoteHistoryMessages =  historyMessageDAO.findByLocalIdAndRemoteId(remoteId, localId);

        List<HistoryMessage> allMessages = Stream.of(selfHistoryMessages, remoteHistoryMessages)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<MessageShowResp> sortedHistoryMessages = allMessages.stream()
                .sorted((message1, message2) -> message1.getTimeStamp().compareTo(message2.getTimeStamp()))
                .map(message -> {
                    MessageShowResp resp = new MessageShowResp();
                    resp.setMessage(message.getContent());
                    resp.setTimestamp(message.getTimeStamp());
                    resp.setIfSelf(localId.equals(message.getLocalId()));
                    return resp;
                })
                .collect(Collectors.toList());

        return sortedHistoryMessages;
    }


}
