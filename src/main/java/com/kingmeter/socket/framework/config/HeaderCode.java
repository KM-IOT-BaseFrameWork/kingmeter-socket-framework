package com.kingmeter.socket.framework.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class HeaderCode {
    @Value("${socket.start_code_1}")
    private byte START_CODE_1;
    @Value("${socket.start_code_2}")
    private byte START_CODE_2;
    @Value("${socket.end_code_1}")
    private byte END_CODE_1;
    @Value("${socket.end_code_2}")
    private byte END_CODE_2;
    @Value("${socket.token_length}")
    private int TOKEN_LENGTH;
}
