package com.mengnankk.tcpfiletransfer.service.impl;

import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.exception.StorageException;
import com.mengnankk.tcpfiletransfer.exception.StorageFileNotFoundException;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
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

@Service("minioStorageService")
public class MinioStorageService implements StorageService {

    private final FileStorageProperties properties;
    private MinioClient minioClient;

    @Autowired
    public MinioStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initClient() {
        FileStorageProperties.Minio minioConfig = properties.getMinio();
        if (minioConfig == null ||
                StringUtils.isEmpty(minioConfig.getEndpoint()) ||
                StringUtils.isEmpty(minioConfig.getAccessKey()) ||
                StringUtils.isEmpty(minioConfig.getSecretKey()) ||
                StringUtils.isEmpty(minioConfig.getBucketName())) {
            if ("minio".equalsIgnoreCase(properties.getType())) {
                throw new StorageException("MinIO storage is selected but not properly configured.");
            }
            return;
        }
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();
        } catch (Exception e) {
            throw new StorageException("Failed to initialize MinIO client", e);
        }
    }

    private void ensureClientInitialized() {
        if (this.minioClient == null) {
            initClient();
            if (this.minioClient == null) {
                throw new StorageException("MinIO client is not initialized. Please check MinIO configuration.");
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
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(properties.getMinio().getBucketName()).object(newFilename)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return newFilename;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + originalFilename + " to MinIO", e);
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
            // For InputStream, size is unknown, so pass -1 for partSize
            // contentType might need to be determined or passed as a parameter if important
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(properties.getMinio().getBucketName()).object(newFilename)
                            .stream(inputStream, -1, 1024 * 1024 * 5) // 5MB part size, adjust as needed
                            .build());
            return newFilename;
        } catch (Exception e) {
            throw new StorageException("Failed to store file " + cleanOriginalFilename + " to MinIO", e);
        }
    }

    @Override
    public Path load(String filename) {
        throw new UnsupportedOperationException("load(String filename) is not supported for MinIO. Use loadAsResource.");
    }

    @Override
    public Resource loadAsResource(String filename) {
        ensureClientInitialized();
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getMinio().getBucketName())
                            .object(filename)
                            .build());
            return new InputStreamResource(stream) {
                @Override
                public String getFilename() {
                    return filename;
                }
                // MinIO GetObject does not directly provide content length without a separate StatObject call
                // For simplicity, we are not fetching it here. It can be added if needed.
            };
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                 throw new StorageFileNotFoundException("Could not find file: " + filename + " in MinIO", e);
            }
            throw new StorageFileNotFoundException("Could not read file: " + filename + " from MinIO", e);
        } catch (Exception e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename + " from MinIO", e);
        }
    }

    @Override
    public void delete(String filename) {
        ensureClientInitialized();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(properties.getMinio().getBucketName()).object(filename).build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + filename + " from MinIO", e);
        }
    }

    @Override
    public void init() {
        // For MinIO, initialization might involve checking bucket existence or creating it.
        // For simplicity, we assume the bucket already exists.
        if ("minio".equalsIgnoreCase(properties.getType())){
            ensureClientInitialized();
            // You might want to add a check for bucket existence here if needed
            // try {
            //     boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.getMinio().getBucketName()).build());
            //     if (!found) {
            //         throw new StorageException("MinIO Bucket '" + properties.getMinio().getBucketName() + "' does not exist.");
            //     }
            // } catch (Exception e) {
            //     throw new StorageException("Could not check MinIO bucket existence", e);
            // }
        }
    }
}