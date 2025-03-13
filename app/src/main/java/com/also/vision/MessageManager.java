package com.also.vision;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息管理类
 * 负责处理设备通信的消息发送和接收
 * <p>
 * 协议说明：
 * 1. 所有通信基于JSON格式
 * 2. 每个消息都包含msg_id字段，用于标识消息类型
 * 3. 请求消息通常包含token字段(会话令牌)，由开始会话获得
 * 4. 响应消息通常包含rval字段，0表示成功，其他值表示错误
 */
public class MessageManager {
    private static final String TAG = "MessageManager";
    private static MessageManager instance;

    private DeviceConnection connection;
    private Handler messageHandler;
    private MessageCallback callback;
    private int tokenNumber = 0; // 会话令牌
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean heartbeatRunning = false;
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒

    // 会话管理相关消息
    /**
     * 开始会话消息ID
     * 请求体: {"msg_id":257}
     * 响应体: {"msg_id":257,"param":12345,"rval":0}
     */
    public static final int MSG_START_SESSION = 257;

    /**
     * 结束会话消息ID
     * 请求体: {"token":12345,"msg_id":258}
     * 响应体: {"msg_id":258,"rval":0}
     */
    public static final int MSG_END_SESSION = 258;

    /**
     * 应用状态消息ID
     * 请求体: {"token":12345,"msg_id":1,"type":"app_status"}
     * 响应体: {"msg_id":1,"rval":0}
     */
    public static final int MSG_APP_STATUS = 1;

    // 设备信息相关消息
    /**
     * 设备信息消息ID
     * 请求体: {"token":12345,"msg_id":11}
     * 响应体: {"msg_id":11,"camera_type":"F6S","firm_ver":"V1.0.0","firm_date":"2023-01-01","param_version":"1.0","serial_num":"SN12345678","verify_code":"VC12345678"}
     */
    public static final int MSG_DEVICE_INFO = 11;

    /**
     * 获取设备状态
     * 请求体: {"token":12345,"msg_id":13}
     * 响应体: {"msg_id":13,"rval":0,"status":"recording","battery":80,"sd_free_space":16384}
     */
    public static final int MSG_GET_DEVICE_STATUS = 13;

    /**
     * 获取GPS信息
     * 请求体: {"token":12345,"msg_id":14}
     * 响应体: {"msg_id":14,"rval":0,"latitude":39.9042,"longitude":116.4074,"speed":60,"direction":90,"time":"2023-05-01 12:00:00"}
     */
    public static final int MSG_GET_GPS_INFO = 14;

    /**
     * 设备重启
     * 请求体: {"token":12345,"msg_id":3}
     * 响应体: {"msg_id":3,"rval":0}
     */
    public static final int MSG_DEVICE_REBOOT = 3;

    /**
     * 固件升级
     * 请求体: {"token":12345,"msg_id":4,"param":"http://server/firmware.bin"}
     * 响应体: {"msg_id":4,"rval":0}
     * <p>
     * 升级状态通知:
     * 响应体: {"msg_id":9,"type":"upgrade_progress","param":"50"} // 升级进度50%
     * 响应体: {"msg_id":9,"type":"upgrade_complete"} // 升级完成
     * 响应体: {"msg_id":9,"type":"upgrade_failed","error":1} // 升级失败
     */
    public static final int MSG_FIRMWARE_UPGRADE = 4;
    public static final int MSG_UPGRADE_NOTIFICATION = 9;

    // 参数设置相关消息
    /**
     * 获取设备参数
     * 请求体: {"token":12345,"msg_id":16,"param":"resolution"}
     * 响应体: {"msg_id":16,"rval":0,"param":"1920x1080"}
     */
    public static final int MSG_GET_PARAM = 16;

    /**
     * 设置设备参数
     * 请求体: {"token":12345,"msg_id":17,"param":"resolution","value":"1920x1080"}
     * 响应体: {"msg_id":17,"rval":0}
     */
    public static final int MSG_SET_PARAM = 17;

