package com.also.vision;

/**
 * 行车记录仪通讯协议文档
 * 基于MainActivity, CameraFragment, SettingActivity等代码分析
 */
public class Protocol {
    // 设备网络配置
    public static final String DEFAULT_DEVICE_IP = "192.168.42.1";    // 默认设备IP
    public static final int DEFAULT_PORT = 7878;                       // 默认设备端口
    public static final int LOCAL_PORT_1 = 9695;                       // 本地端口1
    public static final int LOCAL_PORT_2 = 9696;                       // 本地端口2
    
    /**
     * 命令发送机制：
     * 
     * 1. 命令构建(ProtocolManager.sendCommand)：
     *    - 创建SendParamsJson对象，设置token, msg_id, param, type
     *    - 转换为JSON字符串
     *    - 调用SocketManager.sendData发送字节数据
     * 
     * 2. 命令队列管理(CommandQueue)：
     *    - 命令被包装为SendDataModel对象
     *    - 使用Stack<SendDataModel>存储命令
     *    - 对于相同msg_id的命令会进行去重处理
     *    - 队列最多保存10个命令(maxQueueSize=10)
     * 
     * 3. 异步发送(SendCommandThread)：
     *    - 线程等待队列中有命令
     *    - 从栈顶取出命令(LIFO顺序)
     *    - 使用ByteBuffer准备数据
     *    - 通过SocketChannel发送到设备
     *    - 发送频率控制：相同msg_id的命令至少间隔500ms
     * 
     * 4. Socket通信(SocketManager)：
     *    - 使用Java NIO的SocketChannel
     *    - 非阻塞模式，配置超时为9000ms
     *    - 支持连接/重连/数据发送/接收
     *    - 接收到数据后通过回调接口通知应用层
     */

    /**
     * 开始会话 (257)
     * 请求体: {"msg_id":257}
     * 响应体: {"rval":0,"msg_id":257,"param":12345} - param为返回的token值
     * 
     * 说明: 第一步必须调用此接口获取token，后续所有请求都需要带上token
     * 成功后应该立即调用MSG_GET_APP_STATUS, MSG_GET_FILE_LIST, MSG_GET_SD_INFO和MSG_GET_DEVICE_INFO
     */
    public static final int MSG_START_SESSION = 257;
    
    /**
     * 结束会话 (258)
     * 请求体: {"token":12345,"msg_id":258}
     * 响应体: {"rval":0,"msg_id":258}
     * 
     * 说明: 断开连接前需要调用此接口
     */
    public static final int MSG_STOP_SESSION = 258;
    
    /**
     * 获取设备信息 (11)
     * 请求体: {"token":12345,"msg_id":11}
     * 响应体: {
     *   "rval":0,
     *   "msg_id":11,
     *   "camera_type":"F9",       // 设备型号
     *   "firm_ver":"V1.0.0",      // 固件版本
     *   "firm_date":"2022-01-01", // 固件日期
     *   "param_version":"1.0",    // 参数版本
     *   "serial_num":"12345678",  // 序列号
     *   "verify_code":"ABCDEF"    // 验证码
     * }
     * 
     * 调用方式: ProtocolManager.getInstance().sendCommand(11, null, null);
     */
    public static final int MSG_GET_DEVICE_INFO = 11;
    
    /**
     * 获取SD卡信息 (100)
     * 请求体: {"token":12345,"msg_id":100}
     * 响应体: {
     *   "rval":0,
     *   "msg_id":100,
     *   "total_space":32000,    // 总空间(MB)
     *   "free_space":16000,     // 可用空间(MB)
     *   "sd_status":"normal"    // SD卡状态，可能值: "normal", "locked", "unlocked", "nonsupport"
     * }
     * 
     * 说明: 
     * - 如果无SD卡，则total_space和free_space都为-1
     * - sd_status为"locked"时表示SD卡被锁定，需要密码解锁
     * - 解锁SD卡使用MSG_ID=104的命令，发送sd_passwd参数
     */
    public static final int MSG_GET_SD_INFO = 100;
    
