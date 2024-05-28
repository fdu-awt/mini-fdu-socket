package org.fdu.awt.minifdusocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;
import org.fdu.awt.minifdusocket.result.Result;
import org.fdu.awt.minifdusocket.result.ResultFactory;
import org.fdu.awt.minifdusocket.service.impl.HistoryMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/message-service")
public class HistoryMessageController {

    private final HistoryMessageService historyMessageService;

    @Autowired
    public HistoryMessageController(HistoryMessageService historyMessageService) {
        this.historyMessageService = historyMessageService;
    }


    @GetMapping("get-history-message")
    public Result getHistoryMessage(@RequestParam("localId") Long localId, @RequestParam("remoteId") Long remoteId) {
        List<MessageShowResp> historyMessageRespList = historyMessageService.getHistoryMessages(localId, remoteId);
        return ResultFactory.buildSuccessResult(historyMessageRespList);
    }
}

