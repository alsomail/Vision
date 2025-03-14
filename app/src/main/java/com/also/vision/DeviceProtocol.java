package com.also.vision.protocol;

/**
 * 设备通信协议常量定义
 * 根据海康威视行车记录仪通信协议整理
 */
public class DeviceProtocol {
    // 会话管理
    /**
     * 开始会话/获取令牌
     * 请求体: {"msg_id":257}
     * 响应体: {"msg_id":257,"param":12345,"rval":0}
     */
    public static final int MSG_START_SESSION = 257;
    
    /**
     * 结束会话
     * 请求体: {"token":12345,"msg_id":258}
     * 响应体: {"msg_id":258,"rval":0}
     */
    public static final int MSG_END_SESSION = 258;
    
    /**
     * 应用状态
     * 请求体: {"token":12345,"msg_id":1,"type":"app_status"}
     * 响应体: {"msg_id":1,"rval":0}
     */
    public static final int MSG_APP_STATUS = 1;
    
    // 参数设置
    /**
     * 设置参数
     * 请求体: {"token":12345,"msg_id":2,"param":"参数值","type":"参数名"}
     * 响应体: {"msg_id":2,"rval":0}
     * 
     * 支持的参数类型:
     * - video_resolution: 视频分辨率
     *   - 2: 1296P@25fps
     *   - 3: 1296P@30fps
     *   - 4: 1080P@30fps
     *   - 5: 720P@60fps
     *   - 6: 720P@30fps
     *   - 7: 1080P@25fps
     *   - 8: 720P@50fps
     *   - 9: 720P@25fps
     * 
     * - video_aspectratio: 视频宽高比
     * - video_encodemode: 编码格式
     * - video_quality: 视频品质
     *   - 10: 好
     *   - 11: 极好
     *   - 12: 较好
     * 
     * - video_standard: 视频制式
     * - video_log_duration: 录像时长
     *   - 18: 1分钟
     *   - 19: 2分钟
     *   - 20: 3分钟
     *   - 21: 5分钟
     *   - 22: 10分钟
     *   - 23: 30分钟
     *   - 24: 60分钟
     * 
     * - g_sensor_sensitity: G-sensor灵敏度
     *   - 32: 低
     *   - 33: 中
     *   - 34: 高
     * 
     * - language: 语言
     * - ev_bias_idx: 曝光补偿
     * - poweroff_delay: 延时关机
     * - lcd_autopoweroff: 屏幕自动关闭
     * - adas_mode: ADAS模式
     *   - 2: 全天模式
     *   - 3: 白天模式
     *   - 4: 夜间模式
     * 
     * - motion_detect: 移动侦测
     * - wdr_switch: 宽动态开关
     * - traffic_light_remind: 交通灯提示
     * - front_start_remind: 前车起步提示
     * - speed_limit_recognition: 速度限制识别
     * - dev_DV_mode: DV模式
     * - parking_monitor: 停车监控
     * - usb_device_pattern: USB设备模式
     * 
     * 开关类参数值:
     * - 13: 关闭
     * - 14: 开启
     */
    public static final int MSG_SET_PARAM = 2;
    
    /**
     * 重启设备
     * 请求体: {"token":12345,"msg_id":3}
     * 响应体: {"msg_id":3,"rval":0}
     */
    public static final int MSG_DEVICE_REBOOT = 3;
    
    /**
     * 格式化SD卡
     * 请求体: {"token":12345,"msg_id":4}
     * 响应体: {"msg_id":4,"rval":0}
     */
    public static final int MSG_FORMAT_SD = 4;
    
    /**
     * 升级通知
     * 响应体: {"msg_id":9,"type":"upgrade_progress","param":"50"} // 升级进度50%
     */
    public static final int MSG_UPGRADE_NOTIFICATION = 9;
    
    /**
     * 获取设备信息
     * 请求体: {"token":12345,"msg_id":11}
     * 响应体: {"msg_id":11,"camera_type":"F6S","firm_ver":"1.0.0","firm_date":"2023-01-01","param_version":"1.0","serial_num":"SN12345678","verify_code":"VC12345678"}
     */
    public static final int MSG_GET_DEVICE_INFO = 11;
    
