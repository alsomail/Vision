package com.also.vision.protocol;

/**
 * 行车记录仪通信协议规范
 * 本类仅作为协议文档，不包含实际代码逻辑
 */
public class ProtocolSpec {
    
    /**
     * 1. 通用协议格式
     * 
     * 所有通信基于JSON格式，通过TCP协议传输
     * 每个消息都包含msg_id字段，用于标识消息类型
     * 请求消息通常包含token字段(会话令牌)，由开始会话获得
     * 响应消息通常包含rval字段，0表示成功，其他值表示错误
     */
    
    /**
     * 2. 开始会话 (MSG_ID: 257)
     * 
     * 请求体:
     * {
     *   "msg_id": 257
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 257,
     *   "param": 12345,  // 会话令牌
     *   "rval": 0        // 0表示成功
     * }
     */
    
    /**
     * 3. 设置应用状态 (MSG_ID: 1)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 1,
     *   "param": null,
     *   "type": "app_status"
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 1,
     *   "rval": 0
     * }
     */
    
    /**
     * 4. 获取设备信息 (MSG_ID: 11)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 11,
     *   "param": null,
     *   "type": null
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 11,
     *   "camera_type": "F6S",
     *   "firm_ver": "V1.0.0",
     *   "firm_date": "2023-01-01",
     *   "param_version": "1.0",
     *   "serial_num": "SN12345678",
     *   "verify_code": "VC12345678"
     * }
     */
    
    /**
     * 5. 获取SD卡信息 (MSG_ID: 100)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 100,
     *   "param": null,
     *   "type": null
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 100,
     *   "rval": 0,
     *   "total_space": 32768,  // MB
     *   "free_space": 16384,   // MB
     *   "health_status": "good",
     *   "residual_life": "80%"
     * }
     * 
     * 特殊情况:
     * 当SD卡未插入时，total_space和free_space均为-1
     */
    
    /**
     * 6. 格式化SD卡 (MSG_ID: 102)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 102,
     *   "sd_status": "1",
     *   "format": "1"
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 102,
     *   "rval": 0  // 0表示成功
     * }
     */
    
    /**
     * 7. 拍照 (MSG_ID: 769)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 769,
     *   "param": null,
     *   "type": null
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 769,
     *   "rval": 0,
     *   "url": "http://192.168.42.1/DCIM/100MEDIA/IMG_0001.JPG",
     *   "thumbnailUrl": "http://192.168.42.1/DCIM/100MEDIA/IMG_0001_THUMB.JPG",
     *   "fileType": 1
     * }
     */
    
    /**
     * 8. 事件记录 (MSG_ID: 513)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 513,
     *   "param": null,
     *   "type": null
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 513,
     *   "rval": 0  // 0表示成功，-1表示SD卡已满，-2表示SD卡写入保护
     * }
     */
    
    /**
     * 9. 结束会话 (MSG_ID: 258)
     * 
     * 请求体:
     * {
     *   "token": 12345,
     *   "msg_id": 258,
     *   "param": null,
     *   "type": null
     * }
     * 
     * 响应体:
     * {
     *   "msg_id": 258,
     *   "rval": 0
     * }
     */
    
    /**
     * 10. 视频流协议
     * 
     * 视频流使用RTSP协议传输，URL格式为:
     * rtsp://192.168.42.1/live
     * 
     * 视频流参数:
     * - 编码: H.264
     * - 分辨率: 1920x1080 (可能根据设备型号不同而变化)
     * - 帧率: 30fps
     * - 比特率: 4Mbps
     */
} 