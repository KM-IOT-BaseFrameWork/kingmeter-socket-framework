package com.kingmeter.socket.framework.config;

import java.util.HashMap;
import java.util.Map;

public enum ValidateHeaderErrorCodeType {
    StartCodeErrorType(1),
    EndCodeErrorType(2),
    CRC16CodeErrorType(3),
    PositionOverLengthErrorType(4);
    private int value;
    ValidateHeaderErrorCodeType(int value){
        this.value = value;
    }
    public int value(){
        return value;
    }

    static Map<Integer, ValidateHeaderErrorCodeType> enumMap=new HashMap();
    static{
        for(ValidateHeaderErrorCodeType type: ValidateHeaderErrorCodeType.values()){
            enumMap.put(type.value(), type);
        }
    }
    public static ValidateHeaderErrorCodeType getEnum(int value){
        return enumMap.get(value);
    }
    public static boolean containsValue(int value){
        return enumMap.containsKey(value);
    }


}
