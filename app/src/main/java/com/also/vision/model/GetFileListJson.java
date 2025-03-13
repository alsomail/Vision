package com.also.vision.model;

import java.util.List;

/**
 * 获取文件列表响应JSON
 */
public class GetFileListJson {
    private int msg_id;
    private int rval;
    private int total;
    private List<FileItem> files;
    
    public int getMsg_id() {
        return msg_id;
    }
    
    public void setMsg_id(int msg_id) {
        this.msg_id = msg_id;
    }
    
    public int getRval() {
        return rval;
    }
    
    public void setRval(int rval) {
        this.rval = rval;
    }
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public List<FileItem> getFiles() {
        return files;
    }
    
    public void setFiles(List<FileItem> files) {
        this.files = files;
    }
    
    /**
     * 文件项
     */
    public static class FileItem {
        private String name;
        private String path;
        private String url;
        private String thumb;
        private long size;
        private String time;
        private int type;
        private int duration;
        
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
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getThumb() {
            return thumb;
        }
        
        public void setThumb(String thumb) {
            this.thumb = thumb;
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
        
        public int getDuration() {
            return duration;
        }
        
        public void setDuration(int duration) {
            this.duration = duration;
        }
    }
} 