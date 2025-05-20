package com.mengnankk.tcpfiletransfer.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    /**
     * 存储文件
     *
     * @param file MultipartFile
     * @return 文件名
     */
    String store(MultipartFile file);

    /**
     * 存储文件
     *
     * @param inputStream InputStream
     * @param originalFilename 原始文件名
     * @return 文件名
     */
    String store(InputStream inputStream, String originalFilename);

    /**
     * 加载文件为 Path
     *
     * @param filename 文件名
     * @return Path
     */
    Path load(String filename);

    /**
     * 加载文件为 Resource
     *
     * @param filename 文件名
     * @return Resource
     */
    Resource loadAsResource(String filename);

    /**
     * 删除文件
     *
     * @param filename 文件名
     */
    void delete(String filename);

    /**
     * 初始化存储。例如，创建根目录等。
     */
    void init();
}