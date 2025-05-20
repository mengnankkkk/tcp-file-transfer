package com.mengnankk.tcpfiletransfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tcp")
public class TcpServerProperties {

    private int port;
    // 可以根据需要添加其他TCP相关配置，例如超时时间、线程池大小等

}