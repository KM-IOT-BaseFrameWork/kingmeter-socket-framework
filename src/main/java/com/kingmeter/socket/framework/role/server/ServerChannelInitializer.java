package com.kingmeter.socket.framework.role.server;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.Encoder;
import com.kingmeter.socket.framework.codec.KMDecoder;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.idletrigger.AcceptorIdleStateTrigger;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {


    private SocketServerConfig socketServerConfig;

    private HeaderCode headerCode;

    private WorkerTemplate worker;

    private int readIdleTimeLimit;
    private int writeIdleTimeLimit;
    private int allIdleTimeLimit;

    private EventLoopGroup handlerEventLoopGroup;

    public ServerChannelInitializer(WorkerTemplate worker, HeaderCode headerCode,
                                    SocketServerConfig socketServerConfig,
                                    EventLoopGroup handlerEventLoopGroup) {
        this.worker = worker;
        this.headerCode = headerCode;
        this.readIdleTimeLimit = socketServerConfig.getReadIdleTimeLimit();
        this.writeIdleTimeLimit = socketServerConfig.getWriteIdleTimeLimit();
        this.allIdleTimeLimit = socketServerConfig.getAllIdleTimeLimit();
        this.handlerEventLoopGroup = handlerEventLoopGroup;
        this.socketServerConfig = socketServerConfig;
    }


    @Override
    protected void initChannel(SocketChannel channel) {

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536,2,2,4,0));

        pipeline.addLast(new IdleStateHandler(readIdleTimeLimit, writeIdleTimeLimit, allIdleTimeLimit,
                TimeUnit.SECONDS));

        pipeline.addLast("idleTrigger", new AcceptorIdleStateTrigger(worker));

        pipeline.addLast("decoder", new KMDecoder(headerCode,socketServerConfig));

        pipeline.addLast("encoder", new Encoder());

        pipeline.addLast(this.handlerEventLoopGroup, "business", new KMServerHandler(worker));
    }
}
