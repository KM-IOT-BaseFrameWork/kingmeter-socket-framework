package com.kingmeter.socket.framework.business;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.LoggerConfig;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.ByteUtil;
import com.kingmeter.utils.CRCUtils;
import com.kingmeter.utils.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * template strategy
 * used for receiving data from hardware
 */
@Slf4j
public abstract class WorkerTemplate {

    @Autowired
    private SocketServerConfig config;

    @Autowired
    private LoggerConfig loggerConfig;

    @Autowired
    private HeaderCode headerCode;

    public void run(ChannelHandlerContext ctx, ByteBuf message) {
        String token;
        byte[] tokenArray;
        byte[] dataArray;

        try {
            //1，get function Code
            int functionCode = getFunctionCode(message);
            // get validate length
            int validateLength = getDataLength(message);
            //2,validate CrcCode
            validateCrcCode(functionCode, message, validateLength);
            //3.1 ,get token array
            tokenArray = new byte[headerCode.getTOKEN_LENGTH()];
            message.getBytes(4, tokenArray,
                    0, headerCode.getTOKEN_LENGTH());

            //3.2 get token
            token = getToken(tokenArray);

            //4.1 validate token
            String deviceId = validateTokenAndGetDeviceIdIfInNeed(functionCode, tokenArray, token, ctx);

            //4.2 get data array
            int dataLength = validateLength - headerCode.getTOKEN_LENGTH() - 3;
            dataArray = new byte[dataLength];
            message.getBytes(headerCode.getTOKEN_LENGTH() + 7, dataArray,
                    0, dataLength);

            //5,parse to RequestBody
            int resentFlag = message.getByte(headerCode.getTOKEN_LENGTH() + 4) & 0xFF;

//            readHexString(abc,deviceId,functionCode);

            RequestBody requestBody = parseToRequestBody(
                    deviceId, functionCode,
                    token, tokenArray, resentFlag, dataArray);

            //6,do business
            dealWithBusiness(requestBody, ctx);
        } finally {
            token = null;
            tokenArray = null;
            dataArray = null;
            ReferenceCountUtil.release(message);
        }
    }

