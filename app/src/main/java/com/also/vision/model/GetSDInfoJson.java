package com.also.vision.model;

/**
 * 获取SD卡信息的JSON模型类
 */
public class GetSDInfoJson {
    private int free_space;
    private String health_status;
    private int msg_id;
    private String residual_life;
    private int rval;
    private int total_space;

    public int getFree_space() {
        return this.free_space;
    }

    public String getHealth_status() {
        return this.health_status;
    }

    public int getMsg_id() {
        return this.msg_id;
    }

    public String getResidual_life() {
        return this.residual_life;
    }

    public int getRval() {
        return this.rval;
    }

    public int getTotal_space() {
        return this.total_space;
    }

    public void setFree_space(int i) {
        this.free_space = i;
    }

    public void setHealth_status(String str) {
        this.health_status = str;
    }

    public void setMsg_id(int i) {
        this.msg_id = i;
    }

    public void setResidual_life(String str) {
        this.residual_life = str;
    }

    public void setRval(int i) {
        this.rval = i;
    }

    public void setTotal_space(int i) {
        this.total_space = i;
    }
} 