package com.kingmeter.socket.framework.role.server;

import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.dto.RequestBody;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: crazyandy
 */
@Slf4j
@ChannelHandler.Sharable
public class KMServerHandler extends SimpleChannelInboundHandler<RequestBody> {

    private WorkerTemplate worker;

    public KMServerHandler(WorkerTemplate worker) {
        this.worker = worker;
    }

    /**
     * tcp socket opened
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    /**
     * client disconnected by itself ,server can detect here
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        SocketChannel channel = (SocketChannel) ctx.channel();

        log.warn(new KingMeterMarker("Socket,ChannelInactive"),
                "{}|{}", channel.id().asLongText(),channel.remoteAddress());

        worker.dealWithChannelInactive(channel);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestBody requestBody) {
        worker.dealWithBusiness(requestBody, ctx);
    }

    /**
     * catch exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        worker.dealWithException(ctx, cause);
    }

}
