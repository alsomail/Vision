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
import com.also.vision.model.DeviceFile;
import com.also.vision.model.GetFileListJson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private List<VisionCallback> callbacks = new ArrayList<>();

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
     * 添加回调
     *
     * @param callback 回调接口
     */
    public void addCallback(VisionCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * 移除回调
     *
     * @param callback 回调接口
     */
    public void removeCallback(VisionCallback callback) {
        if (callback != null) {
            callbacks.remove(callback);
        }
    }

    /**
     * 初始化客户端
     */
    public void init(Context context, VisionCallback callback) {
        this.context = context.getApplicationContext();

        // 添加回调
        addCallback(callback);

        // 初始化消息管理器
        messageManager.init(this.context, new MessageManager.MessageCallback() {
            @Override
            public void onConnected() {
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onConnected();
                }
            }

            @Override
            public void onConnectionFailed(String reason) {
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onConnectionFailed(reason);
                }
            }

            @Override
            public void onMessageReceived(int msgId, int result, String content) {
                handleMessage(msgId, result, content);
            }
        });

        // 初始化视频管理器
        videoManager.init(this.context, new VideoStreamManager.VideoCallback() {
            @Override
            public void onPlayStarted() {
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onVideoPlayStarted();
                }
            }

            @Override
            public void onPlayStopped() {
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onVideoPlayStopped();
                }
            }

            @Override
            public void onPlayError(String error) {
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onVideoPlayError(error);
                }
            }
        });
    }

    /**
     * 设置视频预览控件
     */
    public void setSurfaceView(SurfaceView surfaceView) {
        videoManager.setSurfaceView(surfaceView);
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(int msgId, int result, String content) {
        switch (msgId) {
            case MessageManager.MSG_GET_DEVICE_INFO:
                // 获取设备信息响应
                if (result == 0) {
                    try {
                        GetDeviceInfoJson json = JSON.parseObject(content, GetDeviceInfoJson.class);
                        DeviceInfo deviceInfo = json.getDeviceInfo();

                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onDeviceInfoReceived(deviceInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case MessageManager.MSG_GET_SD_INFO:
                // 获取SD卡信息响应
                if (result == 0) {
                    try {
                        GetSDInfoJson json = JSON.parseObject(content, GetSDInfoJson.class);
                        SDCardInfo sdInfo = json.getSdInfo();

                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onSDCardInfoReceived(sdInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case MessageManager.MSG_TAKE_PHOTO:
                // 拍照响应
                if (result == 0) {
                    try {
                        TakePhotoJson json = JSON.parseObject(content, TakePhotoJson.class);
                        // 拍照成功

                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onPhotoTaken();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onPhotoFailed("解析拍照响应失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onPhotoFailed("拍照失败，错误码: " + result);
                    }
                }
                break;

            case MessageManager.MSG_FORMAT_SD:
                // 格式化SD卡响应
                if (result == 0) {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onSDCardFormatted();
                    }
                } else {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onSDCardFormatFailed("格式化SD卡失败，错误码: " + result);
                    }
                }
                break;

            case MessageManager.MSG_GET_FILE_LIST:
                // 获取文件列表响应
                if (result == 0) {
                    try {
                        GetFileListJson json = JSON.parseObject(content, GetFileListJson.class);
                        List<DeviceFile> fileList = json.getFileList();
                        int total = json.getTotal();

                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onFileListReceived(fileList, total);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onFileListFailed("解析文件列表失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onFileListFailed("获取文件列表失败，错误码: " + result);
                    }
                }
                break;

            case MessageManager.MSG_DELETE_FILE:
                // 删除文件响应
                if (result == 0) {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onFileDeleted();
                    }
                } else {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onFileDeleteFailed("删除文件失败，错误码: " + result);
                    }
                }
                break;

            case MessageManager.MSG_DOWNLOAD_FILE:
                // 下载文件响应: {"msg_id":1283,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG"}
                if (result == 0) {
                    try {
                        JSONObject jsonObject = JSON.parseObject(content);
                        String url = jsonObject.getString("url");

                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onFileDownloadUrl(url);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 通知所有回调
                        for (VisionCallback cb : callbacks) {
                            cb.onFileDownloadFailed("解析下载URL失败: " + e.getMessage());
                        }
                    }
                } else {
                    // 通知所有回调
                    for (VisionCallback cb : callbacks) {
                        cb.onFileDownloadFailed("获取下载URL失败，错误码: " + result);
                    }
                }
                break;

            default:
                // 其他消息
                // 通知所有回调
                for (VisionCallback cb : callbacks) {
                    cb.onMessageReceived(msgId, result, content);
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
     * 获取文件列表
     *
     * @param fileType 文件类型，"all"=所有文件，"video"=视频文件，"photo"=图片文件
     * @param offset   起始位置
     * @param count    获取数量
     */
    public void getFileList(String fileType, int offset, int count) {
        messageManager.getFileList(fileType, offset, count);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        messageManager.deleteFile(fileName);
    }

    /**
     * 下载文件
     *
     * @param fileName 文件名
     */
    public void downloadFile(String fileName) {
        messageManager.downloadFile(fileName);
    }

    /**
     * 视频客户端回调接口
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

        // 视频播放相关
        void onVideoPlayStarted();

        void onVideoPlayStopped();

        void onVideoPlayError(String error);

        // 拍照相关
        void onPhotoTaken();

        void onPhotoFailed(String reason);

        // 截图相关
        void onSnapshotTaken(String path);

        // 事件录制相关
        void onEventRecorded();

        void onEventRecordFailed(String reason);

        // SD卡格式化相关
        void onSDCardFormatted();

        void onSDCardFormatFailed(String reason);

        // 文件列表相关
        void onFileListReceived(List<DeviceFile> fileList, int total);

        void onFileListFailed(String reason);

        // 文件删除相关
        void onFileDeleted();

        void onFileDeleteFailed(String reason);

        // 文件下载相关
        void onFileDownloadUrl(String url);

        void onFileDownloadFailed(String reason);

        // 通用消息回调
        void onMessageReceived(int msgId, int result, String content);
    }
}