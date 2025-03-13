package com.also.vision;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息管理类
 * 负责处理设备通信的消息发送和接收
 *
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

    // 消息ID常量
    /**
     * 应用状态消息ID
     * 请求体: {"token":12345,"msg_id":1,"type":"app_status"}
     * 响应体: {"msg_id":1,"rval":0}
     */
    public static final int MSG_APP_STATUS = 1;

    /**
     * 设备信息消息ID
     * 请求体: {"token":12345,"msg_id":11}
     * 响应体: {"msg_id":11,"camera_type":"F6S","firm_ver":"V1.0.0","firm_date":"2023-01-01","param_version":"1.0","serial_num":"SN12345678","verify_code":"VC12345678"}
     */
    public static final int MSG_DEVICE_INFO = 11;

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
     * 事件记录消息ID
     * 请求体: {"token":12345,"msg_id":513}
     * 响应体: {"msg_id":513,"rval":0}
     */
    public static final int MSG_EVENT_RECORD = 513;

    /**
     * 拍照消息ID
     * 请求体: {"token":12345,"msg_id":769}
     * 响应体: {"msg_id":769,"rval":0}
     */
    public static final int MSG_TAKE_PHOTO = 769;

    /**
     * 获取文件列表消息ID
     * 请求体: {"token":12345,"msg_id":1281,"param":"all","offset":0,"count":20}
     * 响应体: {"msg_id":1281,"rval":0,"total":100,"files":[{"name":"IMG_0001.JPG","path":"/DCIM/100MEDIA/","url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG","thumb":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG","size":1024000,"time":"2023-05-01 12:00:00","type":2}]}
     */
    public static final int MSG_GET_FILE_LIST = 1281;

    /**
     * 删除文件消息ID
     * 请求体: {"token":12345,"msg_id":1282,"param":"IMG_0001.JPG"}
     * 响应体: {"msg_id":1282,"rval":0}
     */
    public static final int MSG_DELETE_FILE = 1282;

    /**
     * 下载文件消息ID
     * 请求体: {"token":12345,"msg_id":1283,"param":"IMG_0001.JPG"}
     * 响应体: {"msg_id":1283,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG"}
     */
    public static final int MSG_DOWNLOAD_FILE = 1283;

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
        retryCount = 0;

        // 初始化连接
        connection = DeviceConnection.getInstance();
        connection.init(new DeviceConnection.ConnectionCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "设备连接成功，开始会话");
                startSession();
            }

            @Override
            public void onConnectionFailed(String reason) {
                Log.e(TAG, "连接失败: " + reason);
                if (callback != null) {
                    mainHandler.post(() -> callback.onConnectionFailed(reason));
                }
            }

            @Override
            public void onDataReceived(byte[] data) {
                parseData(data);
            }
        });
    }
    /**
     * 解析接收到的数据
     */
    private void parseData(byte[] data) {
        try {
            // 解析接收到的数据
            String jsonStr = new String(data);
            Log.d(TAG, "接收到数据: " + jsonStr);

            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            if (jsonObject == null) {
                Log.e(TAG, "JSON解析失败，数据格式不正确");
                return;
            }

            if (!jsonObject.containsKey("msg_id")) {
                Log.e(TAG, "JSON缺少msg_id字段");
                return;
            }

            int msgId = jsonObject.getIntValue("msg_id");
            int result = jsonObject.containsKey("rval") ? jsonObject.getIntValue("rval") : 0;

            // 创建消息
            Message message = Message.obtain();
            message.what = msgId;
            message.arg1 = result;
            message.obj = jsonStr;

            // 发送到Handler处理
            messageHandler.sendMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "解析数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理消息
     */
    private void processMessage(Message msg) {
        String jsonStr = (String) msg.obj;
        int msgId = msg.what;
        int result = msg.arg1;

        Log.d(TAG, "处理消息: ID=" + msgId + ", 结果=" + result);

        // 处理特殊消息
        switch (msgId) {
            case MSG_START_SESSION:
                // 处理会话开始响应，获取令牌
                try {
                    JSONObject jsonObject = JSON.parseObject(jsonStr);
                    if (result == 0 && jsonObject.containsKey("param")) {
                        this.tokenNumber = jsonObject.getIntValue("param");
                        Log.d(TAG, "获取会话令牌: " + this.tokenNumber);

                        // 会话开始后，获取设备信息和SD卡信息
                        getDeviceInfo();
                        getSDCardInfo();

                        // 通知连接成功
                        if (callback != null) {
                            mainHandler.post(() -> callback.onConnected());
                        }
                    } else {
                        Log.e(TAG, "获取会话令牌失败，错误码: " + result);
                        if (retryCount < MAX_RETRY_COUNT) {
                            retryCount++;
                            Log.d(TAG, "重试开始会话，第 " + retryCount + " 次");
                            mainHandler.postDelayed(this::startSession, 1000);
                        } else {
                            Log.e(TAG, "开始会话失败，已达到最大重试次数");
                            if (callback != null) {
                                mainHandler.post(() -> callback.onConnectionFailed("开始会话失败，已达到最大重试次数"));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析会话令牌失败: " + e.getMessage());
                    e.printStackTrace();
                    if (callback != null) {
                        mainHandler.post(() -> callback.onConnectionFailed("解析会话令牌失败: " + e.getMessage()));
                    }
                }
                break;
        }

        // 回调消息
        if (callback != null) {
            final int finalMsgId = msgId;
            final int finalResult = result;
            final String finalJsonStr = jsonStr;
            mainHandler.post(() -> callback.onMessageReceived(finalMsgId, finalResult, finalJsonStr));
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(int msgId, String params, String type) {
        // 构建消息内容
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg_id", msgId);

        // 除了开始会话外，其他消息都需要令牌
        if (msgId != MSG_START_SESSION) {
            jsonObject.put("token", this.tokenNumber);
        }

        if (params != null && !params.isEmpty()) {
            jsonObject.put("param", params);
        }

        if (type != null && !type.isEmpty()) {
            jsonObject.put("type", type);
        }

        String jsonStr = jsonObject.toJSONString();

        // 发送数据
        connection.sendData(jsonStr.getBytes(), msgId);

        Log.d(TAG, "发送消息: " + jsonStr);
    }

    /**
     * 开始会话
     */
    public void startSession() {
        sendMessage(MSG_START_SESSION, null, null);
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
     * @param sdStatus SD卡状态，"1"表示正常
     * @param format 格式化类型，"1"表示快速格式化
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
     * 消息回调接口
     */
    public interface MessageCallback {
        void onConnected();
        void onConnectionFailed(String reason);
        void onMessageReceived(int msgId, int result, String content);
    }

    /**
     * 设置会话令牌
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
     * @param fileType 文件类型，"all"=所有文件，"video"=视频文件，"photo"=图片文件
     * @param offset 起始位置
     * @param count 获取数量
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