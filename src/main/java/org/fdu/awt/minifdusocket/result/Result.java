package org.fdu.awt.minifdusocket.result;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Result {
    private int code;
    /**
     * 表示Result是否按照协议进行
     */
    private boolean uxApi;
    /**
     * 请求是否成功
     */
    private boolean success;
    private String msg;
    private Object object;

    public Result(int code, boolean success, String msg, Object object) {
        this.code = code;
        this.uxApi = true;
        this.success = success;
        this.msg = msg;
        this.object = object;
    }
}

