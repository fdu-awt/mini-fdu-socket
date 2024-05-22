package org.fdu.awt.minifdusocket.bo.historyMessage.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageShowResp {
    //返回的历史消息，不用id信息，只需要时间、内容和是否为自己发的
    private String message;
    private Timestamp timestamp ;
    private boolean ifSelf;
    private String type;
}