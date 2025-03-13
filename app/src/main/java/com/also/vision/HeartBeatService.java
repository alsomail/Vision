package com.also.vision;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 心跳服务
 * 负责定期向设备发送心跳消息，保持连接活跃并同步时间
 *
 * 心跳协议：
 * 请求体: {"token":12345,"msg_id":1,"type":"date_time"}
 * 响应体: {"msg_id":1,"rval":0,"param":"2023-05-01 12:00:00"}
 */
public class HeartBeatService extends Service {
    private static final String TAG = "HeartBeatService";

    // 心跳间隔时间，单位：毫秒
    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒

    // 心跳消息ID常量
    /**
     * 心跳消息
     * 请求体: {"token":12345,"msg_id":1,"type":"date_time"}
     * 响应体: {"msg_id":1,"rval":0,"param":"2023-05-01 12:00:00"}
     */
    public static final int MSG_HEARTBEAT = 1;

    /**
     * 设置日期时间
     * 请求体: {"token":12345,"msg_id":2,"type":"date_time","param":"2023-05-01 12:00:00"}
     * 响应体: {"msg_id":2,"rval":0}
     */
    public static final int MSG_SET_DATE_TIME = 2;

    // 会话令牌
    private int tokenNumber;

    // 线程池
    private ExecutorService executorService;

    // 消息处理器
    private Handler messageHandler;

    // 心跳运行标志
    private boolean isRunning = false;

    // 设备连接
    private DeviceConnection connection;

    // 时间同步阈值，单位：毫秒
    private static final long TIME_SYNC_THRESHOLD = 60000; // 1分钟

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "心跳服务创建");

        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();

        // 初始化消息处理器
        initMessageHandler();

        // 获取设备连接实例
        connection = MessageManager.getInstance().getConnection();

        // 启动心跳线程
        isRunning = true;
        startHeartbeatThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // 从Intent中获取会话令牌
            tokenNumber = intent.getIntExtra("tokenNumber", 0);
            Log.d(TAG, "心跳服务启动，令牌：" + tokenNumber);
        }

        // 如果服务被系统杀死后重新创建，保持运行
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "心跳服务销毁");

        // 停止心跳
        isRunning = false;

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // 等待线程池关闭
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * 初始化消息处理器
     */
    private void initMessageHandler() {
        messageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String jsonStr = (String) msg.obj;
                Log.d(TAG, "发送心跳消息：" + jsonStr);

                // 发送心跳消息
                if (connection != null) {
                    connection.sendData(jsonStr.getBytes(), MSG_HEARTBEAT);
                }
            }
        };
    }

    /**
     * 启动心跳线程
     */
    private void startHeartbeatThread() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        // 发送心跳
                        sendHeartbeat();

                        // 等待下一次心跳
                        Thread.sleep(HEARTBEAT_INTERVAL);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "心跳线程被中断：" + e.getMessage());
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "发送心跳失败：" + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 发送心跳消息
     */
    private void sendHeartbeat() {
        // 构建心跳消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", tokenNumber);
        jsonObject.put("msg_id", MSG_HEARTBEAT);
        jsonObject.put("type", "date_time");

        String jsonStr = jsonObject.toJSONString();

        // 通过Handler发送消息
        Message message = Message.obtain();
        message.obj = jsonStr;
        messageHandler.sendMessage(message);
    }

    /**
     * 处理心跳响应
     * @param jsonStr 响应JSON字符串
     */
    public void processHeartbeatResponse(String jsonStr) {
        try {
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            int result = jsonObject.getIntValue("rval");

            if (result == 0 && jsonObject.containsKey("param")) {
                // 获取设备时间
                String deviceTime = jsonObject.getString("param");
                Log.d(TAG, "设备时间：" + deviceTime);

                // 检查是否需要同步时间
                checkTimeSync(deviceTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析心跳响应失败：" + e.getMessage());
        }
    }

    /**
     * 检查是否需要同步时间
     * @param deviceTime 设备时间字符串
     */
    private void checkTimeSync(String deviceTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date deviceDate = sdf.parse(deviceTime);
            long deviceTimestamp = deviceDate.getTime();
            long currentTimestamp = System.currentTimeMillis();

            // 计算时间差
            long timeDiff = Math.abs(deviceTimestamp - currentTimestamp);

            // 如果时间差超过阈值，则同步时间
            if (timeDiff > TIME_SYNC_THRESHOLD) {
                Log.d(TAG, "设备时间与本地时间相差：" + timeDiff + "ms，开始同步时间");

                // 获取当前时间
                String currentTime = sdf.format(new Date(currentTimestamp));

                // 同步时间
                syncTime(currentTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "检查时间同步失败：" + e.getMessage());
        }
    }

    /**
     * 同步时间
     * @param currentTime 当前时间字符串
     */
    private void syncTime(String currentTime) {
        // 构建时间同步消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", tokenNumber);
        jsonObject.put("msg_id", MSG_SET_DATE_TIME);
        jsonObject.put("type", "date_time");
        jsonObject.put("param", currentTime);

        String jsonStr = jsonObject.toJSONString();

        // 发送时间同步消息
        if (connection != null) {
            connection.sendData(jsonStr.getBytes(), MSG_SET_DATE_TIME);
            Log.d(TAG, "发送时间同步消息：" + jsonStr);
        }
    }

    /**
     * 更新会话令牌
     * @param tokenNumber 新的会话令牌
     */
    public void updateToken(int tokenNumber) {
        this.tokenNumber = tokenNumber;
        Log.d(TAG, "更新心跳服务令牌：" + tokenNumber);
    }
}