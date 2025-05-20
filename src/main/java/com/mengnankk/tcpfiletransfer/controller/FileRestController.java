package com.mengnankk.tcpfiletransfer.controller;

import com.mengnankk.tcpfiletransfer.factory.StorageServiceFactory;
import com.mengnankk.tcpfiletransfer.service.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileRestController {

    private static final Logger logger = LoggerFactory.getLogger(FileRestController.class);
    private final StorageService storageService;

    @Autowired
    public FileRestController(StorageServiceFactory storageServiceFactory) {
        this.storageService = storageServiceFactory.getStorageService();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "请选择要上传的文件");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String storedFilename = storageService.store(file);
            logger.info("文件上传成功: {} 保存为 {}", file.getOriginalFilename(), storedFilename);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件上传成功");
            response.put("filename", storedFilename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("文件上传失败: {}", file.getOriginalFilename(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}