package com.also.vision.model;

/**
 * 获取令牌号的JSON模型类
 */
public class GetTokenNumberJson {
    private int msg_id;
    private int param;
    
    public GetTokenNumberJson() {
    }
    
    public int getMsg_id() {
        return msg_id;
    }
    
    public void setMsg_id(int msg_id) {
        this.msg_id = msg_id;
    }
    
    public int getParam() {
        return param;
    }
    
    public void setParam(int param) {
        this.param = param;
    }
} 