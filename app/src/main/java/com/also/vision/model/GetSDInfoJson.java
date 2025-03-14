package com.also.vision.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 获取SD卡信息响应JSON
 */
public class GetSDInfoJson {
    @JSONField(name = "msg_id")
    private int msgId;
    
    @JSONField(name = "rval")
    private int result;
    
    @JSONField(name = "total")
    private long totalSpace;
    
    @JSONField(name = "free")
    private long freeSpace;
    
    @JSONField(name = "status")
    private int status;
    
    /**
     * 将JSON数据转换为SDCardInfo对象
     * @return SDCardInfo对象
     */
    public SDCardInfo getSdInfo() {
        SDCardInfo sdInfo = new SDCardInfo();
        sdInfo.setTotalSpace(totalSpace);
        sdInfo.setFreeSpace(freeSpace);
        sdInfo.setStatus(status);
        return sdInfo;
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
    
    public long getTotalSpace() {
        return totalSpace;
    }
    
    public void setTotalSpace(long totalSpace) {
        this.totalSpace = totalSpace;
    }
    
    public long getFreeSpace() {
        return freeSpace;
    }
    
    public void setFreeSpace(long freeSpace) {
        this.freeSpace = freeSpace;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
} 