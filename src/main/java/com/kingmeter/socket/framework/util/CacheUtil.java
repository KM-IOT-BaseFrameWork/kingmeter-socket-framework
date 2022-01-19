package com.kingmeter.socket.framework.util;

import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.KingMeterMarker;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.utils.StringUtil;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
@Slf4j
public class CacheUtil {

    private volatile static CacheUtil instance;

    private CacheUtil() {
    }

    public static CacheUtil getInstance() {
        if (instance == null) {
            synchronized (CacheUtil.class) {
                if (instance == null) {
                    instance = new CacheUtil();
                }
            }
        }
        return instance;
    }


    /**
     * key: channel id
     * value : channel
     */
    private volatile ConcurrentMap<String, SocketChannel> channelIdAndChannelMap = new ConcurrentHashMap();

    /**
     * key: channel id
     * value : device id
     */
    private volatile ConcurrentMap<String, String> channelIdAndDeviceIdMap = new ConcurrentHashMap();

    /**
     * key: device id
     * value : channel
     */
    private volatile ConcurrentMap<String, SocketChannel> deviceIdAndChannelMap = new ConcurrentHashMap();

    /**
     * key: deviceId
     * value: token
     */
    private volatile ConcurrentMap<String, String> deviceIdAndTokenMap = new ConcurrentHashMap();

    /**
     * key: deviceId
     * value: token
     */
    private volatile ConcurrentMap<String, byte[]> deviceIdAndTokenArrayMap = new ConcurrentHashMap();


    /**
     * key : token
     * value : deviceId
     */
    private volatile ConcurrentMap<String, String> tokenAndDeviceIdMap = new ConcurrentHashMap();


    /**
     * remote unlock result
     * <p>
     * key: {lockId}_{userId} ,value : stu
     */
    private volatile ConcurrentMap<String, Integer> scanUnlockResultMap = new ConcurrentHashMap();

    /**
     * get info from device
     * key {lockId}_{database} ,value : map
     */
    private volatile ConcurrentMap<String, Map<String, String>> deviceResultMap = new ConcurrentHashMap();

    /**
     * key {lockId} , value :map
     * the information of the device
     */
    private volatile ConcurrentMap<Long, Map<String, String>> deviceInfoMap = new ConcurrentHashMap();


