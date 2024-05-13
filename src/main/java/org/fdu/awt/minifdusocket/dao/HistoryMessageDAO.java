package org.fdu.awt.minifdusocket.dao;

import org.fdu.awt.minifdusocket.entity.HistoryMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryMessageDAO extends JpaRepository<HistoryMessage, Long> {

    List<HistoryMessage> findByLocalIdAndRemoteId(Long localId, Long remoteId);

}