    /**
     * 获取所有参数
     * 请求体: {"token":12345,"msg_id":1280}
     * 响应体: {"msg_id":1280,"rval":0,"param":[...]}
     */
    public static final int MSG_GET_ALL_PARAMS = 1280;

    // SD卡相关消息
    /**
     * SD卡信息消息ID
     * 请求体: {"token":12345,"msg_id":100}
     * 响应体: {"msg_id":100,"rval":0,"total_space":32768,"free_space":16384,"health_status":"good","residual_life":"80%"}
     */
    public static final int MSG_SD_INFO = 100;

    /**
     * 格式化SD卡消息ID
     * 请求体: {"token":12345,"msg_id":102,"sd_status":"1","format":"1"}
     * 响应体: {"msg_id":102,"rval":0}
     */
    public static final int MSG_SD_FORMAT = 102;

    // 录像和拍照相关消息
    /**
     * 事件记录/录制控制消息ID
     * <p>
     * 事件记录请求: {"token":12345,"msg_id":513,"type":"emergency"}
     * 事件记录响应: {"msg_id":513,"rval":0,"file_name":"EMR_0001.MP4"}
     * <p>
     * 开始录制请求: {"token":12345,"msg_id":513,"param":"record_start"}
     * 开始录制响应: {"msg_id":513,"rval":0}
     * <p>
     * 事件类型说明:
     * emergency: 紧急事件
     * collision: 碰撞事件
     * manual: 手动触发
     */
    public static final int MSG_EVENT_RECORD = 513;

    /**
     * 停止录制视频
     * 请求体: {"token":12345,"msg_id":514,"param":"record_stop"}
     * 响应体: {"msg_id":514,"rval":0}
     */
    public static final int MSG_RECORD_STOP = 514;

    /**
     * 拍照消息ID
     * 请求体: {"token":12345,"msg_id":769}
     * 响应体: {"msg_id":769,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG","thumbnailUrl":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG","fileType":2}
     * <p>
     * 响应说明:
     * url: 拍摄的照片URL，可通过HTTP直接访问
     * thumbnailUrl: 缩略图URL，可通过HTTP直接访问
     * fileType: 文件类型，2=图片文件
     */
    public static final int MSG_TAKE_PHOTO = 769;

    // 视频流相关消息
    /**
     * 开始视频流
     * 请求体: {"token":12345,"msg_id":259,"param":"rtsp://192.168.42.1/live"}
     * 响应体: {"msg_id":259,"rval":0,"url":"rtsp://192.168.42.1/live"}
     */
    public static final int MSG_START_STREAM = 259;

    /**
     * 停止视频流
     * 请求体: {"token":12345,"msg_id":260}
     * 响应体: {"msg_id":260,"rval":0}
     */
    public static final int MSG_STOP_STREAM = 260;

    // 文件管理相关消息
    /**
     * 获取文件列表消息ID
     * 请求体: {"token":12345,"msg_id":1281,"param":"all","offset":0,"count":20}
     * 响应体: {"msg_id":1281,"rval":0,"total":100,"files":[{"name":"IMG_0001.JPG","path":"/DCIM/100MEDIA/","url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG","thumb":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG","size":1024000,"time":"2023-05-01 12:00:00","type":2}]}
     * <p>
     * 参数说明:
     * param: 文件类型，"all"=所有文件，"video"=视频文件，"photo"=图片文件
     * offset: 起始位置，用于分页
     * count: 获取数量，用于分页
     * <p>
     * 响应说明:
     * total: 文件总数
     * files: 文件列表
     * name: 文件名
     * path: 文件路径
     * url: 文件URL，可通过HTTP直接访问
     * thumb: 缩略图URL，可通过HTTP直接访问
     * size: 文件大小(字节)
     * time: 文件创建时间
     * type: 文件类型，1=视频文件，2=图片文件
     */
    public static final int MSG_GET_FILE_LIST = 1281;

    /**
     * 删除文件消息ID
     * 请求体: {"token":12345,"msg_id":1282,"param":"IMG_0001.JPG"}
     * 响应体: {"msg_id":1282,"rval":0}
     * <p>
     * 参数说明:
     * param: 要删除的文件名
     */
    public static final int MSG_DELETE_FILE = 1282;

