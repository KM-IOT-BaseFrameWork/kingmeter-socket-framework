package com.kingmeter.socket.framework.role.server;

import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.codec.KMDecoder;
import com.kingmeter.socket.framework.codec.KMEncoder;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.idletrigger.AcceptorIdleStateTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component()
public class NettyServer {

    @Autowired
    private SocketServerConfig socketServerConfig;

    @Autowired
    private HeaderCode headerCode;


    private EventLoopGroup handlerEventLoopGroup = new DefaultEventLoopGroup();
    private EventLoopGroup bossGroup; //NioEventLoopGroup extends MultithreadEventLoopGroup Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    private EventLoopGroup workerGroup;


    @Autowired
    private WorkerTemplate worker;


    private final KMEncoder encoder = new KMEncoder();

    public void bind() {
        startAsync().syncUninterruptibly();
    }

    private Future<Void> startAsync() {
//        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf("PARANOID"));

        InetSocketAddress address = new InetSocketAddress(socketServerConfig.getSocketPort());
        initGroups();

        Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
        if (socketServerConfig.isUseLinuxNativeEpoll()) {
            channelClass = EpollServerSocketChannel.class;
        }

        KMDecoder decoder = new KMDecoder(headerCode,socketServerConfig);
        AcceptorIdleStateTrigger idleStateTrigger= new AcceptorIdleStateTrigger(worker);

        KMServerHandler serverHandler = new KMServerHandler(worker);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(channelClass) //非阻塞模式
                .childHandler(new ServerChannelInitializer(
                        worker, headerCode,
                        socketServerConfig,
                        handlerEventLoopGroup,
                        encoder,decoder,idleStateTrigger,serverHandler));

        applyConnectionOptions(b);

        return b.bind(address).addListener((future) -> {
            if (future.isSuccess()) {
                log.info("Socket server started at port: {}", socketServerConfig.getSocketPort());
            } else {
                log.error("Socket server start failed at port: {}!", socketServerConfig.getSocketPort());
            }
        });
    }


    private void applyConnectionOptions(ServerBootstrap bootstrap) {

        bootstrap.childOption(ChannelOption.TCP_NODELAY, socketServerConfig.isTcpNoDelay());
        if (socketServerConfig.getTcpSendBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, socketServerConfig.getTcpSendBufferSize());
        }
        if (socketServerConfig.getTcpReceiveBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, socketServerConfig.getTcpReceiveBufferSize());
            bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(socketServerConfig.getTcpReceiveBufferSize()));
        } else {
            bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator());
        }

        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, socketServerConfig.isTcpKeepAlive());
        bootstrap.childOption(ChannelOption.SO_LINGER, socketServerConfig.getSoLinger());

        bootstrap.option(ChannelOption.SO_REUSEADDR, socketServerConfig.isReuseAddress());
        bootstrap.option(ChannelOption.SO_BACKLOG, socketServerConfig.getAcceptBackLog());

        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.option(ChannelOption.AUTO_READ, true);
    }

    protected void initGroups() {
        if (socketServerConfig.isUseLinuxNativeEpoll()) {
            bossGroup = new EpollEventLoopGroup(socketServerConfig.getBossThreads());
            workerGroup = new EpollEventLoopGroup(socketServerConfig.getWorkerThreads());
        } else {
            bossGroup = new NioEventLoopGroup(socketServerConfig.getBossThreads());
            workerGroup = new NioEventLoopGroup(socketServerConfig.getWorkerThreads());
        }
    }


    public void destroy() {
//        executorService.shutdownNow();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        handlerEventLoopGroup.shutdownGracefully();
        log.error("Socket server stopped");
    }


}