    private void readHexString(ByteBuf in, String deviceId, int code) {
        int first_position = in.readerIndex();
        int first_limit = first_position + in.readableBytes();

        byte[] first_TmpBf = new byte[first_limit - first_position];
        in.markReaderIndex();
        in.readBytes(first_TmpBf, 0, first_limit - first_position);

        log.info(new KingMeterMarker("Socket,TCP_IO," + code),
                "{}|{}|{}|{}|{}|{}|{}", deviceId, first_position, first_limit,
                ByteUtil.bytesToHexString(first_TmpBf), "", 0, "");

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

            CRCUtils.getInstance().validate(toCheckByteArray,
                    getCrcCode(message.getByte(dataLength + 4), message.getByte(dataLength + 5)));
        }
    }

    private String getCrcCode(byte crc1, byte crc2) {
        StringBuffer result = new StringBuffer();
        String code1 = Integer.toHexString(((int) crc1) & 0xff);
        String code2 = Integer.toHexString(((int) crc2) & 0xff);
        if (!code1.equals("0")) {
            result.append(code1);
            if (code2.length() == 1) {
                result.append(0);
            }
            result.append(code2);
        } else {
            result.append(code2);
        }
        return result.toString();
    }


    private String getToken(byte[] tokenArray) {
        StringBuffer token = new StringBuffer();
        for (int i = 0; i < tokenArray.length; i++) {
            token.append(Integer.toHexString(tokenArray[i] & 0xFF));
        }
        return token.toString();
    }


    private RequestBody parseToRequestBody(String deviceId,
                                           int functionCode,
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

    /**
     * set siteId into requestBody,in this way,we can known the siteId when we deal with the data from hardware
     * <p>
     * 1, if the server doesn't receive the heartbeats in certain time or the server restart ,the server will delete the siteID and set it to offline.
     * it will login again until the server receive the login data .
     */
    private String validateTokenAndGetDeviceIdIfInNeed(int functionCode, byte[] msg,
                                                       String token, ChannelHandlerContext ctx) {
        if (functionCode == config.getLoginFunctionCode()) {
            return "";
        }
        SocketChannel channel = (SocketChannel) ctx.channel();
        return CacheUtil.getInstance().validateTokenAndGetDeviceIdExceptLogin(functionCode, token, msg, channel);
    }


    private void dealWithBusiness(RequestBody requestBody, ChannelHandlerContext ctx) {

        long deviceId = StringUtil.isEmpty(requestBody.getDeviceId())?0:Long.parseLong(requestBody.getDeviceId());

        RequestStrategy requestStrategy = getRequestStrategy(requestBody.getFunctionCode());

        if (requestStrategy == null) {
            log.info(new KingMeterMarker("Socket,ReLogin,1005"),
                    "{}|{}|{}|{}", deviceId,
                    Integer.toHexString(requestBody.getFunctionCode()),
                    ctx.channel().id().asLongText(),
                    JSONObject.toJSONString(requestBody));
            ctx.close();
            return;
        }

        if (loggerConfig.isLog_model()) {
            log.info(new KingMeterMarker("Socket,TCP_IO,3001"),
                    "{}|{}|{}|{}", deviceId,
                    Integer.toHexString(requestBody.getFunctionCode()),
                    ctx.channel().id().asLongText(),
                    JSONObject.toJSONString(requestBody));
        }

        ResponseBody responseBody = new ResponseBody();
        responseBody.setDeviceId(deviceId);
        responseBody.setSTART_CODE_1(headerCode.getSTART_CODE_1());
        responseBody.setSTART_CODE_2(headerCode.getSTART_CODE_2());
        responseBody.setEND_CODE_1(headerCode.getEND_CODE_1());
        responseBody.setEND_CODE_2(headerCode.getEND_CODE_2());
        responseBody.setToken_length(headerCode.getTOKEN_LENGTH());
        responseBody.setTokenArray(requestBody.getTokenArray());
        requestStrategy.process(requestBody,
                responseBody, ctx);
    }


    /**
     * deal with data from hardware
     *
     * @return
     */
    public abstract RequestStrategy getRequestStrategy(int functionCode);


    public void dealWithException(ChannelHandlerContext ctx, Throwable cause) {

        if (cause instanceof KingMeterException) {
            KingMeterException e = (KingMeterException) cause;
            if (e.getCode() == ResponseCode.StartCodeErrorType.getCode() ||
                    e.getCode() == ResponseCode.EndCodeErrorType.getCode()) {
                ctx.close();
                return;
            }
        }

        SocketChannel channel = (SocketChannel) ctx.channel();
        String channelId = channel.id().asLongText();

        long deviceId = 0;
        if (channel.hasAttr(AttributeKey.<Long>valueOf("DeviceId"))) {
            deviceId = channel.attr(AttributeKey.<Long>valueOf("DeviceId")).get();
        }
        if (deviceId == 0) {
            if (CacheUtil.getInstance().getChannelIdAndDeviceIdMap().containsKey(channelId)) {
                deviceId = Long.parseLong(CacheUtil.getInstance().getChannelIdAndDeviceIdMap().getOrDefault(channelId, "0"));
            }
        }
        log.error(new KingMeterMarker("Socket,DeviceException,4003"),
                "{}|{}|{}", deviceId, cause.getMessage(),
                channelId, cause);
    }


    public void dealWithOffline(SocketChannel channel, String deviceId) {
        CacheUtil.getInstance().dealWithOffLine(channel, deviceId);

        log.info(new KingMeterMarker("Socket,DeviceOffline,1004"),
                "{}|{}", deviceId, channel.id().asLongText());

        doDealWithOffline(channel, deviceId);
    }

    /**
     * @param channel
     * @param deviceId
     */
    public abstract void doDealWithOffline(SocketChannel channel, String deviceId);
}