    /**
     * 下载文件消息ID
     * 请求体: {"token":12345,"msg_id":1283,"param":"IMG_0001.JPG"}
     * 响应体: {"msg_id":1283,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG"}
     * <p>
     * 参数说明:
     * param: 要下载的文件名
     * <p>
     * 响应说明:
     * url: 文件下载URL，可通过HTTP直接下载
     */
    public static final int MSG_DOWNLOAD_FILE = 1283;

    /**
     * 文件传输状态通知
     * 响应体: {"msg_id":8,"type":"download_complete","param":"IMG_0001.JPG"}
     * 响应体: {"msg_id":8,"type":"download_failed","param":"IMG_0001.JPG","error":1}
     */
    public static final int MSG_FILE_TRANSFER_NOTIFICATION = 8;

    // 通知类消息
    /**
     * 通知消息
     * 响应体: {"msg_id":7,"type":"disconnect_shutdown"} 设备断开连接或关机
     * 响应体: {"msg_id":7,"type":"SD_rm"} SD卡移除
     * 响应体: {"msg_id":7,"type":"SD_insert"} SD卡插入
     * 响应体: {"msg_id":7,"type":"SD_err"} SD卡错误
     * 响应体: {"msg_id":7,"type":"auto_file_delete"} 文件自动删除(循环录制时)
     * <p>
     * 类型说明:
     * disconnect_shutdown: 设备断开连接或关机
     * SD_rm: SD卡移除
     * SD_insert: SD卡插入
     * SD_err: SD卡错误
     * auto_file_delete: 文件自动删除(循环录制时)
     */
    public static final int MSG_NOTIFICATION = 7;

