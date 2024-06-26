package org.fdu.awt.minifdusocket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSendReq;

import java.sql.Timestamp;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "history_message")
public class HistoryMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "local_id")
    private Long localId;

    @Column(nullable = false, name = "remote_id")
    private Long remoteId;

    @Column(nullable = false, name = "content")
    private String content;

    @Column(nullable = false, name = "time_stamp")
    private Timestamp timeStamp;

    @Column(nullable = false, name = "type")
    private String type;

    public static HistoryMessage fromMessageSendReq(MessageSendReq messageSendReq) {
        return HistoryMessage.builder()
                .localId(messageSendReq.getLocalId())
                .remoteId(messageSendReq.getRemoteId())
                .content(messageSendReq.getContent())
                .timeStamp(new Timestamp(System.currentTimeMillis()))
                .type(messageSendReq.getType())
                .build();
    }

}

