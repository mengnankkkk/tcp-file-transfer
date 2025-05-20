package com.mengnankk.tcpfiletransfer.service.impl;

import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.exception.StorageException;
import com.mengnankk.tcpfiletransfer.exception.StorageFileNotFoundException;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private final Path rootLocation;
    private final FileStorageProperties properties;

    @Autowired
    public LocalStorageService(FileStorageProperties properties) {
        this.properties = properties;
        if (properties.getLocal() == null || StringUtils.isEmpty(properties.getLocal().getUploadPath())) {
            throw new StorageException("Local storage upload path is not configured.");
        }
        this.rootLocation = Paths.get(properties.getLocal().getUploadPath());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + (StringUtils.hasText(extension) ? "." + extension : "");

        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFilename)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check
                throw new StorageException("Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return newFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename) {
        if (inputStream == null) {
            throw new StorageException("Input stream cannot be null.");
        }
        String cleanOriginalFilename = StringUtils.cleanPath(Objects.requireNonNull(originalFilename));
        String extension = StringUtils.getFilenameExtension(cleanOriginalFilename);
        String newFilename = UUID.randomUUID().toString() + (StringUtils.hasText(extension) ? "." + extension : "");

        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFilename)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside current directory.");
            }
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return newFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + cleanOriginalFilename, e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + filename, e);
        }
    }

    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }
}