    private MessageManager() {
        this.connection = DeviceConnection.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 创建消息处理Handler
        this.messageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                processMessage(msg);
            }
        };
    }

    public static MessageManager getInstance() {
        if (instance == null) {
            synchronized (MessageManager.class) {
                if (instance == null) {
                    instance = new MessageManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化消息管理器
     */
    public void init(MessageCallback callback) {
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        this.messageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                processMessage(msg);
            }
        };
    }

    /**
     * 处理接收到的消息
     */
    private void processMessage(Message msg) {
        // 处理消息
        if (msg.obj instanceof String) {
            String jsonStr = (String) msg.obj;
            try {
                JSONObject jsonObject = JSON.parseObject(jsonStr);
                int msgId = jsonObject.getIntValue("msg_id");
                int result = jsonObject.containsKey("rval") ? jsonObject.getIntValue("rval") : 0;

                Log.d(TAG, "收到消息: " + jsonStr);

                // 根据消息ID处理不同类型的消息
                switch (msgId) {
                    case MSG_START_SESSION:
                        // 处理开始会话响应
                        if (result == 0) {
                            int param = jsonObject.getIntValue("param");
                            Log.d(TAG, "会话开始成功，令牌: " + param);
                            this.tokenNumber = param;

                            // 会话开始后，设置应用状态、获取所有参数、获取SD卡信息和设备信息
                            setAppStatus();
                            getAllParams();
                            getSDCardInfo();
                            getDeviceInfo();

                            if (callback != null) {
                                mainHandler.post(() -> callback.onConnected());
                            }
                        } else {
                            Log.e(TAG, "会话开始失败，错误码: " + result);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onConnectionFailed("会话开始失败，错误码: " + result));
                            }
                        }
                        break;
                    case MSG_END_SESSION:
                        // 处理结束会话响应
                        if (result == 0) {
                            Log.d(TAG, "会话结束成功");
                        } else {
                            Log.e(TAG, "会话结束失败，错误码: " + result);
                        }
                        break;
                    case MSG_APP_STATUS:
                        // 处理应用状态或心跳响应
                        if (jsonObject.containsKey("type")) {
                            String type = jsonObject.getString("type");
                            if ("date_time".equals(type) && jsonObject.containsKey("param")) {
                                // 处理心跳响应中的时间同步
                                String deviceTime = jsonObject.getString("param");
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    Date deviceDate = sdf.parse(deviceTime);
                                    long deviceTimestamp = deviceDate.getTime();
                                    long currentTimestamp = System.currentTimeMillis();

                                    // 如果设备时间与本地时间相差超过1分钟，则同步时间
                                    if (Math.abs(deviceTimestamp - currentTimestamp) > 60000) {
                                        Log.d(TAG, "设备时间与本地时间相差较大，同步时间");
                                        String currentTime = sdf.format(new Date(currentTimestamp));
                                        setDateTime(currentTime);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "解析设备时间失败: " + e.getMessage());
                                }
                            }
                        }
                        break;
                    case MSG_NOTIFICATION:
                        // 处理通知消息
                        if (jsonObject.containsKey("type")) {
                            String type = jsonObject.getString("type");
                            Log.d(TAG, "收到通知消息: " + type);

                            if ("disconnect_shutdown".equals(type)) {
                                // 设备断开连接或关机
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onDeviceDisconnected("设备已断开连接或关机"));
                                }
                            } else if ("SD_rm".equals(type)) {
                                // SD卡移除
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onSDCardRemoved());
                                }
                            } else if ("SD_insert".equals(type)) {
                                // SD卡插入
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onSDCardInserted());
                                }
                            } else if ("SD_err".equals(type)) {
                                // SD卡错误
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onSDCardError());
                                }
                            }
                        }
                        break;
                    case MSG_TAKE_PHOTO:
                        // 处理拍照响应
                        if (result == 0) {
                            String url = jsonObject.containsKey("url") ? jsonObject.getString("url") : "";
                            String thumbnailUrl =
                                jsonObject.containsKey("thumbnailUrl") ? jsonObject.getString("thumbnailUrl") : "";
                            int fileType = jsonObject.containsKey("fileType") ? jsonObject.getIntValue("fileType") : 0;

                            Log.d(TAG, "拍照成功: URL=" + url + ", 缩略图URL=" + thumbnailUrl);

                            if (callback != null) {
                                mainHandler.post(() -> callback.onPhotoTaken(url, thumbnailUrl, fileType));
                            }
                        } else {
                            Log.e(TAG, "拍照失败，错误码: " + result);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onPhotoFailed(result));
                            }
                        }
                        break;
                    case MSG_EVENT_RECORD:
                        // 处理事件记录响应
                        if (result == 0) {
                            Log.d(TAG, "事件记录成功");
                            if (callback != null) {
                                mainHandler.post(() -> callback.onEventRecordSuccess());
                            }
                        } else {
                            Log.e(TAG, "事件记录失败，错误码: " + result);
                            if (callback != null) {
                                mainHandler.post(() -> callback.onEventRecordFailed(result));
                            }
                        }
                        break;
                    default:
                        // 处理其他消息
                        if (callback != null) {
                            mainHandler.post(() -> callback.onMessageReceived(msgId, result, jsonStr));
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "解析消息失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息
     *
     * @param msgId 消息ID
     * @param param 参数
     * @param type  类型
     */
    public void sendMessage(int msgId, String param, String type) {
        JSONObject jsonObject = new JSONObject();

        // 除了开始会话外，其他消息都需要令牌
        if (msgId != MSG_START_SESSION) {
            jsonObject.put("token", this.tokenNumber);
        }

        jsonObject.put("msg_id", msgId);

        if (param != null) {
            jsonObject.put("param", param);
        }

        if (type != null) {
            jsonObject.put("type", type);
        }

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), msgId);

        Log.d(TAG, "发送消息: " + jsonStr);
    }

    /**
     * 开始会话
     */
    public void startSession() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg_id", MSG_START_SESSION);

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), MSG_START_SESSION);

        Log.d(TAG, "发送开始会话命令: " + jsonStr);
    }

    /**
     * 结束会话
     */
    public void endSession() {
        sendMessage(MSG_END_SESSION, null, null);
    }

    /**
     * 获取设备信息
     */
    public void getDeviceInfo() {
        sendMessage(MSG_DEVICE_INFO, null, null);
    }

    /**
     * 获取SD卡信息
     */
    public void getSDCardInfo() {
        sendMessage(MSG_SD_INFO, null, null);
    }

    /**
     * 格式化SD卡
     *
     * @param sdStatus SD卡状态
     * @param format   格式化标志
     */
    public void formatSDCard(String sdStatus, String format) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", this.tokenNumber);
        jsonObject.put("msg_id", MSG_SD_FORMAT);
        jsonObject.put("sd_status", sdStatus);
        jsonObject.put("format", format);

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), MSG_SD_FORMAT);

        Log.d(TAG, "发送格式化SD卡命令: " + jsonStr);
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        sendMessage(MSG_TAKE_PHOTO, null, null);
    }

    /**
     * 事件记录
     */
    public void eventRecord() {
        sendMessage(MSG_EVENT_RECORD, null, null);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (connection != null) {
            // 停止心跳
            stopHeartbeat();
            // 先发送结束会话消息
            endSession();
            // 然后断开连接
            connection.disconnect();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        disconnect();
        executorService.shutdown();
    }

    /**
     * 获取所有参数
     */
    public void getAllParams() {
        sendMessage(MSG_GET_ALL_PARAMS, null, null);
    }

    /**
     * 消息回调接口
     */
    public interface MessageCallback {
        void onConnected();

        void onConnectionFailed(String reason);

        void onMessageReceived(int msgId, int result, String content);

        void onDeviceDisconnected(String reason);

        void onSDCardRemoved();

        void onSDCardInserted();

        void onSDCardError();

        void onPhotoTaken(String url, String thumbnailUrl, int fileType);

        void onPhotoFailed(int errorCode);

        void onEventRecordSuccess();

        void onEventRecordFailed(int errorCode);
    }

    /**
     * 设置会话令牌
     *
     * @param tokenNumber 会话令牌
     */
    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    /**
     * 设置应用状态
     */
    public void setAppStatus() {
        sendMessage(MSG_APP_STATUS, null, "app_status");
    }

    /**
     * 连接设备
     */
    public void connect() {
        // 重置重试计数
        retryCount = 0;

        // 连接设备
        if (connection != null) {
            connection.connect();
        } else {
            Log.e(TAG, "连接为空，无法连接设备");
            if (callback != null) {
                mainHandler.post(() -> callback.onConnectionFailed("连接为空，无法连接设备"));
            }
        }
    }

    /**
     * 获取连接实例
     */
    public DeviceConnection getConnection() {
        return connection;
    }

    /**
     * 获取令牌
     */
    public int getTokenNumber() {
        return this.tokenNumber;
    }

    /**
     * 获取文件列表
     *
     * @param fileType 文件类型，"all"=所有文件，"video"=视频文件，"photo"=图片文件
     * @param offset   起始位置
     * @param count    获取数量
     */
    public void getFileList(String fileType, int offset, int count) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", this.tokenNumber);
        jsonObject.put("msg_id", MSG_GET_FILE_LIST);
        jsonObject.put("param", fileType);
        jsonObject.put("offset", offset);
        jsonObject.put("count", count);

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), MSG_GET_FILE_LIST);

        Log.d(TAG, "发送获取文件列表命令: " + jsonStr);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", this.tokenNumber);
        jsonObject.put("msg_id", MSG_DELETE_FILE);
        jsonObject.put("param", fileName);

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), MSG_DELETE_FILE);

        Log.d(TAG, "发送删除文件命令: " + jsonStr);
    }

    /**
     * 下载文件
     *
     * @param fileName 文件名
     */
    public void downloadFile(String fileName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", this.tokenNumber);
        jsonObject.put("msg_id", MSG_DOWNLOAD_FILE);
        jsonObject.put("param", fileName);

        String jsonStr = jsonObject.toJSONString();
        connection.sendData(jsonStr.getBytes(), MSG_DOWNLOAD_FILE);

        Log.d(TAG, "发送下载文件命令: " + jsonStr);
    }
}