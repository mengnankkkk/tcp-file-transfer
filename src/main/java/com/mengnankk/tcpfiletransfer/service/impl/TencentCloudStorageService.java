package com.mengnankk.tcpfiletransfer.service.impl;

import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.exception.StorageException;
import com.mengnankk.tcpfiletransfer.exception.StorageFileNotFoundException;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.Path;

@Service("tencentCloudStorageService")
public class TencentCloudStorageService implements StorageService {

    private final COSClient cosClient;
    private final FileStorageProperties properties;

    @Value("${file-storage.tencent.bucket-name}")
    private String bucketName;

    @Autowired
    public TencentCloudStorageService(COSClient cosClient, FileStorageProperties properties) {
        this.cosClient = cosClient;
        this.properties = properties;
    }

    @Override
    public void init() {
        if ("tencent".equalsIgnoreCase(properties.getType())) {
            ensureClientInitialized();
            if (!cosClient.doesBucketExist(bucketName)) {
                throw new StorageException("COS Bucket '" + bucketName + "' does not exist.");
            }
        }
    }

    private void ensureClientInitialized() {
        if (this.cosClient == null) {
            throw new StorageException("COS client is not initialized. Please check COS configuration.");
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, newFilename, inputStream, metadata);
            cosClient.putObject(putObjectRequest);
            return newFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + originalFilename + " to COS", e);
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(inputStream.available());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, newFilename, inputStream, metadata);
            cosClient.putObject(putObjectRequest);
            return newFilename;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + cleanOriginalFilename + " to COS", e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        ensureClientInitialized();
        try {
            COSObject cosObject = cosClient.getObject(bucketName, filename);
            if (cosObject == null) {
                throw new StorageFileNotFoundException("Could not find file: " + filename + " in COS");
            }
            return new InputStreamResource(cosObject.getObjectContent()) {
                @Override
                public String getFilename() {
                    return filename;
                }

                @Override
                public long contentLength() {
                    return cosObject.getObjectMetadata().getContentLength();
                }
            };
        } catch (Exception e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename + " from COS", e);
        }
    }

    @Override
    public Path load(String filename) {
        throw new UnsupportedOperationException("load not supported for COS.");
    }

    @Override
    public void delete(String filename) {
        ensureClientInitialized();
        try {
            cosClient.deleteObject(bucketName, filename);
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + filename + " from COS", e);
        }
    }
}