    /**
     * 获取应用状态 (1)
     * 请求体: {"token":12345,"msg_id":1,"type":"app_status"}
     * 响应体: {
     *   "rval":0,
     *   "msg_id":1,
     *   "type":"app_status",
     *   "param":"record"  // 可能值: "record", "capture", "idle"
     * }
     * 
     * 说明: 
     * - "record" = 正在录像
     * - "capture" = 正在拍照
     * - "idle" = 空闲状态
     */
    public static final int MSG_GET_APP_STATUS = 1;
    
    /**
     * 获取系统时间 (1)
     * 请求体: {"token":12345,"msg_id":1,"type":"date_time"}
     * 响应体: {
     *   "rval":0,
     *   "msg_id":1,
     *   "type":"date_time",
     *   "param":"2022-01-01 12:00:00"  // 当前系统时间
     * }
     */
    public static final int MSG_GET_DATE_TIME = 1;
    
    /**
     * 设置参数 (2)
     * 请求体: {
     *   "token":12345,
     *   "msg_id":2,
     *   "type":"parameter_name",  // 参数名称
     *   "param":"parameter_value" // 参数值
     * }
     * 响应体: {"rval":0,"msg_id":2,"type":"parameter_name"}
     * 
     * 常用参数名称:
     * - video_resolution: 视频分辨率 ("1080p", "720p"等)
     * - video_quality: 视频质量 ("high", "medium", "low")
     * - loop_record: 循环录制 ("off", "3min", "5min", "10min")
     * - meter_mode: 测光模式 ("average", "center", "spot")
     * - record_audio: 录音设置 ("on", "off")
     * - auto_power_off: 自动关机 ("off", "3min", "5min", "10min")
     * - language: 系统语言 ("cn", "en", "jp"等)
     * - dev_sound: 声音提示 ("on", "off")
     * 
     * 调用方式: ProtocolManager.getInstance().sendCommand(2, "720p", "video_resolution");
     */
    public static final int MSG_SET_PARAM = 2;
    
    /**
     * 获取设置项可选值 (9)
     * 请求体: {"token":12345,"msg_id":9,"type":"parameter_name"}
     * 响应体: {
     *   "rval":0,
     *   "msg_id":9,
     *   "type":"parameter_name",
     *   "param":[  // 可选值数组
     *     {"1080p":0},
     *     {"720p":1}
     *   ],
     *   "permission":"settable", // 是否可设置
     *   "default":"1080p"        // 默认值
     * }
     * 
     * 说明: 
     * - 不同参数返回的param数组结构可能有差异
     * - permission为"settable"表示可设置，为"readonly"表示只读
     * - 大部分参数可通过ParamDisplayHelper类获取本地化显示名称
     */
    public static final int MSG_GET_PARAM_OPTIONS = 9;
    
    /**
     * 获取文件列表 (1280)
     * 请求体: {
     *   "token":12345,
     *   "msg_id":1280,
     *   "param":0,           // 分页起始索引
     *   "type":"video",      // 文件类型: "video", "photo", "event_video", "event_photo"
     *   "pageSize":100       // 每页数量
     * }
     * 响应体: {
     *   "rval":0,
     *   "msg_id":1280,
     *   "totalFileNum":150,  // 该类型文件总数
     *   "param":0,           // 当前页起始索引
     *   "listing":[          // 文件列表
     *     {"name":"20220101_120000.MP4"},
     *     {"name":"20220101_120500.MP4"}
     *   ]
     * }
     * 
     * 说明:
     * - 使用ProtocolManager.parseFileList方法解析响应
     * - 文件名格式通常为"YYYYMMDD_HHMMSS.EXT"
     * - 缩略图文件与视频文件同名但扩展名为.thm
     */
    public static final int MSG_GET_FILE_LIST = 1280;
    
    /**
     * 删除文件 (1281)
     * 请求体: {
     *   "token":12345,
     *   "msg_id":1281,
     *   "param":"20220101_120000.MP4" // 文件名
     * }
     * 响应体: {"rval":0,"msg_id":1281}
     */
    public static final int MSG_DELETE_FILE = 1281;
    
