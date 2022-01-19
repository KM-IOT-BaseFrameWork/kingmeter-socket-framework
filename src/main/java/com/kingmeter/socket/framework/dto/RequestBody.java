package com.kingmeter.socket.framework.dto;

import lombok.Data;

@Data
public class RequestBody {
    private String deviceId;//hardware id
    private int functionCode;
    private String token;
    private byte[] tokenArray;
    private int resentFlag;
    private String data;


    public RequestBody(String deviceId,int functionCode,
                       String token,byte[] tokenArray,
                       int resentFlag,String data){
        this.deviceId = deviceId;
        this.functionCode = functionCode;
        this.token = token;
        this.tokenArray = tokenArray;
        this.resentFlag = resentFlag;
        this.data = data;
    }
}
