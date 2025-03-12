package com.also.vision.model;

/**
 * 发送参数的JSON模型类
 */
public class SendParamsJson {
    private int token;
    private int msg_id;
    private String param;
    private String type;
    
    public SendParamsJson() {
    }
    
    public int getToken() {
        return token;
    }
    
    public void setToken(int token) {
        this.token = token;
    }
    
    public int getMsg_id() {
        return msg_id;
    }
    
    public void setMsg_id(int msg_id) {
        this.msg_id = msg_id;
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
} 