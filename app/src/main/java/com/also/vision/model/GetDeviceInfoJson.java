package com.also.vision.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 获取设备信息响应JSON
 */
public class GetDeviceInfoJson {
    @JSONField(name = "msg_id")
    private int msgId;
    
    @JSONField(name = "rval")
    private int result;
    
    @JSONField(name = "camera_type")
    private String deviceName;
    
    @JSONField(name = "chip_id")
    private String chipId;
    
    @JSONField(name = "firm_ver")
    private String firmwareVersion;
    
    @JSONField(name = "hw_ver")
    private String hardwareVersion;
    
    @JSONField(name = "api_ver")
    private String apiVersion;
    
    @JSONField(name = "serial_num")
    private String serialNumber;
    
    @JSONField(name = "vendor_name")
    private String vendorName;
    
    /**
     * 将JSON数据转换为DeviceInfo对象
     * @return DeviceInfo对象
     */
    public DeviceInfo getDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceName(deviceName);
        deviceInfo.setChipId(chipId);
        deviceInfo.setFirmwareVersion(firmwareVersion);
        deviceInfo.setHardwareVersion(hardwareVersion);
        deviceInfo.setApiVersion(apiVersion);
        deviceInfo.setSerialNumber(serialNumber);
        deviceInfo.setVendorName(vendorName);
        return deviceInfo;
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
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getChipId() {
        return chipId;
    }
    
    public void setChipId(String chipId) {
        this.chipId = chipId;
    }
    
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    public String getHardwareVersion() {
        return hardwareVersion;
    }
    
    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getVendorName() {
        return vendorName;
    }
    
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }
} 