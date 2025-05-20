package com.mengnankk.tcpfiletransfer.service;

import com.mengnankk.tcpfiletransfer.model.FileInfo;
import java.util.List;

public interface FileListService {
    /**
     * 获取所有已上传文件的信息列表
     *
     * @return 文件信息列表
     */
    List<FileInfo> listFiles();
}