package com.kingmeter.socket.framework.codec;

import ch.qos.logback.classic.Level;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.ByteUtil;
import com.kingmeter.utils.CRCUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description:
 * @author: crazyandy
 */
@Slf4j
@ChannelHandler.Sharable
public class KMDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final HeaderCode headerCode;
    private SocketServerConfig config;

    public KMDecoder(HeaderCode headerCode, SocketServerConfig config) {
        this.headerCode = headerCode;
        this.config = config;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        final int limit = in.readableBytes();
        Channel channel = ctx.channel();
//        Channel channel = null;
        long deviceId = 0;
        if (channel.hasAttr(AttributeKey.<Long>valueOf("DeviceId"))) {
            deviceId = ctx.channel().attr(AttributeKey.<Long>valueOf("DeviceId")).get();
        }

        if (headerCode.getSTART_CODE_1() != in.getByte(0) &&
                headerCode.getSTART_CODE_2() != in.getByte(1)) {
            checkByteDetail(in, deviceId, channel, Level.ERROR);
            throw new KingMeterException(ResponseCode.StartCodeErrorType);
        }
        if (headerCode.getEND_CODE_1() != in.getByte(limit - 2) &&
                headerCode.getEND_CODE_2() != in.getByte(limit - 1)) {
            checkByteDetail(in, deviceId, channel, Level.ERROR);
            throw new KingMeterException(ResponseCode.EndCodeErrorType);
        }

        checkByteDetail(in, deviceId, channel, Level.TRACE);

        packageRequestBody(ctx, in, out);
    }

    private void packageRequestBody(ChannelHandlerContext ctx, ByteBuf message, List<Object> out) {
        //1，get function Code
        int functionCode = getFunctionCode(message);
        // get validate length
        int validateLength = getDataLength(message);
        //2,validate CrcCode
        validateCrcCode(functionCode, message, validateLength);
        //3.1 ,get token array
        byte[] tokenArray = new byte[headerCode.getTOKEN_LENGTH()];
        message.getBytes(4, tokenArray,
                0, headerCode.getTOKEN_LENGTH());

        //3.2 get token
        String token = getToken(tokenArray);

        //4.1 validate token
//        String deviceId = "";
        String deviceId = validateTokenAndGetDeviceIdIfInNeed(functionCode, tokenArray, token, ctx, message);

        //4.2 get data array
        int dataLength = validateLength - headerCode.getTOKEN_LENGTH() - 3;
        byte[] dataArray = new byte[dataLength];
        message.getBytes(headerCode.getTOKEN_LENGTH() + 7, dataArray,
                0, dataLength);

        //5,parse to RequestBody
        int resentFlag = message.getByte(headerCode.getTOKEN_LENGTH() + 4) & 0xFF;

        RequestBody requestBody = parseToRequestBody(deviceId,
                functionCode,
                token, tokenArray, resentFlag, dataArray);

        out.add(requestBody);
    }

    private RequestBody parseToRequestBody(String deviceId, int functionCode,
                                           String token,
                                           byte[] tokenArray,
                                           int resentFlag,
                                           byte[] dataArray) {
        StringBuffer data = new StringBuffer();
        for (int i = 0; i < dataArray.length; i++) {
            data.append((char) dataArray[i]);
        }
        return new RequestBody(deviceId, functionCode, token, tokenArray,
                resentFlag, data.toString());
    }

    private void checkByteDetail(ByteBuf in, long deviceId, Channel channel, Level logLevel) {
        int first_position = in.readerIndex();
        int first_limit = first_position + in.readableBytes();

        byte[] first_TmpBf = new byte[first_limit - first_position];
        in.markReaderIndex();
        in.readBytes(first_TmpBf, 0, first_limit - first_position);

        InetSocketAddress inSocket = (InetSocketAddress) channel.remoteAddress();
        String ip = inSocket.getAddress().getHostAddress();
        int port = inSocket.getPort();

        if (logLevel.equals(Level.ERROR)) {
            log.error(new KingMeterMarker("Socket,TCP_IO,2001"),
                    "{}|{}|{}|{}|{}|{}|{}", deviceId, first_position, first_limit,
                    ByteUtil.bytesToHexString(first_TmpBf), ip, port, channel.id().asLongText());
        } else {
            log.trace(new KingMeterMarker("Socket,TCP_IO,2001"),
                    "{}|{}|{}|{}|{}|{}|{}", deviceId, first_position, first_limit,
                    ByteUtil.bytesToHexString(first_TmpBf), ip, port, channel.id().asLongText());
        }


        in.resetReaderIndex();
    }


    private int getFunctionCode(ByteBuf message) {
        return ((message.getByte(headerCode.getTOKEN_LENGTH() + 5) & 0xFF) << 8) + (message.getByte(headerCode.getTOKEN_LENGTH() + 6) & 0xFF);
    }

    private int getDataLength(ByteBuf message) {
        return ((message.getByte(2) & 0xFF) << 8) + (message.getByte(3) & 0xFF);
    }

    private void validateCrcCode(int functionCode, ByteBuf message, int dataLength) {
        //if login , let it go ,just for more effective
        if (functionCode != config.getLoginFunctionCode()) {
            byte[] toCheckByteArray = new byte[dataLength];

            message.getBytes(4, toCheckByteArray, 0, dataLength);

            if (!CRCUtils.getInstance().validate(toCheckByteArray,
                    message.getByte(dataLength + 4), message.getByte(dataLength + 5))) {
                throw new KingMeterException(ResponseCode.CRC16CodeErrorType);
            }
        }
    }


    private String getToken(byte[] tokenArray) {
        StringBuffer token = new StringBuffer();
        for (int i = 0; i < tokenArray.length; i++) {
            token.append(Integer.toHexString(tokenArray[i] & 0xFF));
        }
        return token.toString();
    }

    /**
     * set siteId into requestBody,in this way,we can known the siteId when we deal with the data from hardware
     * <p>
     * 1, if the server doesn't receive the heartbeats in certain time or the server restart ,the server will delete the siteID and set it to offline.
     * it will login again until the server receive the login data .
     */
    private String validateTokenAndGetDeviceIdIfInNeed(int functionCode, byte[] tokenArray,
                                                       String token, ChannelHandlerContext ctx, ByteBuf message) {
        if (functionCode == config.getLoginFunctionCode()) {

            int first_position = message.readerIndex();
            int first_limit = first_position + message.readableBytes();

            byte[] first_TmpBf = new byte[first_limit - first_position];
            message.markReaderIndex();
            message.readBytes(first_TmpBf, 0, first_limit - first_position);

            log.warn(new KingMeterMarker("Socket,ReLogin,1008"),
                    "{}|{}|{}|{}", Integer.toHexString(functionCode), ctx.channel().id().asLongText(),
                    token, ByteUtil.bytesToHexString(first_TmpBf));

            message.resetReaderIndex();

            return "";
        }
        SocketChannel channel = (SocketChannel) ctx.channel();
        return CacheUtil.getInstance().validateTokenAndGetDeviceIdExceptLogin(functionCode, token, tokenArray, channel, message);
    }
}
