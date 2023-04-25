package com.kingmeter.socket.framework.application;


import com.kingmeter.common.KingMeterException;
import com.kingmeter.common.ResponseCode;
import com.kingmeter.socket.framework.util.CacheUtil;

import java.util.HashMap;
import java.util.Map;

public class ResultFromDeviceImpl implements ResultFromDevice{
    @Override
    public Map<String, String> getResult(String key,int waitSeconds){
//        for (int i = 0; i < waitSeconds*2-1; i++) {
//            try {
//                Thread.sleep(500l);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            Map<String, String> result = CacheUtil.getInstance().getDeviceResultMap().getOrDefault(key,null);
//            if (result != null){
//                CacheUtil.getInstance().getDeviceResultMap().remove(key);
//                return result;
//            }
////            if (CacheUtil.getInstance().getDeviceResultMap().containsKey(key)) {
////                Map<String, String> result = CacheUtil.getInstance().getDeviceResultMap().get(key);
////                CacheUtil.getInstance().getDeviceResultMap().remove(key);
////                return result;
////            }
//        }
//        CacheUtil.getInstance().getDeviceResultMap().remove(key);
        throw new KingMeterException(ResponseCode.Device_Not_Response);
    }
}
