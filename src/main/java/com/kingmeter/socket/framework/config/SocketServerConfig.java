package com.kingmeter.socket.framework.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SocketServerConfig {

    @Value("${socket.port}")
    private int socketPort;

    @Value("${device.loginFunctionCode}")
    private int loginFunctionCode;

    @Value("${socket.waitSeconds:30}")
    private int waitSeconds;

    @Value("${socket.readIdleTimeLimit}")
    private int readIdleTimeLimit;

    @Value("${socket.writeIdleTimeLimit}")
    private int writeIdleTimeLimit;

    @Value("${socket.allIdleTimeLimit}")
    private int allIdleTimeLimit;


    @Value("${socket.bossThreads:0}")
    private int bossThreads = 0; // 0 = current_processors_amount * 2

    @Value("${socket.workerThreads:0}")
    private int workerThreads = 0; // 0 = current_processors_amount * 2


    @Value("${socket.useLinuxNativeEpoll:true}")
    private boolean useLinuxNativeEpoll = true;

    @Value("${socket.tcpNoDelay:true}")
    private boolean tcpNoDelay = true;

    @Value("${socket.tcpSendBufferSize:-1}")
    private int tcpSendBufferSize = -1;

    @Value("${socket.tcpReceiveBufferSize:-1}")
    private int tcpReceiveBufferSize = -1;

    @Value("${socket.tcpKeepAlive:true}")
    private boolean tcpKeepAlive = true;

    @Value("${socket.soLinger:-1}")
    private int soLinger = -1;

    @Value("${socket.reuseAddress:true}")
    private boolean reuseAddress = true;

    @Value("${socket.acceptBackLog:1024}")
    private int acceptBackLog = 1024;
}