    /**
     * 删除所有文件 (1282)
     * 请求体: {"token":12345,"msg_id":1282}
     * 响应体: {"rval":0,"msg_id":1282}
     * 
     * 说明: 谨慎使用，该操作会删除设备上所有媒体文件
     */
    public static final int MSG_DELETE_ALL_FILES = 1282;
    
    /**
     * 开始录像 (513)
     * 请求体: {"token":12345,"msg_id":513}
     * 响应体: {"rval":0,"msg_id":513}
     */
    public static final int MSG_START_RECORD = 513;
    
    /**
     * 停止录像 (514)
     * 请求体: {"token":12345,"msg_id":514}
     * 响应体: {"rval":0,"msg_id":514}
     */
    public static final int MSG_STOP_RECORD = 514;
    
    /**
     * 拍照 (769)
     * 请求体: {"token":12345,"msg_id":769}
     * 响应体: {"rval":0,"msg_id":769}
     */
    public static final int MSG_TAKE_PHOTO = 769;
    
    /**
     * WIFI 设置 (1554)
     * 请求体: {
     *   "token":12345,
     *   "msg_id":1554,
     *   "ssid":"wifi_name",     // WIFI名称
     *   "passwd":"wifi_pwd"     // WIFI密码
     * }
     * 响应体: {"rval":0,"msg_id":1554}
     * 
     * 调用方式: ProtocolManager.getInstance().setWiFiConfig(msgId, "wifi_name", "wifi_pwd");
     */
    public static final int MSG_SET_WIFI = 1554;
    
    /**
     * 设备通知 (7)
     * 请求体: 无(设备主动推送)
     * 响应体: {
     *   "msg_id":7,
     *   "type":"notify_type", // 通知类型，见下面的常量定义
     *   "param":"param_value" // 通知参数
     * }
     * 
     * 说明:
     * - 设备会在某些状态变化时主动发送通知
     * - 通过ProtocolManager.parseNotification解析通知内容
     */
    public static final int MSG_NOTIFY = 7;
    
    // 状态码
    public static final int STATUS_SUCCESS = 0;             // 成功
    public static final int STATUS_PARAM_ERROR = -1;        // 参数错误
    public static final int STATUS_CONNECT_FAILED = -9;     // 连接失败
    public static final int STATUS_INVALID_TOKEN = -4;      // 无效的Token
    public static final int STATUS_SESSION_START_FAIL = -3; // 会话启动失败
    public static final int STATUS_REACH_MAX_CLIENT = -5;   // 达到最大客户端数
    public static final int STATUS_JSON_PACKAGE_ERROR = -7; // JSON包错误
    public static final int STATUS_JSON_PACKAGE_TIMEOUT = -8; // JSON包超时
    public static final int STATUS_JSON_SYNTAX_ERROR = -9;  // JSON语法错误
    public static final int STATUS_INVALID_OPERATION = -14; // 无效操作
    public static final int STATUS_INVALID_OPTION_VALUE = -13; // 无效的选项值
    public static final int STATUS_HDMI_INSERTED = -16;     // HDMI已插入
    public static final int STATUS_NO_MORE_SPACE = -17;     // 没有更多空间
    public static final int STATUS_CARD_PROTECTED = -18;    // 卡被保护
    public static final int STATUS_NO_MORE_MEMORY = -19;    // 没有更多内存
    public static final int STATUS_PIV_NOT_ALLOWED = -20;   // 不允许PIV
    public static final int STATUS_SYSTEM_BUSY = -21;       // 系统忙
    public static final int STATUS_APP_NOT_READY = -22;     // 应用未就绪
    public static final int STATUS_OPERATION_UNSUPPORTED = -23; // 不支持的操作
    public static final int STATUS_INVALID_TYPE = -24;      // 无效类型
    
