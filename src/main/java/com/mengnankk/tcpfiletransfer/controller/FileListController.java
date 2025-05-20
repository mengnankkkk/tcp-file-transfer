package com.mengnankk.tcpfiletransfer.controller;

import com.mengnankk.tcpfiletransfer.model.FileInfo;
import com.mengnankk.tcpfiletransfer.service.FileListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileListController {

    private final FileListService fileListService;

    @Autowired
    public FileListController(FileListService fileListService) {
        this.fileListService = fileListService;
    }

    @GetMapping("/list")
    public String listFiles(Model model) {
        List<FileInfo> files = fileListService.listFiles();
        
        // 数据验证和格式化
        if (files != null) {
            files.forEach(file -> {
                if (file.getFilename() == null) {
                    file.setFilename("未知文件");
                }
                if (file.getUploadTime() == null) {
                    file.setUploadTime(LocalDateTime.now());
                }
            });
        }
        
        model.addAttribute("files", files);
        return "fileList";
    }
    
    @GetMapping(value = "/list/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FileInfo> listFilesJson() {
        return fileListService.listFiles();
    }

    private static final Logger logger = LoggerFactory.getLogger(FileListController.class);
    // 日志语句已移动到相应方法内部
}