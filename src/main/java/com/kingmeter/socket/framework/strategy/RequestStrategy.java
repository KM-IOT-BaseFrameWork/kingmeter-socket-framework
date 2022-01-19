package com.kingmeter.socket.framework.strategy;


import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import io.netty.channel.ChannelHandlerContext;

/**
 * template strategy
 */
public interface RequestStrategy {

    /**
     * deal with data from hardware
     * @param requestBody
     * @return
     */
    void process(RequestBody requestBody, ResponseBody responseBody, ChannelHandlerContext ctx);

}
