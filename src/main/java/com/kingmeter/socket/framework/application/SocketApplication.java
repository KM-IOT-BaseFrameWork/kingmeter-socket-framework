package com.kingmeter.socket.framework.application;


import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.config.HeaderCode;
import com.kingmeter.socket.framework.config.SocketServerConfig;
import com.kingmeter.socket.framework.dto.ResponseBody;
import com.kingmeter.socket.framework.util.CacheUtil;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SocketApplication {

    @Autowired
    private HeaderCode headerCode;

    @Autowired
    private SocketServerConfig config;

    /**
     * send tcp message to hardware
     *
     * @param deviceId
     * @param functionCodeArray
     * @param obj
     */
    public void sendSocketMsg(long deviceId,
                              byte[] functionCodeArray,
                              Object obj) {

        byte[] tokenArray = getTokenFromCache(String.valueOf(deviceId));

        SocketChannel channel = getChannelByDeviceId(String.valueOf(deviceId));

        sendSocketMsg(deviceId, tokenArray, channel,
                functionCodeArray, obj);
    }

    public void sendSocketMsg(long deviceId,
                              byte[] tokenArray, SocketChannel channel,
                              byte[] functionCodeArray,
                              Object obj) {
        channel.writeAndFlush(createResponseBody(deviceId, tokenArray, functionCodeArray, obj));
    }

    public void sendSocketMsg(String deviceId, List<ResponseBody> array) {
        SocketChannel channel = getChannelByDeviceId(deviceId);
        log.info("~~~~~~~~~~~~1~~prepare stick package now {}",deviceId);
        for (ResponseBody body : array) {
            channel.write(body);
        }
        log.info("~~~~~~~~~~~~2~~write stick package now {}",deviceId);
        channel.flush();
        log.info("~~~~~~~~~~~~3~~write stick package end {}",deviceId);
    }

    public ResponseBody createResponseBody(long deviceId,
                                           byte[] functionCodeArray,
                                           Object obj) {
        byte[] tokenArray = getTokenFromCache(String.valueOf(deviceId));
        return createResponseBody(deviceId, tokenArray, functionCodeArray, obj);
    }

    public ResponseBody createResponseBody(long deviceId,
                                           byte[] tokenArray,
                                           byte[] functionCodeArray,
                                           Object obj) {
        ResponseBody responseBody = new ResponseBody();
        responseBody.setTokenArray(tokenArray);
        responseBody.setFunctionCodeArray(functionCodeArray);
        responseBody.setData(obj != null ? obj.toString() : "");
        responseBody.setSTART_CODE_1(headerCode.getSTART_CODE_1());
        responseBody.setSTART_CODE_2(headerCode.getSTART_CODE_2());
        responseBody.setEND_CODE_1(headerCode.getEND_CODE_1());
        responseBody.setEND_CODE_2(headerCode.getEND_CODE_2());

        responseBody.setToken_length(headerCode.getTOKEN_LENGTH());
        responseBody.setDeviceId(deviceId);
        return responseBody;
    }

    public Map<String, String> waitForMapResult(String key) {
        return waitForMapResult(key, config.getWaitSeconds());
    }

    public Map<String, String> waitForMapResult(String key, int waitSeconds) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        SimpleTimeLimiter limiter = SimpleTimeLimiter.create(executor);
        ResultFromDevice result = new ResultFromDeviceImpl();
//        ResultFromDevice proxy = limiter.newProxy(result, ResultFromDevice.class, waitSeconds, TimeUnit.SECONDS);
        return result.getResult(key, waitSeconds);
    }


    public byte[] getTokenFromCache(String deviceId) {
        if (CacheUtil.getInstance().getDeviceIdAndTokenArrayMap().containsKey(deviceId)) {
            return CacheUtil.getInstance().getDeviceIdAndTokenArrayMap().get(deviceId);
        } else {
            throw new KingMeterException(ResponseCode.Device_Not_Logon);
        }
    }

    public SocketChannel getChannelByDeviceId(String deviceId) {
        if (CacheUtil.getInstance().getDeviceIdAndChannelMap().containsKey(deviceId)) {
            return CacheUtil.getInstance().getDeviceIdAndChannelMap().get(deviceId);
        } else {
            throw new KingMeterException(ResponseCode.Device_Not_Logon);
        }
    }


    public void setHeartBeatIdleTime(long deviceId, long heartInterval) {
        SocketChannel channel = getChannelByDeviceId(String.valueOf(deviceId));
        channel.pipeline().remove(IdleStateHandler.class);
        channel.pipeline().addBefore("idleTrigger",
                "idleHandler", new
                        IdleStateHandler(heartInterval * 3, 0, 0,
                        TimeUnit.SECONDS));
    }

}
