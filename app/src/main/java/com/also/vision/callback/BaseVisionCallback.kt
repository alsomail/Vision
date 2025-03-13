package com.also.vision.callback

import com.also.vision.VisionClient
import com.also.vision.model.DeviceFile
import com.also.vision.model.DeviceInfo
import com.also.vision.model.SDCardInfo

/**
 * VisionCallback 的基础实现，提供所有方法的默认空实现
 * 子类只需要重写它们关心的方法
 */
abstract class BaseVisionCallback : VisionClient.VisionCallback {
    override fun onConnected() {}
    
    override fun onConnectionFailed(reason: String) {}
    
    override fun onSessionStarted() {}
    
    override fun onSessionFailed(reason: String) {}
    
    override fun onDeviceInfoReceived(deviceInfo: DeviceInfo) {}
    
    override fun onSDCardInfoReceived(sdInfo: SDCardInfo) {}
    
    override fun onVideoPlayStarted() {}
    
    override fun onVideoPlayStopped() {}
    
    override fun onVideoPlayError(error: String) {}
    
    override fun onPhotoTaken() {}
    
    override fun onPhotoFailed(reason: String) {}
    
    override fun onSnapshotTaken(path: String) {}
    
    override fun onEventRecorded() {}
    
    override fun onEventRecordFailed(reason: String) {}
    
    override fun onSDCardFormatted() {}
    
    override fun onSDCardFormatFailed(reason: String) {}
    
    override fun onFileListReceived(files: List<DeviceFile>, total: Int) {}
    
    override fun onFileListFailed(reason: String) {}
    
    override fun onFileDeleted() {}
    
    override fun onFileDeleteFailed(reason: String) {}
    
    override fun onFileDownloadUrl(url: String) {}
    
    override fun onFileDownloadFailed(reason: String) {}
    
    override fun onMessageReceived(msgId: Int, result: Int, content: String) {}
} 