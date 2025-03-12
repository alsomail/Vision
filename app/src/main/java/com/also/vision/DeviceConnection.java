package com.also.vision;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

/**
 * 设备连接管理类
 * 负责与行车记录仪设备建立Socket连接并处理通信
 */
public class DeviceConnection {
    private static final String TAG = "DeviceConnection";
    private static DeviceConnection instance;
    
    private SocketChannel socketChannel;
    private InetSocketAddress deviceAddress;
    private InetSocketAddress localAddress;
    private Selector selector;
    private ByteBuffer buffer;
    private ConnectionCallback callback;
    private boolean isConnected = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private ExecutorService executorService;
    private Handler mainHandler;
    private volatile boolean isRunning = false;
    
    // 设备默认IP地址和端口
    private static final String DEVICE_IP = "192.168.42.1";
    private static final int DEVICE_PORT = 8080;
    
    private DeviceConnection() {
        this.deviceAddress = new InetSocketAddress(DEVICE_IP, DEVICE_PORT);
        this.buffer = ByteBuffer.allocate(4096);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static DeviceConnection getInstance() {
        if (instance == null) {
            synchronized (DeviceConnection.class) {
                if (instance == null) {
                    instance = new DeviceConnection();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取WiFi IP地址
     */
    private static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            
            // 转换IP地址格式
            return String.format(Locale.US, "%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }
        return "";
    }
    
    /**
     * 检查是否已连接到设备WiFi
     */
    public static boolean isConnectedToDeviceWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        
        // 首先检查是否连接到任何WiFi
        if (activeNetworkInfo == null || activeNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            return false;
        }
        
        // 检查是否连接到行车记录仪的WiFi网络
        if (NetworkInfo.State.CONNECTED == activeNetworkInfo.getState()) {
            // 检查IP地址是否在192.168.42.x网段
            String ipAddress = getWifiIpAddress(context);
            if (ipAddress.startsWith("192.168.42.")) {
                // 尝试检测设备是否可达
                try {
                    InetAddress deviceAddr = InetAddress.getByName(DEVICE_IP);
                    return deviceAddr.isReachable(1000); // 1秒超时
                } catch (IOException e) {
                    Log.e(TAG, "检测设备连接失败: " + e.getMessage());
                }
            }
        }
        
        return false;
    }
    
    /**
     * 初始化连接
     */
    public void init(ConnectionCallback callback) {
        this.callback = callback;
        retryCount = 0;
    }
    
    /**
     * 连接设备
     */
    public void connect() {
        if (isConnected) {
            Log.d(TAG, "已经连接到设备");
            if (callback != null) {
                mainHandler.post(() -> callback.onConnected());
            }
            return;
        }
        
        // 使用线程池在后台线程执行网络操作
        executorService.execute(() -> {
            try {
                // 创建选择器
                selector = Selector.open();
                
                // 创建Socket通道
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                
                // 连接设备
                boolean connected = socketChannel.connect(deviceAddress);
                
                if (connected) {
                    // 直接连接成功
                    isConnected = true;
                    Log.d(TAG, "连接成功");
                    
                    // 注册读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    
                    if (callback != null) {
                        mainHandler.post(() -> callback.onConnected());
                    }
                } else {
                    // 异步连接，注册连接事件
                    socketChannel.register(selector, SelectionKey.OP_CONNECT);
                    
                    // 启动连接监听线程
                    isRunning = true;
                    executorService.submit(new ConnectionMonitor());
                    
                    Log.d(TAG, "开始异步连接");
                }
            } catch (IOException e) {
                Log.e(TAG, "连接失败: " + e.getMessage());
                e.printStackTrace();
                
                if (callback != null) {
                    final String errorMsg = e.getMessage();
                    mainHandler.post(() -> callback.onConnectionFailed(errorMsg));
                }
                
                retryConnect();
            }
        });
    }
    
    /**
     * 重试连接
     */
    private void retryConnect() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "重试连接，第 " + retryCount + " 次");
            
            // 延迟1秒后重试
            mainHandler.postDelayed(this::connect, 1000);
        } else {
            Log.e(TAG, "连接失败，已达到最大重试次数");
            
            if (callback != null) {
                mainHandler.post(() -> callback.onConnectionFailed("连接失败，已达到最大重试次数"));
            }
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        isRunning = false;
        isConnected = false;
        
        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
            }
            
            if (selector != null) {
                selector.close();
                selector = null;
            }
            
            Log.d(TAG, "断开连接");
        } catch (IOException e) {
            Log.e(TAG, "断开连接异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送数据
     */
    public void sendData(byte[] data, int msgId) {
        if (!isConnected || socketChannel == null) {
            Log.e(TAG, "发送数据失败: 未连接");
            return;
        }
        
        // 使用线程池在后台线程执行网络操作
        executorService.execute(() -> {
            try {
                buffer.clear();
                buffer.put(data);
                buffer.flip();
                
                while (buffer.hasRemaining()) {
                    socketChannel.write(buffer);
                }
                
                Log.d(TAG, "发送数据成功: " + new String(data));
            } catch (IOException e) {
                Log.e(TAG, "发送数据失败: " + e.getMessage());
                e.printStackTrace();
                
                // 如果发送失败，尝试重新连接
                isConnected = false;
                if (msgId == MessageManager.MSG_START_SESSION) {
                    retryConnect();
                } else {
                    if (callback != null) {
                        final String errorMsg = e.getMessage();
                        mainHandler.post(() -> callback.onConnectionFailed("发送数据失败: " + errorMsg));
                    }
                }
            }
        });
    }
    
    /**
     * 接收数据
     */
    private byte[] receiveData() {
        if (!isConnected || socketChannel == null) {
            Log.e(TAG, "接收数据失败: 未连接");
            return null;
        }
        
        try {
            buffer.clear();
            int readBytes = socketChannel.read(buffer);
            
            if (readBytes > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                
                Log.d(TAG, "接收数据成功: " + new String(data));
                return data;
            } else if (readBytes < 0) {
                // 连接已关闭
                Log.e(TAG, "连接已关闭");
                isConnected = false;
                
                if (callback != null) {
                    mainHandler.post(() -> callback.onConnectionFailed("连接已关闭"));
                }
                
                retryConnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "接收数据异常: " + e.getMessage());
            e.printStackTrace();
            
            isConnected = false;
            
            if (callback != null) {
                final String errorMsg = e.getMessage();
                mainHandler.post(() -> callback.onConnectionFailed("接收数据异常: " + errorMsg));
            }
            
            retryConnect();
        }
        
        return null;
    }
    
    /**
     * 连接监听器
     */
    private class ConnectionMonitor implements Runnable {
        @Override
        public void run() {
            try {
                while (isRunning) {
                    if (selector.select(1000) > 0) {
                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                        
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            
                            if (key.isConnectable()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                
                                // 完成连接
                                if (channel.finishConnect()) {
                                    isConnected = true;
                                    Log.d(TAG, "连接完成");
                                    
                                    // 注册读事件
                                    channel.register(selector, SelectionKey.OP_READ);
                                    
                                    if (callback != null) {
                                        mainHandler.post(() -> callback.onConnected());
                                    }
                                } else {
                                    Log.e(TAG, "连接失败");
                                    
                                    if (callback != null) {
                                        mainHandler.post(() -> callback.onConnectionFailed("连接失败"));
                                    }
                                    return;
                                }
                            }
                            
                            if (key.isReadable()) {
                                byte[] data = receiveData();
                                if (data != null && data.length > 0 && callback != null) {
                                    final byte[] finalData = data;
                                    mainHandler.post(() -> callback.onDataReceived(finalData));
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "连接监听异常: " + e.getMessage());
                e.printStackTrace();
                
                if (callback != null && isRunning) {
                    final String errorMsg = e.getMessage();
                    mainHandler.post(() -> callback.onConnectionFailed(errorMsg));
                }
            }
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
     * 连接回调接口
     */
    public interface ConnectionCallback {
        void onConnected();
        void onConnectionFailed(String reason);
        void onDataReceived(byte[] data);
    }
}