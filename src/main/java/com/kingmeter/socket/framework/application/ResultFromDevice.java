package com.kingmeter.socket.framework.application;

import java.util.Map;

public interface ResultFromDevice {
    Map<String, String> getResult(String key, int waitSeconds);
}
