package com.mengnankk.tcpfiletransfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    private String type;
    private Local local;
    private Oss oss;
    private Minio minio;

    @Data
    public static class Local {
        private String uploadPath;
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
    }

    @Data
    public static class Tencent {
        private String bucketName;
        private String region;
        private String secretId;
        private String secretKey;
    }

    public Tencent getTencent() {
        return tencent;
    }

    private Tencent tencent;
}