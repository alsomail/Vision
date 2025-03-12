package com.also.vision.model;

/**
 * 发送数据模型类
 */
public class SendDataModel {
    private byte[] data;
    private int msgId = -1;
    
    public SendDataModel() {
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    public int getMsgId() {
        return msgId;
    }
    
    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }
} 