package com.also.vision.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 拍照响应JSON
 */
public class TakePhotoJson {
    @JSONField(name = "msg_id")
    private int msgId;
    
    @JSONField(name = "rval")
    private int result;
    
    @JSONField(name = "url")
    private String url;
    
    @JSONField(name = "thumb_url")
    private String thumbnailUrl;
    
    @JSONField(name = "type")
    private int fileType;
    
    public TakePhotoJson() {
    }
    
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
    
    public int getFileType() {
        return fileType;
    }
    
    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
} 