package com.kingmeter.socket.framework.dto;

import lombok.Data;

@Data
public class DeviceLoginSuperDto {
    private String deviceId;

    public DeviceLoginSuperDto(){}

    public DeviceLoginSuperDto(String[] array){
        this.deviceId = array[0];
    }
}
