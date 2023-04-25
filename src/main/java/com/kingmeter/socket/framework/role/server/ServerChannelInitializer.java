package com.kingmeter.socket.framework.role.server;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.KMEncoder;
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

    private KMEncoder encoder;
    private KMDecoder decoder;
    private EventLoopGroup handlerEventLoopGroup;
    private AcceptorIdleStateTrigger idleStateTrigger;
    private KMServerHandler serverHandler;

    public ServerChannelInitializer(WorkerTemplate worker, HeaderCode headerCode,
                                    SocketServerConfig socketServerConfig,
                                    EventLoopGroup handlerEventLoopGroup,
                                    KMEncoder encoder,KMDecoder decoder,
                                    AcceptorIdleStateTrigger idleStateTrigger,
                                    KMServerHandler serverHandler) {
        this.worker = worker;
        this.headerCode = headerCode;
        this.readIdleTimeLimit = socketServerConfig.getReadIdleTimeLimit();
        this.writeIdleTimeLimit = socketServerConfig.getWriteIdleTimeLimit();
        this.allIdleTimeLimit = socketServerConfig.getAllIdleTimeLimit();
        this.handlerEventLoopGroup = handlerEventLoopGroup;
        this.socketServerConfig = socketServerConfig;
        this.encoder = encoder;
        this.decoder = decoder;
        this.idleStateTrigger = idleStateTrigger;
        this.serverHandler =serverHandler;
    }


    @Override
    protected void initChannel(SocketChannel channel) {

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast(new LengthFieldBasedFrameDecoder(4096,2,2,4,0));

        pipeline.addLast(new IdleStateHandler(readIdleTimeLimit, writeIdleTimeLimit, allIdleTimeLimit,
                TimeUnit.SECONDS));

        pipeline.addLast("idleTrigger", this.idleStateTrigger);

        pipeline.addLast("decoder", this.decoder);

        pipeline.addLast("encoder", this.encoder);

        pipeline.addLast(this.handlerEventLoopGroup, "business", this.serverHandler);
    }
}
