package com.kingmeter.socket.framework.role.client;


import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component()
public class ClientAdapter {

    @Autowired
    private HeaderCode headerCode;

    @Autowired
    private SocketServerConfig socketServerConfig;

    @Autowired
    private LoggerConfig loggerConfig;

    private EventLoopGroup group = new NioEventLoopGroup(5);
    private final EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(5);

    private Bootstrap b = new Bootstrap();
    @Autowired
    private WorkerTemplate worker;
    @Autowired
    private BusinessWorkerTemplate businessWorker;
    @Autowired
    private ClientAdapter clientAdapter;


    public void bind(String host, int port, long siteIdStart, int deviceCount, String password) {
        if (b.group() == null) {
            if (socketServerConfig.isUseLinuxNativeEpoll()) {
                b.group(group).channel(EpollSocketChannel.class);
            } else {
                b.group(group).channel(NioSocketChannel.class);
            }
        }
        bind(siteIdStart, deviceCount, host, port, password);
    }

    private void bind(long siteIdStart, int deviceCount, String host, int port, String password) {
        b.remoteAddress(new InetSocketAddress(host, port))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ClientChannelInitializer(
                        worker, headerCode,
                        businessWorker, businessGroup, clientAdapter, loggerConfig));

        for (int i = 0; i < deviceCount; i++) {
            Long siteId = siteIdStart + i;
            try {
                connect(b, siteId, host, port, password);
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }


    public void connect(Bootstrap b, long siteId, String host, int port, String password) {
        try {
            b.connect().addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.info("client created connection failed {}", siteId);
                } else {
                    log.info("client created connection succeed {}", siteId);
                    SocketChannel channel = (SocketChannel) f.channel();

                    //siteId
                    AttributeKey<Long> siteIdKey = AttributeKey.valueOf("siteId");
                    Attribute<Long> siteIdAttr = channel.attr(siteIdKey);
                    siteIdAttr.set(siteId);

                    //password
                    AttributeKey<String> passwordKey = AttributeKey.valueOf("password");
                    Attribute<String> passwordAttr = channel.attr(passwordKey);
                    passwordAttr.set(password);

                    //host
                    AttributeKey<String> hostKey = AttributeKey.valueOf("host");
                    Attribute<String> hostAttr = channel.attr(hostKey);
                    hostAttr.set(host);

                    //port
                    AttributeKey<Integer> portKey = AttributeKey.valueOf("port");
                    Attribute<Integer> portAttr = channel.attr(portKey);
                    portAttr.set(port);

                    CacheUtil.getInstance().getChannelIdAndChannelMap()
                            .put(channel.id().asLongText(),
                                    channel);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        try {
            for (SocketChannel channel :
                    CacheUtil.getInstance().getChannelIdAndChannelMap().values()) {
                channel.close();
            }
            group.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Data
    @AllArgsConstructor
    class SucceedFlag {
        private boolean isSucceed;
    }
}
