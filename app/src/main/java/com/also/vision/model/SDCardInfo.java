package com.also.vision.model;

/**
 * SD卡信息模型类
 */
public class SDCardInfo {
    private long totalSpace;
    private long freeSpace;
    private int sdStatus;
    private String sdStatusDesc;
    
    public SDCardInfo() {
        this.totalSpace = 0;
        this.freeSpace = 0;
        this.sdStatus = 0;
        this.sdStatusDesc = "";
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
    
    public int getSdStatus() {
        return sdStatus;
    }
    
    public void setSdStatus(int sdStatus) {
        this.sdStatus = sdStatus;
        
        // 设置SD卡状态描述
        switch (sdStatus) {
            case 0:
                this.sdStatusDesc = "正常";
                break;
            case 1:
                this.sdStatusDesc = "未插入";
                break;
            case 2:
                this.sdStatusDesc = "错误";
                break;
            case 3:
                this.sdStatusDesc = "正在格式化";
                break;
            default:
                this.sdStatusDesc = "未知状态";
                break;
        }
    }
    
    public String getSdStatusDesc() {
        return sdStatusDesc;
    }
    
    public void setSdStatusDesc(String sdStatusDesc) {
        this.sdStatusDesc = sdStatusDesc;
    }
    
    /**
     * 获取已使用空间
     */
    public long getUsedSpace() {
        return totalSpace - freeSpace;
    }
    
    /**
     * 获取已使用空间百分比
     */
    public int getUsedPercent() {
        if (totalSpace <= 0) {
            return 0;
        }
        return (int) (getUsedSpace() * 100 / totalSpace);
    }
    
    /**
     * 格式化空间大小为可读字符串
     */
    public static String formatSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    @Override
    public String toString() {
        return "SDCardInfo{" +
                "totalSpace=" + formatSize(totalSpace) +
                ", freeSpace=" + formatSize(freeSpace) +
                ", usedSpace=" + formatSize(getUsedSpace()) +
                ", usedPercent=" + getUsedPercent() + "%" +
                ", sdStatus=" + sdStatus +
                ", sdStatusDesc='" + sdStatusDesc + '\'' +
                '}';
    }
} 