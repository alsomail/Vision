package com.also.vision.model;

/**
 * 获取设备信息的JSON模型类
 */
public class GetDeviceInfoJson {
    private String camera_type;
    private String firm_date;
    private String firm_ver;
    private int msg_id;
    private String param_version;
    private String serial_num;
    private String verify_code;

    public String getCamera_type() {
        return this.camera_type;
    }

    public String getFirm_date() {
        return this.firm_date;
    }

    public String getFirm_ver() {
        return this.firm_ver;
    }

    public int getMsg_id() {
        return this.msg_id;
    }

    public String getParam_version() {
        return this.param_version;
    }

    public String getSerial_num() {
        return this.serial_num;
    }

    public String getVerify_code() {
        return this.verify_code;
    }

    public void setCamera_type(String str) {
        this.camera_type = str;
    }

    public void setFirm_date(String str) {
        this.firm_date = str;
    }

    public void setFirm_ver(String str) {
        this.firm_ver = str;
    }

    public void setMsg_id(int i) {
        this.msg_id = i;
    }

    public void setParam_version(String str) {
        this.param_version = str;
    }

    public void setSerial_num(String str) {
        this.serial_num = str;
    }

    public void setVerify_code(String str) {
        this.verify_code = str;
    }
} 