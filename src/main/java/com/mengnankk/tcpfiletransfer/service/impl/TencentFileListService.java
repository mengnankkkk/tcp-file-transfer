package com.mengnankk.tcpfiletransfer.service.impl;

import com.mengnankk.tcpfiletransfer.config.FileStorageProperties;
import com.mengnankk.tcpfiletransfer.model.FileInfo;
import com.mengnankk.tcpfiletransfer.service.FileListService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "tencent")
public class TencentFileListService implements FileListService {

    private final COSClient cosClient;
    private final FileStorageProperties properties;

    @Autowired
    public TencentFileListService(COSClient cosClient, FileStorageProperties properties) {
        this.cosClient = cosClient;
        this.properties = properties;
    }

    @Override
    public List<FileInfo> listFiles() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
        listObjectsRequest.setBucketName(properties.getTencent().getBucketName());
        
        ObjectListing objectListing = cosClient.listObjects(listObjectsRequest);
        
        return objectListing.getObjectSummaries().stream()
                .map(this::convertToFileInfo)
                .collect(Collectors.toList());
    }

    private FileInfo convertToFileInfo(COSObjectSummary summary) {
        return new FileInfo(
                summary.getKey(),
                LocalDateTime.ofInstant(
                        summary.getLastModified().toInstant(),
                        ZoneId.systemDefault()
                )
        );
    }
}