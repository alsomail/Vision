package com.also.vision.model;

/**
 * 拍照响应的JSON模型类
 */
public class TakePhotoJson {
    private int msg_id;
    private int rval;
    private String param;
    private String type;
    private int fileType;
    private String url;
    private String thumbnailUrl;
    
    public TakePhotoJson() {
    }
    
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
    
    public String getParam() {
        return param;
    }
    
    public void setParam(String param) {
        this.param = param;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getFileType() {
        return fileType;
    }
    
    public void setFileType(int fileType) {
        this.fileType = fileType;
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