package com.kingmeter.socket.framework.role.client;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.KMEncoder;
import com.kingmeter.socket.framework.codec.KMDecoder;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private WorkerTemplate worker;
    private HeaderCode headerCode;
    private BusinessWorkerTemplate businessWorker;

    private EventExecutorGroup businessGroup;
    private ClientAdapter clientAdapter;

    private SocketServerConfig serverConfig;


    public ClientChannelInitializer(WorkerTemplate worker, HeaderCode headerCode,
                                    BusinessWorkerTemplate businessWorker,
                                    EventExecutorGroup businessGroup,
                                    ClientAdapter clientAdapter,
                                    SocketServerConfig serverConfig) {
        super();
        this.worker = worker;
        this.headerCode = headerCode;
        this.businessWorker = businessWorker;
        this.businessGroup = businessGroup;
        this.clientAdapter = clientAdapter;
        this.serverConfig = serverConfig;
    }


    @Override
    protected void initChannel(SocketChannel channel) {
        channel.pipeline().addLast("decoder", new KMDecoder(headerCode,serverConfig));

        channel.pipeline().addLast("encoder", new KMEncoder());

        channel.pipeline().addLast("business",
                new ClientHandler(worker, businessWorker, businessGroup,clientAdapter));
    }


}
