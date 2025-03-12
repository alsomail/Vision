package com.also.vision;

import android.content.Context;
import android.view.SurfaceView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.also.vision.model.DeviceInfo;
import com.also.vision.model.GetDeviceInfoJson;
import com.also.vision.model.GetSDInfoJson;
import com.also.vision.model.GetTokenNumberJson;
import com.also.vision.model.SDCardInfo;
import com.also.vision.model.TakePhotoJson;

/**
 * 行车记录仪客户端
 * 提供统一的接口与设备进行通信
 */
public class VisionClient {
    private static final String TAG = "VisionClient";
    private static VisionClient instance;
    
    private Context context;
    private MessageManager messageManager;
    private VideoStreamManager videoManager;
    private VisionCallback callback;
    
    private VisionClient() {
        messageManager = MessageManager.getInstance();
        videoManager = VideoStreamManager.getInstance();
    }
    
    public static VisionClient getInstance() {
        if (instance == null) {
            synchronized (VisionClient.class) {
                if (instance == null) {
                    instance = new VisionClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化客户端
     */
    public void init(Context context, VisionCallback callback) {
        this.context = context;
        this.callback = callback;
        
        // 初始化消息管理器
        messageManager.init(new MessageManager.MessageCallback() {
            @Override
            public void onConnected() {
                if (VisionClient.this.callback != null) {
                    VisionClient.this.callback.onConnected();
                }
            }
            
            @Override
            public void onConnectionFailed(String reason) {
                if (VisionClient.this.callback != null) {
                    VisionClient.this.callback.onConnectionFailed(reason);
                }
            }
            
            @Override
            public void onMessageReceived(int msgId, int result, String content) {
                handleMessage(msgId, result, content);
            }
        });
    }
    
    /**
     * 初始化视频播放器
     */
    public void initVideoPlayer(SurfaceView surfaceView) {
        videoManager.init(context, surfaceView, new VideoStreamManager.VideoCallback() {
            @Override
            public void onPlayStarted() {
                if (callback != null) {
                    callback.onVideoPlayStarted();
                }
            }
            
            @Override
            public void onPlayStopped() {
                if (callback != null) {
                    callback.onVideoPlayStopped();
                }
            }
            
            @Override
            public void onPlayError(String error) {
                if (callback != null) {
                    callback.onVideoPlayError(error);
                }
            }
            
            @Override
            public void onSnapshotTaken(String path) {
                if (callback != null) {
                    callback.onSnapshotTaken(path);
                }
            }
        });
    }
    
    /**
     * 处理接收到的消息
     * 根据不同的消息ID解析对应的协议体并回调给上层
     */
    private void handleMessage(int msgId, int result, String content) {
        switch (msgId) {
            case MessageManager.MSG_START_SESSION:
                // 开始会话响应: {"msg_id":257,"param":12345,"rval":0}
                if (result == 0) {
                    try {
                        GetTokenNumberJson tokenJson = JSON.parseObject(content, GetTokenNumberJson.class);
                        int tokenNumber = tokenJson.getParam();

                        // 设置会话令牌
                        messageManager.setTokenNumber(tokenNumber);

                        // 设置应用状态
                        messageManager.setAppStatus();

                        if (callback != null) {
                            callback.onSessionStarted();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onSessionFailed("解析令牌失败: " + e.getMessage());
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onSessionFailed("开始会话失败，错误码: " + result);
                    }
                }
                break;
                
            case MessageManager.MSG_DEVICE_INFO:
                // 设备信息响应: {"msg_id":11,"camera_type":"F6S","firm_ver":"V1.0.0","firm_date":"2023-01-01","param_version":"1.0","serial_num":"SN12345678","verify_code":"VC12345678"}
                if (result == 0) {
                    try {
                        GetDeviceInfoJson deviceInfoJson = JSON.parseObject(content, GetDeviceInfoJson.class);
                        
                        DeviceInfo deviceInfo = new DeviceInfo();
                        deviceInfo.setCameraType(deviceInfoJson.getCamera_type());
                        deviceInfo.setFirmwareVersion(deviceInfoJson.getFirm_ver());
                        deviceInfo.setFirmwareDate(deviceInfoJson.getFirm_date());
                        deviceInfo.setParamVersion(deviceInfoJson.getParam_version());
                        deviceInfo.setSerialNumber(deviceInfoJson.getSerial_num());
                        deviceInfo.setVerifyCode(deviceInfoJson.getVerify_code());
                        
                        if (callback != null) {
                            callback.onDeviceInfoReceived(deviceInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
                
            case MessageManager.MSG_SD_INFO:
                // SD卡信息响应: {"msg_id":100,"rval":0,"total_space":32768,"free_space":16384,"health_status":"good","residual_life":"80%"}
                if (result == 0) {
                    try {
                        GetSDInfoJson sdInfoJson = JSON.parseObject(content, GetSDInfoJson.class);
                        
                        SDCardInfo sdInfo = new SDCardInfo();
                        sdInfo.setTotalSpace(sdInfoJson.getTotal_space() * 1024 * 1024L); // 转换为字节
                        sdInfo.setFreeSpace(sdInfoJson.getFree_space() * 1024 * 1024L);   // 转换为字节
                        
                        // 设置SD卡状态
                        if (sdInfoJson.getTotal_space() == -1 || sdInfoJson.getFree_space() == -1) {
                            sdInfo.setSdStatus(1); // 未插入
                        } else {
                            sdInfo.setSdStatus(0); // 正常
                        }
                        
                        if (callback != null) {
                            callback.onSDCardInfoReceived(sdInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
                
            case MessageManager.MSG_SD_FORMAT:
                // 格式化SD卡响应: {"msg_id":102,"rval":0}
                if (result == 0) {
                    if (callback != null) {
                        callback.onSDCardFormatted();
                    }
                } else {
                    if (callback != null) {
                        callback.onSDCardFormatFailed("格式化SD卡失败，错误码: " + result);
                    }
                }
                break;
                
            case MessageManager.MSG_TAKE_PHOTO:
                // 拍照响应: {"msg_id":769,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG","thumbnailUrl":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG","fileType":1}
                if (result == 0) {
                    try {
                        TakePhotoJson photoJson = JSON.parseObject(content, TakePhotoJson.class);
                        
                        // 保存照片URL
                        String photoUrl = photoJson.getUrl();
                        String thumbnailUrl = photoJson.getThumbnailUrl();
                        
                        if (callback != null) {
                            callback.onPhotoTaken();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onPhotoFailed("解析照片信息失败: " + e.getMessage());
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onPhotoFailed("拍照失败，错误码: " + result);
                    }
                }
                break;
                
            case MessageManager.MSG_EVENT_RECORD:
                // 事件记录响应: {"msg_id":513,"rval":0}
                if (result == 0) {
                    if (callback != null) {
                        callback.onEventRecorded();
                    }
                } else {
                    String errorMsg = "事件记录失败，错误码: " + result;
                    if (result == -1) {
                        errorMsg = "SD卡已满";
                    } else if (result == -2) {
                        errorMsg = "SD卡写入保护";
                    }
                    
                    if (callback != null) {
                        callback.onEventRecordFailed(errorMsg);
                    }
                }
                break;
                
            default:
                // 其他消息
                if (callback != null) {
                    callback.onMessageReceived(msgId, result, content);
                }
                break;
        }
    }
    
    /**
     * 连接设备
     */
    public void connect() {
        messageManager.connect();
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        messageManager.disconnect();
        videoManager.release();
    }
    
    /**
     * 开始播放视频
     */
    public void startVideoPlay() {
        videoManager.startPlay();
    }
    
    /**
     * 停止播放视频
     */
    public void stopVideoPlay() {
        videoManager.stopPlay();
    }
    
    /**
     * 拍照
     */
    public void takePhoto() {
        messageManager.takePhoto();
    }
    
    /**
     * 截图
     */
    public void takeSnapshot() {
        videoManager.takeSnapshot();
    }
    
    /**
     * 事件记录
     */
    public void eventRecord() {
        messageManager.eventRecord();
    }
    
    /**
     * 格式化SD卡
     */
    public void formatSDCard() {
        messageManager.formatSDCard("1", "1");
    }
    
    /**
     * 获取设备信息
     */
    public void getDeviceInfo() {
        messageManager.getDeviceInfo();
    }
    
    /**
     * 获取SD卡信息
     */
    public void getSDCardInfo() {
        messageManager.getSDCardInfo();
    }
    
    /**
     * 客户端回调接口
     */
    public interface VisionCallback {
        // 连接相关
        void onConnected();
        void onConnectionFailed(String reason);
        void onSessionStarted();
        void onSessionFailed(String reason);
        
        // 设备信息相关
        void onDeviceInfoReceived(DeviceInfo deviceInfo);
        void onSDCardInfoReceived(SDCardInfo sdInfo);
        
        // 视频相关
        void onVideoPlayStarted();
        void onVideoPlayStopped();
        void onVideoPlayError(String error);
        
        // 拍照相关
        void onPhotoTaken();
        void onPhotoFailed(String reason);
        void onSnapshotTaken(String path);
        
        // 事件记录相关
        void onEventRecorded();
        void onEventRecordFailed(String reason);
        
        // SD卡相关
        void onSDCardFormatted();
        void onSDCardFormatFailed(String reason);
        
        // 其他消息
        void onMessageReceived(int msgId, int result, String content);
    }
}