    public String validateTokenAndGetDeviceIdExceptLogin(
            int functionCode, String token, byte[] msg, SocketChannel newChannel) {

        String deviceId = tokenAndDeviceIdMap.getOrDefault(token, "0");
        String newChannelId = newChannel.id().asLongText();

        if (channelIdAndChannelMap.containsKey(newChannelId)) {
            //this show us that this channel is created before,we would validate the token here
            String oldToken = deviceIdAndTokenMap.getOrDefault(deviceId, "");

            if (StringUtil.isNotEmpty(oldToken)) {
                if (token.equals(oldToken)) {
                    return deviceId;
                } else {
                    log.info(new KingMeterMarker("Socket,ReLogin,1001"),
                            "{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId, msg);
                    newChannel.close();
                    throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
                }
            } else {
                log.info(new KingMeterMarker("Socket,ReLogin,1002"),
                        "{}|{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId, msg, token);

                newChannel.close();
                throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
            }
        } else {
            /**
             * perhaps the client change another port to connect to the server this time
             * we should validate the token ,if the token exist in memory , we should update the channelId for this device
             */
            if (tokenAndDeviceIdMap.containsKey(token)) {
                dealWithChangeIpOrPort(functionCode,deviceId, token, newChannel);
                return deviceId;
            } else {
                /**
                 * the token does not exist in memory , so we think it's invalid , we should close this connection right now.
                 */
                log.info(new KingMeterMarker("Socket,ReLogin,1003"),
                        "{}|{}|{}|{}", deviceId, Integer.toHexString(functionCode), newChannelId, msg);
                newChannel.close();
                throw new KingMeterException(ResponseCode.Device_Token_Not_Correct);
            }
        }
    }

    public void dealWithLoginSucceed(String deviceId, String token,
                                     byte[] tokenArray, SocketChannel channel) {
        //1,confirm whether there is already channel in memory
        String newChannelId = channel.id().asLongText();
        String oldChannelId = "";
        if (deviceIdAndChannelMap.containsKey(deviceId)) {
            //is channel exist ,close it and drop it
            SocketChannel oldChannel = deviceIdAndChannelMap.get(deviceId);
            oldChannelId = oldChannel.id().asLongText();
            if (!newChannelId.equals(oldChannelId)) {
                log.info(new KingMeterMarker("Socket,Login,1001"),
                        "{}|{}|{}|{}|{}", deviceId, newChannelId, oldChannelId,
                        channel.remoteAddress(), oldChannel.remoteAddress());

                oldChannel.pipeline().deregister();
                oldChannel.deregister();
                dealWithOffLine(oldChannel, deviceId, false);
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

//        log.info(new KingMeterMarker("Socket,Login,1000"),
//                "{}|{}|{}|{}|{}", deviceId, newChannelId, oldChannelId,
//                token, oldToken);

        channel.attr(AttributeKey.<Long>valueOf("DeviceId")).set(Long.parseLong(deviceId));
    }


    private void dealWithChangeIpOrPort(int functionCode,String deviceId, String token, SocketChannel channel) {
        //1,confirm whether there is already channel in memory
        String newChannelId = channel.id().asLongText();
        String oldChannelId = "";
        if (deviceIdAndChannelMap.containsKey(deviceId)) {
            //is channel exist ,close it and drop it
            SocketChannel oldChannel = deviceIdAndChannelMap.get(deviceId);
            oldChannelId = oldChannel.id().asLongText();
            if (!newChannelId.equals(oldChannelId)) {
                log.info(new KingMeterMarker("Socket,ReLogin,1004"),
                        "{}|{}|{}|{}|{}|{}", deviceId,Integer.toHexString(functionCode),
                        newChannelId, oldChannelId,
                        channel.remoteAddress(), oldChannel.remoteAddress());

                oldChannel.pipeline().deregister();
                oldChannel.deregister();

                channelIdAndChannelMap.remove(oldChannelId);
                channelIdAndDeviceIdMap.remove(oldChannelId);
            }
        }
        channelIdAndChannelMap.put(newChannelId, channel);
        channelIdAndDeviceIdMap.put(newChannelId, deviceId);
        deviceIdAndChannelMap.put(deviceId, channel);

        log.info(new KingMeterMarker("Socket,ReLogin,1006"),
                "{}|{}|{}|{}|{}", deviceId,Integer.toHexString(functionCode),
                newChannelId, oldChannelId, token);

        channel.attr(AttributeKey.<Long>valueOf("DeviceId")).set(Long.parseLong(deviceId));
    }

    public String dealWithOffLine(SocketChannel channel, String deviceId) {
        return dealWithOffLine(channel, deviceId, true);
    }


    public String dealWithOffLine(Channel channel, String deviceId, boolean deleteDeviceInfoFlag) {
        String channelId = channel.id().asLongText();

        if (channelIdAndDeviceIdMap.containsKey(channelId)) {
            deviceId = channelIdAndDeviceIdMap.getOrDefault(channelId, "");

            if (StringUtil.isNotEmpty(deviceId)) {
                log.info(new KingMeterMarker("Socket,DeviceOffline,1001"),
                        "{}|{}", deviceId, channelId);
                deviceIdAndChannelMap.remove(deviceId);
            } else {
                log.info(new KingMeterMarker("Socket,DeviceOffline,1002"),
                        "{}|{}", 0, channelId);
            }
        } else {
            log.info(new KingMeterMarker("Socket,DeviceOffline,1003"),
                    "{}|{}", deviceId, channelId);
        }
        channelIdAndChannelMap.remove(channelId);
        channelIdAndDeviceIdMap.remove(channelId);
        if (deleteDeviceInfoFlag) {
            if (StringUtil.isNoneEmpty(deviceId) &&
                    !deviceId.equals("0")) deleteDeviceInfoInCache(deviceId);
        }
        return deviceId;
    }


    private void deleteDeviceInfoInCache(String deviceId) {
        if (deviceIdAndTokenMap.containsKey(deviceId)) {
            String token = deviceIdAndTokenMap.get(deviceId);
            tokenAndDeviceIdMap.remove(token);
            deviceIdAndTokenMap.remove(deviceId);
            deviceIdAndTokenArrayMap.remove(deviceId);
        }
        deviceIdAndChannelMap.remove(deviceId);
        deviceInfoMap.remove(Long.valueOf(deviceId));
    }

}
