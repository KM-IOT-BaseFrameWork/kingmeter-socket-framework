package com.kingmeter.socket.framework.role.client;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.Decoder;
import com.kingmeter.socket.framework.codec.Encoder;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.LoggerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private WorkerTemplate worker;
    private HeaderCode headerCode;
    private BusinessWorkerTemplate businessWorker;

    private EventExecutorGroup businessGroup;
    private ClientAdapter clientAdapter;

    private LoggerConfig loggerConfig;


    public ClientChannelInitializer(WorkerTemplate worker, HeaderCode headerCode,
                                    BusinessWorkerTemplate businessWorker,
                                    EventExecutorGroup businessGroup,
                                    ClientAdapter clientAdapter,
                                    LoggerConfig loggerConfig) {
        super();
        this.worker = worker;
        this.headerCode = headerCode;
        this.businessWorker = businessWorker;
        this.businessGroup = businessGroup;
        this.clientAdapter = clientAdapter;
        this.loggerConfig = loggerConfig;
    }


    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast("decoder", new Decoder(headerCode,loggerConfig));

        channel.pipeline().addLast("encoder", new Encoder(loggerConfig));

        channel.pipeline().addLast("business",
                new ClientHandler(worker, businessWorker, businessGroup,clientAdapter));
    }


}
