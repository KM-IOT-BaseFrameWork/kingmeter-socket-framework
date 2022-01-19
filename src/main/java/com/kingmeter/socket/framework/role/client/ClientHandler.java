package com.kingmeter.socket.framework.role.client;


import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.socket.framework.business.WorkerTemplate;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private ClientAdapter clientAdapter;
    private WorkerTemplate worker;
    private BusinessWorkerTemplate businessWorker;

    private EventExecutorGroup businessGroup;

    public ClientHandler(WorkerTemplate worker, BusinessWorkerTemplate businessWorker,
                         EventExecutorGroup businessGroup,
                         ClientAdapter clientAdapter) {
        this.worker = worker;
        this.businessWorker = businessWorker;
        this.businessGroup = businessGroup;
        this.clientAdapter = clientAdapter;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        SocketChannel channel = (SocketChannel) ctx.channel();
        channel.config().setWriteBufferHighWaterMark(10 * 1024 * 1024);

        AttributeKey<Long> nameAttrKey = AttributeKey.valueOf("siteId");
        Attribute<Long> attr = channel.attr(nameAttrKey);
        Long siteId = attr.get();

        //password
        AttributeKey<String> passwordKey = AttributeKey.valueOf("password");
        Attribute<String> passwordAttr = channel.attr(passwordKey);
        String password = passwordAttr.get();

        log.info("active siteId is {},password is {}", siteId,password);

        //add businessGroup into channel , for running heartbeat job
        AttributeKey<EventExecutorGroup> executorGroupAttributeKey =
                AttributeKey.valueOf("businessGroup");
        Attribute<EventExecutorGroup> groupAttr = channel.attr(executorGroupAttributeKey);
        groupAttr.set(businessGroup);

        businessGroup.submit(() ->
                businessWorker.login(ctx, siteId,password)
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
//        log.info(new KingMeterMarker("Socket,ChannelInactive"),
//                "{}", ctx.channel().id().asLongText());
        SocketChannel channel = (SocketChannel) ctx.channel();
//        log.info("{}|DeviceOffline|1005|{}", deviceId,channel.id().asLongText());

        AttributeKey<Long> nameAttrKey = AttributeKey.valueOf("siteId");
        Attribute<Long> attr = channel.attr(nameAttrKey);
        Long siteId = attr.get();

        //password
        AttributeKey<String> passwordKey = AttributeKey.valueOf("password");
        Attribute<String> passwordAttr = channel.attr(passwordKey);
        String password = passwordAttr.get();

        //host
        AttributeKey<String> hostKey = AttributeKey.valueOf("host");
        Attribute<String> hostAttr = channel.attr(hostKey);
        String host = hostAttr.get();

        //port
        AttributeKey<Integer> portKey = AttributeKey.valueOf("port");
        Attribute<Integer> portAttr = channel.attr(portKey);
        int port = portAttr.get();


        log.info(new KingMeterMarker("Socket,DeviceOffline,1005"),
                "{}|{}", siteId, channel.id().asLongText());

        //connect new server
        if(CacheUtil.getInstance().getDeviceResultMap().containsKey(siteId+"_reLogin")){
            log.info(new KingMeterMarker("Socket,ReLogin,1007"),
                    "{}", siteId);

            clientAdapter.bind(host,port,siteId,1,password);
            businessWorker.login(ctx,siteId,password);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        msg.retain();
        businessGroup.submit(() -> {
            worker.run(ctx,msg);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
