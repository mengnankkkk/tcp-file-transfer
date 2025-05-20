package com.mengnankk.tcpfiletransfer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileInfo {
    private String filename;
    private LocalDateTime uploadTime;

    public FileInfo(String filename, LocalDateTime uploadTime) {
        this.filename = filename;
        this.uploadTime = uploadTime;
    }
}