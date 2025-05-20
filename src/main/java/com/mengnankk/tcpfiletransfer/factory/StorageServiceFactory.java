package com.mengnankk.tcpfiletransfer.factory;

import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StorageServiceFactory {

    private final FileStorageProperties fileStorageProperties;
    private final ApplicationContext applicationContext;
    private StorageService storageService;

    @Autowired
    public StorageServiceFactory(FileStorageProperties fileStorageProperties, ApplicationContext applicationContext) {
        this.fileStorageProperties = fileStorageProperties;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        String type = fileStorageProperties.getType();
        if ("oss".equalsIgnoreCase(type)) {
            this.storageService = applicationContext.getBean("ossStorageService", StorageService.class);
        } else if ("minio".equalsIgnoreCase(type)) {
            this.storageService = applicationContext.getBean("minioStorageService", StorageService.class);
        } else if ("local".equalsIgnoreCase(type)) {
            this.storageService = applicationContext.getBean("localStorageService", StorageService.class);
        } else if ("tencent".equalsIgnoreCase(type)) {
            this.storageService = applicationContext.getBean("tencentCloudStorageService", StorageService.class);
        }
        else {
            throw new IllegalArgumentException("Invalid storage type: " + type + ". Supported types are: local, oss, minio.");
        }
        // Initialize the selected storage service
        if (this.storageService != null) {
            this.storageService.init();
        }
    }

    public StorageService getStorageService() {
        if (storageService == null) {
            // This might happen if the configuration was invalid at startup and init() failed silently for some reason,
            // or if the type was changed dynamically and not re-initialized (which is not supported by this simple factory).
            // Re-attempting initialization or throwing a more specific error might be needed depending on desired behavior.
            init(); // Try to initialize again
            if (storageService == null) {
                 throw new IllegalStateException("StorageService is not initialized. Check configuration and logs.");
            }
        }
        return storageService;
    }
}