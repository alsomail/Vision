package com.also.vision.model;

/**
 * SD卡信息实体类
 */
public class SDCardInfo {
    private long totalSpace; // 总空间(字节)
    private long freeSpace; // 可用空间(字节)
    private int status; // 状态: 0=正常, 1=未挂载, 2=错误
    
    // Getters and Setters
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
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "SDCardInfo{" +
                "totalSpace=" + totalSpace +
                ", freeSpace=" + freeSpace +
                ", status=" + status +
                '}';
    }
} 