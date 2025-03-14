package com.also.vision.model;

import java.util.Date;

/**
 * 设备文件模型类
 * 用于表示设备上的视频和图片文件
 */
public class DeviceFile {
    // 文件类型常量
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_PHOTO = 2;
    
    private String fileName;      // 文件名
    private String filePath;      // 文件路径
    private String thumbnailUrl;  // 缩略图URL
    private String fileUrl;       // 文件URL
    private long fileSize;        // 文件大小(字节)
    private String createTime;    // 创建时间
    private int fileType;         // 文件类型(1=视频, 2=图片)
    private int duration;         // 视频时长(秒)，仅对视频有效
    
    public DeviceFile() {
    }
    
    public DeviceFile(String fileName, String filePath, String thumbnailUrl, String fileUrl, 
                     long fileSize, String createTime, int fileType) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.thumbnailUrl = thumbnailUrl;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.createTime = createTime;
        this.fileType = fileType;
    }
    
    // Getters and Setters
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public int getFileType() {
        return fileType;
    }
    
    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    @Override
    public String toString() {
        return "DeviceFile{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", createTime='" + createTime + '\'' +
                ", fileType=" + fileType +
                ", fileUrl='" + fileUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                '}';
    }
} 