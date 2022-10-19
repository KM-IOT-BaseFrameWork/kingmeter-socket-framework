package com.kingmeter.socket.framework.role.server;

import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

@Slf4j
//@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private WorkerTemplate worker;

    public ServerHandler(WorkerTemplate worker) {
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
//        executorService.execute(() -> {
//
//        });
        //            log.info(new KingMeterMarker("Socket,ChannelInactive"),
//                "{}", ctx.channel().id().asLongText());
        SocketChannel channel = (SocketChannel) ctx.channel();
        String deviceId = CacheUtil.getInstance().getChannelIdAndDeviceIdMap().get(channel.id().asLongText());
//            log.info("{}|DeviceOffline|1005|{}", deviceId, channel.id().asLongText());
        log.info(new KingMeterMarker("Socket,DeviceOffline,1005"),
                "{}|{}", deviceId, channel.id().asLongText());

        worker.dealWithOffline(channel, deviceId);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
//        msg.retain();
//        log.info("msg ref_cnt is {}",msg.refCnt());
        worker.run(ctx, msg);
    }

    /**
     * catch exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        executorService.submit(() -> {
//            worker.dealWithException(ctx, cause);
//        });
        worker.dealWithException(ctx, cause);
    }

}
