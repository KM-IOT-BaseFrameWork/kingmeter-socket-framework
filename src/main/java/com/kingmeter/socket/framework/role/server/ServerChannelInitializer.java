package com.kingmeter.socket.framework.role.server;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.Decoder;
import com.kingmeter.socket.framework.codec.Encoder;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.idletrigger.AcceptorIdleStateTrigger;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private HeaderCode headerCode;

    private WorkerTemplate worker;

    private int readIdleTimeLimit;
    private int writeIdleTimeLimit;
    private int allIdleTimeLimit;

    private LoggerConfig loggerConfig;

    private EventLoopGroup handlerEventLoopGroup;

    public ServerChannelInitializer(WorkerTemplate worker, HeaderCode headerCode,
                                    SocketServerConfig socketServerConfig,
                                    LoggerConfig loggerConfig,
                                    EventLoopGroup handlerEventLoopGroup) {
        this.worker = worker;
        this.headerCode = headerCode;
        this.readIdleTimeLimit = socketServerConfig.getReadIdleTimeLimit();
        this.writeIdleTimeLimit = socketServerConfig.getWriteIdleTimeLimit();
        this.allIdleTimeLimit = socketServerConfig.getAllIdleTimeLimit();
        this.handlerEventLoopGroup = handlerEventLoopGroup;
        this.loggerConfig = loggerConfig;
    }


    @Override
    protected void initChannel(SocketChannel channel) {

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast(new IdleStateHandler(readIdleTimeLimit, writeIdleTimeLimit, allIdleTimeLimit,
                TimeUnit.SECONDS));

        pipeline.addLast("idleTrigger", new AcceptorIdleStateTrigger(worker));

        pipeline.addLast("decoder", new Decoder(headerCode, loggerConfig));

        pipeline.addLast("encoder", new Encoder(loggerConfig));

        pipeline.addLast(this.handlerEventLoopGroup, "business", new ServerHandler(worker));
    }
}
