package org.fdu.awt.minifdusocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.fdu.awt.minifdusocket.bo.historyMessage.req.MessageSaveReq;
import org.fdu.awt.minifdusocket.bo.historyMessage.resp.MessageShowResp;
import org.fdu.awt.minifdusocket.result.Result;
import org.fdu.awt.minifdusocket.result.ResultFactory;
import org.fdu.awt.minifdusocket.service.impl.HistoryMessageService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/message-service")
public class HistoryMessageController {
    private final HistoryMessageService historyMessageService;

    public HistoryMessageController(HistoryMessageService historyMessageService) {
        this.historyMessageService = historyMessageService;
    }

    @PostMapping("save-history-message")
    public Result saveHistoryMessage(@Validated @RequestBody MessageSaveReq messageSaveReq) {
        try{
            historyMessageService.save(messageSaveReq);
            return ResultFactory.buildSuccessResult();
        }
        catch (DataIntegrityViolationException e){
            return ResultFactory.buildFailResult("传入的id有误");
        }
        catch (RuntimeException e) {
            return ResultFactory.buildInternalServerErrorResult();
        }

    }

    @GetMapping("get-history-message")
    public Result getHistoryMessage(@RequestParam("localId") Long localId, @RequestParam("remoteId") Long remoteId) {
        List<MessageShowResp> historyMessageRespList = historyMessageService.getHistoryMessages(localId,remoteId);
        if(historyMessageRespList.isEmpty()) {
            return ResultFactory.buildFailResult("您当前无历史信息");
        }
        else{
            return ResultFactory.buildSuccessResult(historyMessageRespList);
        }
    }
}

