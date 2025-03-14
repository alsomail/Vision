package com.also.vision.model;

/**
 * 设备信息实体类
 */
public class DeviceInfo {
    private String deviceName; // 设备名称
    private String chipId; // 芯片ID
    private String firmwareVersion; // 固件版本
    private String hardwareVersion; // 硬件版本
    private String apiVersion; // API版本
    private String serialNumber; // 序列号
    private String vendorName; // 厂商名称
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceName='" + deviceName + '\'' +
                ", chipId='" + chipId + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", hardwareVersion='" + hardwareVersion + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", vendorName='" + vendorName + '\'' +
                '}';
    }
}