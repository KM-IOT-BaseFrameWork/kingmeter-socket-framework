package com.kingmeter.socket.framework.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBody {
    private long deviceId;

    private byte START_CODE_1;
    private byte START_CODE_2;
    private byte END_CODE_1;
    private byte END_CODE_2;

    private int token_length;
    private byte[] tokenArray;
    private byte[] functionCodeArray;
    private String data;

}