    // 通知类型 (msg_id=7的响应中的type字段可能值)
    public static final String NOTIFY_SD_FULL = "SD_full";           // SD卡已满
    public static final String NOTIFY_SD_NO = "SD_no";               // 无SD卡
    public static final String NOTIFY_SD_RM = "SD_rm";               // SD卡移除
    public static final String NOTIFY_SD_INSERT = "SD_insert";       // SD卡插入
    public static final String NOTIFY_SD_ERR = "SD_err";             // SD卡错误
    public static final String NOTIFY_SD_ABNORMAL = "SD_abnormal";   // SD卡异常
    public static final String NOTIFY_SD_LITTLE = "SD_little";       // SD卡剩余空间不足
    public static final String NOTIFY_SD_UNFORMAT = "SD_unformat";   // SD卡未格式化
    public static final String NOTIFY_APP_STATUS = "app_status";     // 应用状态
    public static final String NOTIFY_DATE_TIME = "date_time";       // 日期时间
    public static final String NOTIFY_UPGRADE_STATUS = "upgrade_status"; // 升级状态
    public static final String NOTIFY_APP_UPGRADE = "app_upgrade";   // 应用升级
    public static final String NOTIFY_SYSTEM_REBOOT = "system_reboot"; // 系统重启
    public static final String NOTIFY_UPGRADE_FILE = "upgrade_file"; // 升级文件
    public static final String NOTIFY_DISCONNECT_SHUTDOWN = "disconnect_shutdown"; // 断开连接并关机
    
    /**
     * 设置流程:
     * 1. 建立Socket连接(192.168.42.1:7878)
     * 2. 发送MSG_START_SESSION(257)消息获取token (sendCommand(257, null, null))
     * 3. 使用token进行参数设置和获取:
     *    - 获取应用状态: sendCommand(1, null, "app_status")
     *    - 获取文件列表: sendCommand(1280, null, null)
     *    - 获取SD卡信息: sendCommand(100, null, null)
     *    - 获取设备信息: sendCommand(11, null, null)
     * 4. 设置参数:
     *    - 获取参数选项: sendCommand(9, null, paramName)
     *    - 设置参数值: sendCommand(2, paramValue, paramName)
     * 5. 心跳保持(重置计时器): clearBeatTime()
     * 6. 断开连接前发送MSG_STOP_SESSION(258)
     * 
     * 注意事项:
     * - 错误码解析见ProtocolManager.getErrorMessage(Context, int)方法
     * - 参数中文显示见ParamDisplayHelper类
     * - ProtocolManager.getInstance()返回单例实例，用于发送所有命令
     */

     /**
     * 协议通信流程详解:
     * 
     * 1. Socket连接建立:
     *    - ProtocolManager.initConnection(context)初始化连接
     *    - ConnectionThread线程执行实际连接操作
     *    - SocketManager处理底层socket通信
     * 
     * 2. 命令发送流程:
     *    - 应用层调用ProtocolManager.sendCommand(msg_id, param, type)
     *    - 命令被包装为JSON并加入SendDataModel队列
     *    - SendCommandThread线程异步从队列取出命令发送
     *    - 命令去重和限流(相同命令500ms内只发一次)
     *    - ByteBuffer写入SocketChannel
     * 
     * 3. 命令接收流程:
     *    - SocketManager线程监听socket读事件
     *    - 接收数据解析为字符串
     *    - 通过CommandCallback回调接口返回给应用层
     *    - 应用层根据msg_id路由到不同处理方法
     * 
     * 4. 心跳机制:
     *    - HeartbeatThread线程每秒执行一次心跳检测
     *    - 每4秒自动获取一次日期时间(msg_id=1, type="date_time")
     *    - 如果10秒没有响应触发重连
     *    - HeartBeatService服务也可独立发送心跳
     * 
     * 5. 队列管理:
     *    - CommandQueue管理命令队列(Stack<SendDataModel>)
     *    - 实现了命令去重、队列上限控制
     *    - 保证高优先级命令先发送(LIFO特性)
     *    - 队列最多保存10个命令
     */
}