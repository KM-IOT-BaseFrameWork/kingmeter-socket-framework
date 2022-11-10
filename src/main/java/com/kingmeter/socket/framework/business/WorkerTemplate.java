package com.kingmeter.socket.framework.business;


import com.alibaba.fastjson.JSONObject;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.dto.RequestBody;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.strategy.RequestStrategy;
import com.kingmeter.socket.framework.util.CacheUtil;
import com.kingmeter.utils.StringUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * template strategy
 * used for receiving data from hardware
 */
@Data
@Slf4j
public abstract class WorkerTemplate {

    @Autowired
    private SocketServerConfig config;


    @Autowired
    private HeaderCode headerCode;


    public void dealWithBusiness(RequestBody requestBody, ChannelHandlerContext ctx) {

        long deviceId = StringUtil.isEmpty(requestBody.getDeviceId()) ? 0 : Long.parseLong(requestBody.getDeviceId());

        RequestStrategy requestStrategy = getRequestStrategy(requestBody.getFunctionCode());

        if (requestStrategy == null) {
            log.error(new KingMeterMarker("Socket,ReLogin,1005"),
                    "{}|{}|{}|{}", deviceId,
                    Integer.toHexString(requestBody.getFunctionCode()),
                    ctx.channel().id().asLongText(),
                    JSONObject.toJSONString(requestBody));
            ctx.close();
            return;
        }

        log.trace(new KingMeterMarker("Socket,TCP_IO,3001"),
                "{}|{}|{}|{}", deviceId,
                Integer.toHexString(requestBody.getFunctionCode()),
                ctx.channel().id().asLongText(),
                JSONObject.toJSONString(requestBody));

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
        channel.close();
        Throwable source = cause.getCause();
        if (source instanceof KingMeterException) {
            KingMeterException e = (KingMeterException) source;
            if (e.getCode() == ResponseCode.StartCodeErrorType.getCode() ||
                    e.getCode() == ResponseCode.EndCodeErrorType.getCode() ||
                    e.getCode() == ResponseCode.CRC16CodeErrorType.getCode() ||
                    e.getCode() == ResponseCode.Device_Token_Not_Correct.getCode()) {
                log.error("{}|{}", e.getCode(),"start or end code, crc16 error,token not correct ");
                return;
            }
        }

        // 如果是 connection reset by peer ，那么要更新缓存内容
        if (cause instanceof IOException) {
            log.error("update channel and device info when connection reset by peer {}|{}|{}",
                    deviceId, channelId, channel.remoteAddress());
            CacheUtil.getInstance().dealWithConnectionReset(String.valueOf(deviceId), channel);
            return;
        }

        if (cause instanceof TooLongFrameException) {
            log.error("io.netty.handler.codec.TooLongFrameException: Adjusted frame length exceeds {}|{}|{}",
                    deviceId, channelId, channel.remoteAddress());
            CacheUtil.getInstance().dealWithConnectionReset(String.valueOf(deviceId), channel);
            return;
        }

        log.error(new KingMeterMarker("Socket,DeviceException,4003"),
                "{}|{}|{}", deviceId, channelId, getStackTraceInfo(cause));
    }

    /**
     * 获取e.printStackTrace() 的具体信息，赋值给String 变量，并返回
     *
     * @param e Exception
     * @return e.printStackTrace() 中 的信息
     */
    private static String getStackTraceInfo(Throwable e) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);//将出错的栈信息输出到printWriter中
            pw.flush();
            sw.flush();
            return sw.toString();
        } catch (Exception ex) {
            return "printStackTrace()转换错误";
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }

    }


    public void dealWithOffline(SocketChannel channel, String deviceId) {
        CacheUtil.getInstance().dealWithOffLine(channel);

        log.warn(new KingMeterMarker("Socket,DeviceOffline,1004"),
                "{}|{}|{}", deviceId, channel.id().asLongText(), channel.remoteAddress());

        doDealWithOffline(channel, deviceId);
    }

    public void dealWithChannelInactive(SocketChannel channel) {
        CacheUtil.getInstance().dealWithChannelInactive(channel);
    }

    /**
     * @param channel
     * @param deviceId
     */
    public abstract void doDealWithOffline(SocketChannel channel, String deviceId);

    /**
     * 测试连接是否还能使用
     *
     * @return responseBody
     */
    public abstract ResponseBody getConnectionTestCommand(String deviceId);
}