    /**
     * 重置设备（恢复出厂设置）
     * 请求体: {"token":12345,"msg_id":12}
     * 响应体: {"msg_id":12,"rval":0}
     */
    public static final int MSG_RESET_DEVICE = 12;
    
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
     * 设备重启完成后执行
     * 请求体: {"token":12345,"msg_id":32}
     * 响应体: 无
     */
    public static final int MSG_REBOOT_COMPLETE = 32;
    
    // SD卡相关
    /**
     * 获取SD卡信息
     * 请求体: {"token":12345,"msg_id":100}
     * 响应体: {"msg_id":100,"rval":0,"total_space":32768,"free_space":16384,"health_status":"good","residual_life":"80%"}
     */
    public static final int MSG_GET_SD_INFO = 100;
    
    /**
     * 格式化SD卡（替代接口）
     * 请求体: {"token":12345,"msg_id":102,"sd_status":"1","format":"1"}
     * 响应体: {"msg_id":102,"rval":0}
     */
    public static final int MSG_SD_FORMAT_ALT = 102;
    
    // 文件管理
    /**
     * 获取文件列表
     * 请求体: {"token":12345,"msg_id":1283,"param":"video","offset":0,"count":10}
     * 响应体: {"msg_id":1283,"rval":0,"totalFileNum":100,"param":[{"path":"A:\\video1.mp4","size":12345,"time":"2023-05-01 12:00:00","duration":60,"locked":0},{"path":"A:\\video2.mp4"...}]}
     */
    public static final int MSG_GET_FILE_LIST = 1283;
    
    /**
     * 删除文件
     * 请求体: {"token":12345,"msg_id":1284,"param":"A:\\video1.mp4"}
     * 响应体: {"msg_id":1284,"rval":0}
     */
    public static final int MSG_DELETE_FILE = 1284;
    
    /**
     * 下载文件
     * 请求体: {"token":12345,"msg_id":1285,"param":"A:\\video1.mp4"}
     * 响应体: {"msg_id":1285,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/video1.mp4"}
     */
    public static final int MSG_DOWNLOAD_FILE = 1285;
    
    // 录像和拍照
    /**
     * 紧急事件记录
     * 请求体: {"token":12345,"msg_id":513,"type":"emergency"}
     * 响应体: {"msg_id":513,"rval":0,"file_name":"EMR_0001.MP4"}
     */
    public static final int MSG_EVENT_RECORD = 513;
    
    /**
     * 拍照
     * 请求体: {"token":12345,"msg_id":769}
     * 响应体: {"msg_id":769,"rval":0,"url":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG","thumbnailUrl":"http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG","fileType":2}
     */
    public static final int MSG_TAKE_PHOTO = 769;
    
    /**
     * 开始录像
     * 请求体: {"token":12345,"msg_id":770}
     * 响应体: {"msg_id":770,"rval":0}
     */
    public static final int MSG_START_RECORD = 770;
    
    /**
     * 停止录像
     * 请求体: {"token":12345,"msg_id":771}
     * 响应体: {"msg_id":771,"rval":0}
     */
    public static final int MSG_STOP_RECORD = 771;
    
    // 系统命令
    /**
     * 获取所有参数
     * 请求体: {"token":12345,"msg_id":1280}
     * 响应体: {"msg_id":1280,"rval":0,"param":[{"type":"resolution","permission":"rw","value":"1080P30","options":[...]},{"type":"video_quality"...}]}
     */
    public static final int MSG_GET_ALL_PARAMS = 1280;
    
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
    
    /**
     * 获取参数选项
     * 请求体: {"token":12345,"msg_id":257}
     * 响应体: {"msg_id":257,"param":"参数名","options":[{"option":"选项值","desc":"选项描述"},...]}
     */
    public static final int MSG_GET_OPTIONS = 257;
}