package com.kingmeter.socket.framework.idletrigger;

import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {


    public AcceptorIdleStateTrigger(){
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                SocketChannel channel = (SocketChannel) ctx.channel();
                String deviceId = CacheUtil.getInstance().getChannelIdAndDeviceIdMap().getOrDefault(channel.id().asLongText(),"0");
//                    log.info("{}|ChannelIdle|1001|{}", deviceId, channel.id().asLongText());
                log.info(new KingMeterMarker("Socket,ChannelIdle,1001"),
                        "{}|{}", deviceId, channel.id().asLongText());
                dealWithIDLE(channel, deviceId);
                return;
            }
        }
    }

    private void dealWithIDLE(SocketChannel channel, String deviceId) {
        //if cannot send command ,it means this device if offline
        channel.close();
    }


}