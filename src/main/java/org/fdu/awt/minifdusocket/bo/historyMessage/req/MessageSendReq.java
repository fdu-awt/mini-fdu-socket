package org.fdu.awt.minifdusocket.bo.historyMessage.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class MessageSendReq {

    @NotNull(message = "本地用户id必填")
    private Long localId;

    @NotNull(message = "远端用户id必填")
    private Long remoteId;

    @NotNull(message = "聊天信息内容必填")
    private String content;


    @NotNull(message = "type必须指定")
    private String type;



    public MessageSendReq(Long userId, Long remoteId, String textMessage, String type) {
        this.localId = userId;
        this.remoteId = remoteId;
        this.content = textMessage;
        this.type = type;
    }
}
