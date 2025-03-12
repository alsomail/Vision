package com.also.vision.model;

/**
 * 设备信息模型类
 */
public class DeviceInfo {
    private String cameraType;
    private String firmwareVersion;
    private String firmwareDate;
    private String paramVersion;
    private String serialNumber;
    private String verifyCode;
    
    public DeviceInfo() {
    }
    
    public String getCameraType() {
        return cameraType;
    }
    
    public void setCameraType(String cameraType) {
        this.cameraType = cameraType;
    }
    
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    public String getFirmwareDate() {
        return firmwareDate;
    }
    
    public void setFirmwareDate(String firmwareDate) {
        this.firmwareDate = firmwareDate;
    }
    
    public String getParamVersion() {
        return paramVersion;
    }
    
    public void setParamVersion(String paramVersion) {
        this.paramVersion = paramVersion;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getVerifyCode() {
        return verifyCode;
    }
    
    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }
    
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "cameraType='" + cameraType + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", firmwareDate='" + firmwareDate + '\'' +
                ", paramVersion='" + paramVersion + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", verifyCode='" + verifyCode + '\'' +
                '}';
    }
}