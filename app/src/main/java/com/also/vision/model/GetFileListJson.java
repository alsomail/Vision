package com.also.vision.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取文件列表响应JSON
 */
public class GetFileListJson {
    @JSONField(name = "msg_id")
    private int msgId;
    
    @JSONField(name = "rval")
    private int result;
    
    @JSONField(name = "total")
    private int total;
    
    @JSONField(name = "offset")
    private int offset;
    
    @JSONField(name = "count")
    private int count;
    
    @JSONField(name = "files")
    private List<FileJson> files;
    
    /**
     * 将JSON数据转换为DeviceFile列表
     * @return DeviceFile列表
     */
    public List<DeviceFile> getFileList() {
        List<DeviceFile> fileList = new ArrayList<>();
        if (files != null) {
            for (FileJson fileJson : files) {
                DeviceFile file = new DeviceFile();
                file.setFileName(fileJson.getName());
                file.setFileSize(fileJson.getSize());
                file.setFileTime(fileJson.getTime());
                file.setFileType(fileJson.getType());
                file.setFileUrl(fileJson.getUrl());
                file.setThumbnailUrl(fileJson.getThumbnailUrl());
                fileList.add(file);
            }
        }
        return fileList;
    }
    
    // Getters and Setters
    public int getMsgId() {
        return msgId;
    }
    
    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }
    
    public int getResult() {
        return result;
    }
    
    public void setResult(int result) {
        this.result = result;
    }
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public List<FileJson> getFiles() {
        return files;
    }
    
    public void setFiles(List<FileJson> files) {
        this.files = files;
    }
    
    /**
     * 文件JSON对象
     */
    public static class FileJson {
        @JSONField(name = "name")
        private String name;
        
        @JSONField(name = "path")
        private String path;
        
        @JSONField(name = "size")
        private long size;
        
        @JSONField(name = "time")
        private String time;
        
        @JSONField(name = "type")
        private int type;
        
        @JSONField(name = "url")
        private String url;
        
        @JSONField(name = "thumb_url")
        private String thumbnailUrl;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
        
        public int getType() {
            return type;
        }
        
        public void setType(int type) {
            this.type = type;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
        
        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }
} 