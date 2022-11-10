package com.kingmeter.socket.framework.util;

import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.codec.Encoder;
import com.kingmeter.socket.framework.codec.KMDecoder;
import com.kingmeter.socket.framework.idletrigger.AcceptorIdleStateTrigger;
import com.kingmeter.socket.framework.role.server.KMServerHandler;
import com.kingmeter.utils.ByteUtil;
import com.kingmeter.utils.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
@Data
@Slf4j
public class CacheUtil {

    private static CacheUtil instance = null;
    private static final Object LOCK = new Object();

    private CacheUtil() {
    }

    public static CacheUtil getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            if (null == instance) {
                instance = new CacheUtil();
            }
        }
        return instance;
    }

    /**
     * key: channel id
     * value : channel
     */
    private ConcurrentHashMap<String, SocketChannel> channelIdAndChannelMap = new ConcurrentHashMap();

    /**
     * key: channel id
     * value : device id
     */
    private ConcurrentHashMap<String, String> channelIdAndDeviceIdMap = new ConcurrentHashMap();

    /**
     * key: device id
     * value : channel
     */
    private ConcurrentHashMap<String, SocketChannel> deviceIdAndChannelMap = new ConcurrentHashMap();

    /**
     * key: deviceId
     * value: token
     */
    private ConcurrentHashMap<String, String> deviceIdAndTokenMap = new ConcurrentHashMap();

    /**
     * key: deviceId
     * value: token
     */
    private ConcurrentHashMap<String, byte[]> deviceIdAndTokenArrayMap = new ConcurrentHashMap();


    /**
     * key : token
     * value : deviceId
     */
    private ConcurrentHashMap<String, String> tokenAndDeviceIdMap = new ConcurrentHashMap();

    /**
     * get info from device
     * key {lockId}_{database} ,value : map
     */
    private ConcurrentHashMap<String, Map<String, String>> deviceResultMap = new ConcurrentHashMap();

    /**
     * key {lockId} , value :map
     * the information of the device
     */
    private ConcurrentHashMap<Long, Map<String, String>> deviceInfoMap = new ConcurrentHashMap();


    public String validateTokenAndGetDeviceIdExceptLogin(
            int functionCode, String token, byte[] tokenArray, SocketChannel newChannel, ByteBuf message) {

        String deviceId = tokenAndDeviceIdMap.getOrDefault(token, "0");
        String newChannelId = newChannel.id().asLongText();

        if (channelIdAndChannelMap.containsKey(newChannelId)) {
            //this show us that this channel is created before,we would validate the token here
            String oldToken = deviceIdAndTokenMap.getOrDefault(deviceId, "");

            if (StringUtil.isNotEmpty(oldToken)) {
                if (token.equals(oldToken)) {
                    return deviceId;
                } else {
                    log.error(new KingMeterMarker("Socket,ReLogin,1001"),
                            "{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId,
                            ByteUtil.bytesToHexString(tokenArray));
                    throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
                }
            } else {
                log.error(new KingMeterMarker("Socket,ReLogin,1002"),
                        "{}|{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId,
                        ByteUtil.bytesToHexString(tokenArray), token);
                throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
            }
        } else {
            /**
             * perhaps the client change another port to connect to the server this time
             * we should validate the token ,if the token exist in memory , we should update the channelId for this device
             */
            if (tokenAndDeviceIdMap.containsKey(token)) {
                dealWithChangeIpOrPort(deviceId, newChannel);
                return deviceId;
            } else {
                /**
                 * the token does not exist in memory , so we think it's invalid , we should close this connection right now.
                 */
                log.error(" ~~~~~~~~~~ tokenAndDeviceIdMap start:");
                tokenAndDeviceIdMap.entrySet().forEach(entry -> {
                    log.error("id : {} , token : {}", entry.getValue(), entry.getKey());
                });
                log.error(" ~~~~~~~~~~ tokenAndDeviceIdMap end:");

                log.error(" ######### channelIdAndChannelMap start:");
                channelIdAndChannelMap.entrySet().forEach(entry -> {
                    log.error("channelId : {} ", entry.getKey());
                });
                log.error(" ######### channelIdAndChannelMap end:");

                int first_position = message.readerIndex();
                int first_limit = first_position + message.readableBytes();

                byte[] first_TmpBf = new byte[first_limit - first_position];
                message.markReaderIndex();
                message.readBytes(first_TmpBf, 0, first_limit - first_position);

                log.error(new KingMeterMarker("Socket,ReLogin,1003"),
                        "{}|{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId,
                        token, ByteUtil.bytesToHexString(first_TmpBf));

                message.resetReaderIndex();
                throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
            }
        }
    }

    /**
     * 登录的时候，把原来的内容清理掉，用最新的
     *
     * @param deviceId
     * @param token
     * @param tokenArray
     * @param channel
     */
    public void dealWithLoginSucceed(String deviceId, String token,
                                     byte[] tokenArray, SocketChannel channel) {

        String newChannelId = channel.id().asLongText();
        if (deviceIdAndChannelMap.containsKey(deviceId)) {
            //原来内存中有该设备之前的信息
            //很有可能是设备 上次登录使用的端口没有成功，又重新换了一个端口来登录
            SocketChannel oldChannel = deviceIdAndChannelMap.get(deviceId);
            if (oldChannel != null) {
                //把老的记录都给清除掉
                String oldChannelId = oldChannel.id().asLongText();
                channelIdAndChannelMap.remove(oldChannelId);
                channelIdAndDeviceIdMap.remove(oldChannelId);
                deviceIdAndChannelMap.remove(deviceId);
                oldChannel.deregister();
                oldChannel.close();
            }
        }
        channelIdAndChannelMap.put(newChannelId, channel);
        channelIdAndDeviceIdMap.put(newChannelId, deviceId);
        deviceIdAndChannelMap.put(deviceId, channel);
        String oldToken = deviceIdAndTokenMap.getOrDefault(deviceId, "");
        if (StringUtil.isNotEmpty(oldToken) && !token.equals(oldToken)) tokenAndDeviceIdMap.remove(oldToken);
        deviceIdAndTokenMap.put(deviceId, token);
        tokenAndDeviceIdMap.put(token, deviceId);
        deviceIdAndTokenArrayMap.put(deviceId, tokenArray);

        channel.attr(AttributeKey.<Long>valueOf("DeviceId")).set(Long.parseLong(deviceId));
    }


    public void dealWithConnectionReset(String deviceId, SocketChannel channel) {
        String channelId = channel.id().asLongText();

    }

    public void dealWithChangeIpOrPort(String deviceId, SocketChannel channel) {
        //1,confirm whether there is already channel in memory
        String newChannelId = channel.id().asLongText();
        String oldChannelId = "";
        if (deviceIdAndChannelMap.containsKey(deviceId)) {
            //is channel exist ,close it and drop it
            SocketChannel oldChannel = deviceIdAndChannelMap.get(deviceId);
            oldChannelId = oldChannel.id().asLongText();
            if (!newChannelId.equals(oldChannelId)) {
                log.warn(new KingMeterMarker("Socket,ReLogin,1004"),
                        "{}|0|{}|{}|{}|{}", deviceId,
                        newChannelId, oldChannelId,
                        channel.remoteAddress(), oldChannel.remoteAddress());
                oldChannel.pipeline().remove(LengthFieldBasedFrameDecoder.class);
                oldChannel.pipeline().remove(IdleStateHandler.class);
                oldChannel.pipeline().remove(AcceptorIdleStateTrigger.class);
                oldChannel.pipeline().remove(KMDecoder.class);
                oldChannel.pipeline().remove(Encoder.class);
                oldChannel.pipeline().remove(KMServerHandler.class);
                oldChannel.pipeline().deregister();
                oldChannel.deregister();

                channelIdAndChannelMap.remove(oldChannelId);
                channelIdAndDeviceIdMap.remove(oldChannelId);
            }
        }else{
            log.warn(new KingMeterMarker("Socket,ReLogin,1006"),
                    "{}|0|{}|{}|{}", deviceId,
                    newChannelId, oldChannelId, channel.remoteAddress());
        }
        channelIdAndChannelMap.put(newChannelId, channel);
        channelIdAndDeviceIdMap.put(newChannelId, deviceId);
        deviceIdAndChannelMap.put(deviceId, channel);

        channel.attr(AttributeKey.<Long>valueOf("DeviceId")).set(Long.parseLong(deviceId));
    }

    //only remove channel info
    public void dealWithChannelInactive(SocketChannel channel) {
        String channelId = channel.id().asLongText();
        channelIdAndChannelMap.remove(channelId);
        String deviceId = channelIdAndDeviceIdMap.getOrDefault(channelId, null);
        if (deviceId != null) {
            channelIdAndDeviceIdMap.remove(channelId);
            Channel channelInMem = deviceIdAndChannelMap.getOrDefault(deviceId, null);
            if (channelInMem != null) {
                String channelIdInMem = channelInMem.id().asLongText();
                if (channelIdInMem.equals(channelId)) {
                    deviceIdAndChannelMap.remove(deviceId);
                }
            }
        }
    }


    public String dealWithOffLine(Channel channel) {
        String channelId = channel.id().asLongText();
        String deviceId = channelIdAndDeviceIdMap.getOrDefault(channelId, "0");

        channelIdAndChannelMap.remove(channelId);
        channelIdAndDeviceIdMap.remove(channelId);
        deviceIdAndChannelMap.remove(deviceId);

        String token = deviceIdAndTokenMap.getOrDefault(deviceId, null);
        if (token != null) {
            deviceIdAndTokenMap.remove(deviceId);
            tokenAndDeviceIdMap.remove(token);
            deviceIdAndTokenArrayMap.remove(deviceId);
        }
        deviceResultMap.remove(deviceId);

        deviceInfoMap.remove(Long.parseLong(deviceId));
        return deviceId;
    }

}
