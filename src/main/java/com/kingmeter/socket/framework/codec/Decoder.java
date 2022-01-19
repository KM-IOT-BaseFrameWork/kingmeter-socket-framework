package com.kingmeter.socket.framework.codec;

import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;


@Slf4j
public class Decoder extends ByteToMessageDecoder {

    private final HeaderCode headerCode;
    private final int packHeadLength = 4;

    private final LoggerConfig loggerConfig;

    public Decoder(HeaderCode headerCode, LoggerConfig loggerConfig) {
        this.headerCode = headerCode;
        this.loggerConfig = loggerConfig;
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        Channel channel = ctx.channel();
        long deviceId = 0;
        if (channel.hasAttr(AttributeKey.<Long>valueOf("DeviceId"))) {
            deviceId = ctx.channel().attr(AttributeKey.<Long>valueOf("DeviceId")).get();
        }

        checkByteDetail(in, deviceId, channel);

        final int position = in.readerIndex();
        final int limit = position + in.readableBytes();

        cutOutMessage(channel, in, packHeadLength, position, limit, deviceId, out);
    }

    private void cutOutMessage(Channel channel, ByteBuf in, int packHeadLength,
                               int position, int limit, long deviceId, List<Object> out) {

        if (in.readableBytes() > packHeadLength) {
            in.markReaderIndex();//mark current position，convenient for reset in the future if in need
            //judge the header byte is right
            if (headerCode.getSTART_CODE_1() != in.getByte(position) &&
                    headerCode.getSTART_CODE_2() != in.getByte(position + 1)) {
//                  return 0;
                //read all and clear
                byte[] tmpBf = new byte[limit - position];
                in.readBytes(tmpBf, 0, limit - position);

                if (loggerConfig.isLog_exception()) {
                    log.info(new KingMeterMarker("Socket,DeviceException,4001"),
                            "{}|{}|{}|{}", deviceId, position, limit, ByteUtil.bytesToHexString(tmpBf));
                }

                throw new KingMeterException(ResponseCode.StartCodeErrorType);
            }

            final int lengthIndex = in.readerIndex() + 2;
            final int length = ((in.getByte(lengthIndex) & 0xFF) << 8) + (in.getByte(lengthIndex + 1) & 0xFF);

            if (in.readableBytes() < length + 8) {
                //the content is not big enough, it should reset index ,and get more bytes
                in.resetReaderIndex();
                return;
            } else {
                //it means the content is big enough for reading
                //check the end content
                if (headerCode.getEND_CODE_1() != in.getByte(position + length + 6) &&
                        headerCode.getEND_CODE_2() != in.getByte(position + length + 7)) {
                    byte[] tmpBf = new byte[limit - position];
                    in.readBytes(tmpBf, 0, limit - position);

                    if (loggerConfig.isLog_exception()) {
                        log.info(new KingMeterMarker("Socket,DeviceException,4002"),
                                "{}|{}|{}|{}|{}", deviceId, length, in.getByte(position + length + 6), in.getByte(position + length + 7), ByteUtil.bytesToHexString(tmpBf));
                    }

                    throw new KingMeterException(ResponseCode.EndCodeErrorType);
                }

                PooledByteBufAllocator allocator = (PooledByteBufAllocator) channel.config().getAllocator();
                ByteBuf result = allocator.heapBuffer(length + 8);
                in.readBytes(result, length + 8);
                out.add(result);
            }
        }
    }

    private void checkByteDetail(ByteBuf in, long deviceId, Channel channel) {
        if (!loggerConfig.isLog_message()) {
            return;
        }

        int first_position = in.readerIndex();
        int first_limit = first_position + in.readableBytes();

        byte[] first_TmpBf = new byte[first_limit - first_position];
        in.markReaderIndex();
        in.readBytes(first_TmpBf, 0, first_limit - first_position);

        InetSocketAddress inSocket = (InetSocketAddress) channel.remoteAddress();
        String ip = inSocket.getAddress().getHostAddress();
        int port = inSocket.getPort();

        log.info(new KingMeterMarker("Socket,TCP_IO,2001"),
                "{}|{}|{}|{}|{}|{}|{}", deviceId, first_position, first_limit,
                ByteUtil.bytesToHexString(first_TmpBf), ip, port, channel.id().asLongText());

        in.resetReaderIndex();
    }

}
