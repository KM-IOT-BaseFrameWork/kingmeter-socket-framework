package com.kingmeter.socket.framework.role.client;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class BusinessWorkerTemplate {

    public void login(ChannelHandlerContext ctx, Long siteId, String password){}

    public void doJob(byte[] tokenArray, ChannelHandlerContext ctx, long siteId,long internal) {
        try {
            SocketChannel channel = (SocketChannel) ctx.channel();

            AttributeKey<EventExecutorGroup> executorGroupAttributeKey =
                    AttributeKey.valueOf("businessGroup");
            Attribute<EventExecutorGroup> groupAttr = channel.attr(executorGroupAttributeKey);
            EventExecutorGroup businessGroup = groupAttr.get();
            businessGroup.scheduleAtFixedRate(new CommonJob(tokenArray, channel, siteId),
                    1, internal, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("job executed failed"+e.getMessage());
        }
    }


    class CommonJob implements Runnable {
        private byte[] tokenArray;
        private SocketChannel channel;
        private long siteId;

        CommonJob(byte[] tokenArray, SocketChannel channel, long siteId) {
            this.tokenArray = tokenArray;
            this.channel = channel;
            this.siteId = siteId;
        }

        @Override
        public void run() {
            if (channel.isOpen() && channel.isWritable()) {
                sendHeartBeat(tokenArray, channel, siteId);
            }
        }
    }

    public void sendHeartBeat(byte[] tokenArray, SocketChannel channel, long siteId){

    }

}
