package com.mengnankk.tcpfiletransfer.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.exception.StorageException;
import com.mengnankk.tcpfiletransfer.exception.StorageFileNotFoundException;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service("ossStorageService")
public class OssStorageService implements StorageService {

    private final FileStorageProperties properties;
    private OSS ossClient;

    @Autowired
    public OssStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initClient() {
        FileStorageProperties.Oss ossConfig = properties.getOss();
        if (ossConfig == null ||
                StringUtils.isEmpty(ossConfig.getEndpoint()) ||
                StringUtils.isEmpty(ossConfig.getAccessKeyId()) ||
                StringUtils.isEmpty(ossConfig.getAccessKeySecret()) ||
                StringUtils.isEmpty(ossConfig.getBucketName())) {
            // OSS is not configured, so we don't initialize the client.
            // This service will not be usable until configuration is provided.
            // Consider logging a warning or throwing an exception if OSS is the active profile.
            if ("oss".equalsIgnoreCase(properties.getType())) {
                throw new StorageException("OSS storage is selected but not properly configured.");
            }
            return;
        }
        this.ossClient = new OSSClientBuilder().build(ossConfig.getEndpoint(), ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
    }

    private void ensureClientInitialized() {
        if (this.ossClient == null) {
            // Attempt to initialize again, in case config was added dynamically (less common)
            initClient();
            if (this.ossClient == null) {
                 throw new StorageException("OSS client is not initialized. Please check OSS configuration.");
            }
        }
    }

    @Override
    public String store(MultipartFile file) {
        ensureClientInitialized();
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + (StringUtils.hasText(extension) ? "." + extension : "");

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(properties.getOss().getBucketName(), newFilename, inputStream);
            return newFilename;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + originalFilename + " to OSS", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename) {
        ensureClientInitialized();
        if (inputStream == null) {
            throw new StorageException("Input stream cannot be null.");
        }
        String cleanOriginalFilename = StringUtils.cleanPath(Objects.requireNonNull(originalFilename));
        String extension = StringUtils.getFilenameExtension(cleanOriginalFilename);
        String newFilename = UUID.randomUUID().toString() + (StringUtils.hasText(extension) ? "." + extension : "");

        try {
            ossClient.putObject(properties.getOss().getBucketName(), newFilename, inputStream);
            return newFilename;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + cleanOriginalFilename + " to OSS", e);
        }
    }

    @Override
    public Path load(String filename) {
        // OSS does not directly map to java.nio.Path in the same way local files do.
        // This method might need a different approach or be re-evaluated for OSS.
        // For now, throwing an UnsupportedOperationException or returning null.
        throw new UnsupportedOperationException("load(String filename) is not supported for OSS. Use loadAsResource.");
    }

    @Override
    public Resource loadAsResource(String filename) {
        ensureClientInitialized();
        try {
            OSSObject ossObject = ossClient.getObject(properties.getOss().getBucketName(), filename);
            if (ossObject == null) {
                throw new StorageFileNotFoundException("Could not find file: " + filename + " in OSS");
            }
            return new InputStreamResource(ossObject.getObjectContent()) {
                @Override
                public String getFilename() {
                    return filename;
                }

                @Override
                public long contentLength() {
                    return ossObject.getObjectMetadata().getContentLength();
                }
            };
        } catch (Exception e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename + " from OSS", e);
        }
    }

    @Override
    public void delete(String filename) {
        ensureClientInitialized();
        try {
            ossClient.deleteObject(properties.getOss().getBucketName(), filename);
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + filename + " from OSS", e);
        }
    }

    @Override
    public void init() {
        // For OSS, initialization might involve checking bucket existence or creating it if configured.
        // For simplicity, we assume the bucket already exists.
        // If client is null and type is oss, it means it's not configured.
        if ("oss".equalsIgnoreCase(properties.getType())){
            ensureClientInitialized();
            if (!ossClient.doesBucketExist(properties.getOss().getBucketName())){
                throw new StorageException("OSS Bucket '" + properties.getOss().getBucketName() + "' does not exist.");
            }
        }
    }
}