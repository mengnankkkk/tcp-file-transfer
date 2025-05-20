package com.mengnankk.tcpfiletransfer.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "tencent")
public class TencentCloudConfig {

    @Value("${file-storage.tencent.secret-id}")
    private String secretId;

    @Value("${file-storage.tencent.secret-key}")
    private String secretKey;

    @Value("${file-storage.tencent.region}")
    private String region;

    @Bean
    public COSClient cosClient() {
        // 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket所在的区域
        Region cosRegion = new Region(region);
        ClientConfig clientConfig = new ClientConfig(cosRegion);
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}