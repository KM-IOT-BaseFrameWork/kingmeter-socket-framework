package com.kingmeter.socket.framework.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: crazyandy
 */
@Data
@Configuration
public class LoggerConfig {

    @Value("${log.message:true}")
    private boolean log_message = true;

    @Value("${log.model:true}")
    private boolean log_model = true;

    @Value("${log.business:true}")
    private boolean log_business = true;

    @Value("${log.exception:true}")
    private boolean log_exception = true;

}
