package com.also.vision.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 获取令牌响应JSON
 */
public class GetTokenNumberJson {
    @JSONField(name = "msg_id")
    private int msgId;
    
    @JSONField(name = "rval")
    private int result;
    
    @JSONField(name = "param")
    private int tokenNumber;
    
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
    
    public int getTokenNumber() {
        return tokenNumber;
    }
    
    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }
} 