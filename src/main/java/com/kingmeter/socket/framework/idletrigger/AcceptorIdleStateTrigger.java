package com.kingmeter.socket.framework.idletrigger;

import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;


@ChannelHandler.Sharable
@Slf4j
public class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {

    private WorkerTemplate worker;

    public AcceptorIdleStateTrigger(WorkerTemplate worker) {
        this.worker = worker;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                SocketChannel channel = (SocketChannel) ctx.channel();
                Long deviceId = channel.attr(AttributeKey.<Long>valueOf("DeviceId")).get();
//                String deviceId = CacheUtil.getInstance().getChannelIdAndDeviceIdMap().getOrDefault(channel.id().asLongText(), "0");
                log.warn(new KingMeterMarker("Socket,ChannelIdle,1001"),
                        "{}|{}|{}", deviceId, channel.id().asLongText(),channel.remoteAddress());
                dealWithIDLE(channel, String.valueOf(deviceId));
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void dealWithIDLE(SocketChannel channel, String deviceId) {
        if (deviceId != null && !deviceId.equals("null") && Long.parseLong(deviceId) > 0) {
            if (channel != null) {
                worker.dealWithOffline(channel, deviceId);
            }
        }
        if (channel != null) {
            channel.close();
        }
    }


}