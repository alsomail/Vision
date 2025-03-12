package com.also.vision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.also.vision.model.DeviceInfo
import com.also.vision.model.SDCardInfo

class MainActivity : AppCompatActivity() {
    private var client: VisionClient? = null
    private var surfaceView: SurfaceView? = null
    private var btnConnect: Button? = null
    private var btnPlay: Button? = null
    private var btnSnapshot: Button? = null
    private var btnPhoto: Button? = null

    private var isConnected = false
    private var isPlaying = false

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // 检查权限
        showPermissionExplanation()


        // 初始化视图
        initViews()


        // 初始化客户端
        initClient()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surface_view)
        btnConnect = findViewById(R.id.btn_connect)
        btnPlay = findViewById(R.id.btn_play)
        btnSnapshot = findViewById(R.id.btn_snapshot)
        btnPhoto = findViewById(R.id.btn_photo)


        // 设置按钮点击事件
        btnConnect!!.setOnClickListener { v: View? ->
            if (!isConnected) {
                // 连接设备，但不立即改变按钮文本
                connectDevice()
                // 按钮文本将在连接成功的回调中更改
            } else {
                disconnectDevice()
                btnConnect!!.text = "连接"
            }
        }

        btnPlay!!.setOnClickListener { v: View? ->
            if (!isConnected) {
                showToast("请先连接设备")
                return@setOnClickListener
            }
            if (!isPlaying) {
                startVideoPlay()
                btnPlay!!.text = "停止"
            } else {
                stopVideoPlay()
                btnPlay!!.text = "播放"
            }
        }

        btnSnapshot!!.setOnClickListener { v: View? ->
            if (!isPlaying) {
                showToast("请先开始播放视频")
                return@setOnClickListener
            }
            takeSnapshot()
        }

        btnPhoto!!.setOnClickListener { v: View? ->
            if (!isConnected) {
                showToast("请先连接设备")
                return@setOnClickListener
            }
            takePhoto()
        }
    }

    private fun initClient() {
        client = VisionClient.getInstance()
        client?.init(this, VisionCallback())
        client?.initVideoPlayer(surfaceView)
    }

    private fun connectDevice() {
        client!!.connect()
    }

    private fun disconnectDevice() {
        client!!.disconnect()
        isConnected = false
        isPlaying = false
        btnPlay!!.text = "播放"
    }

    private fun startVideoPlay() {
        client!!.startVideoPlay()
    }

    private fun stopVideoPlay() {
        client!!.stopVideoPlay()
    }

    private fun takeSnapshot() {
        client!!.takeSnapshot()
    }

    private fun takePhoto() {
        client!!.takePhoto()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions() {
        // 基础权限列表（适用于所有 Android 版本）
        val basePermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        // 存储权限列表（根据 Android 版本区分）
        val storagePermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用细粒度媒体权限
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Android 12 及以下使用存储权限
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        // 合并权限列表
        val permissions = basePermissions + storagePermissions
        
        // 检查哪些权限需要申请
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        // 申请权限
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, 
                permissionsToRequest.toTypedArray(), 
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = ArrayList<String>()
            
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }
            
            if (deniedPermissions.isNotEmpty()) {
                // 检查是否有必要的权限被拒绝
                val criticalPermissionDenied = deniedPermissions.any { permission ->
                    when (permission) {
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE -> true
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE -> android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO -> android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                        else -> false
                    }
                }
                
                if (criticalPermissionDenied) {
                    showToast("应用需要相关权限才能正常运行")
                    finish()
                } else {
                    showToast("部分功能可能受限")
                }
            }
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (client != null) {
            client!!.disconnect()
        }
    }

    /**
     * 视频客户端回调
     */
    private inner class VisionCallback : VisionClient.VisionCallback {
        override fun onConnected() {
            runOnUiThread {
                isConnected = true
                btnConnect!!.text = "断开"
                showToast("设备连接成功")
                Log.d(TAG, "设备连接成功")
            }
        }

        override fun onConnectionFailed(reason: String) {
            runOnUiThread {
                isConnected = false
                btnConnect!!.text = "连接"
                showToast("连接失败: $reason")
                Log.e(TAG, "连接失败: $reason")
            }
        }

        override fun onSessionStarted() {
            Log.d(TAG, "会话开始")
        }

        override fun onSessionFailed(reason: String) {
            runOnUiThread {
                showToast("会话失败: $reason")
                Log.e(TAG, "会话失败: $reason")
            }
        }

        override fun onDeviceInfoReceived(deviceInfo: DeviceInfo) {
            Log.d(TAG, "设备信息: $deviceInfo")
        }

        override fun onSDCardInfoReceived(sdInfo: SDCardInfo) {
            Log.d(TAG, "SD卡信息: $sdInfo")
        }

        override fun onVideoPlayStarted() {
            runOnUiThread {
                isPlaying = true
                showToast("视频开始播放")
                Log.d(TAG, "视频开始播放")
            }
        }

        override fun onVideoPlayStopped() {
            runOnUiThread {
                isPlaying = false
                btnPlay!!.text = "播放"
                showToast("视频停止播放")
                Log.d(TAG, "视频停止播放")
            }
        }

        override fun onVideoPlayError(error: String) {
            runOnUiThread {
                isPlaying = false
                btnPlay!!.text = "播放"
                showToast("视频播放错误: $error")
                Log.e(TAG, "视频播放错误: $error")
            }
        }

        override fun onPhotoTaken() {
            runOnUiThread {
                showToast("拍照成功")
                Log.d(TAG, "拍照成功")
            }
        }

        override fun onPhotoFailed(reason: String) {
            runOnUiThread {
                showToast("拍照失败: $reason")
                Log.e(TAG, "拍照失败: $reason")
            }
        }

        override fun onSnapshotTaken(path: String) {
            runOnUiThread {
                showToast("截图成功: $path")
                Log.d(TAG, "截图成功: $path")
            }
        }

        override fun onEventRecorded() {
            runOnUiThread {
                showToast("事件记录成功")
                Log.d(TAG, "事件记录成功")
            }
        }

        override fun onEventRecordFailed(reason: String) {
            runOnUiThread {
                showToast("事件记录失败: $reason")
                Log.e(TAG, "事件记录失败: $reason")
            }
        }

        override fun onSDCardFormatted() {
            runOnUiThread {
                showToast("SD卡格式化成功")
                Log.d(TAG, "SD卡格式化成功")
            }
        }

        override fun onSDCardFormatFailed(reason: String) {
            runOnUiThread {
                showToast("SD卡格式化失败: $reason")
                Log.e(TAG, "SD卡格式化失败: $reason")
            }
        }

        override fun onMessageReceived(msgId: Int, result: Int, content: String) {
            Log.d(TAG, "收到消息: ID=$msgId, 结果=$result, 内容=$content")
        }
    }

    private fun showPermissionExplanation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("权限申请")
            .setMessage("此应用需要以下权限才能正常工作：\n" +
                    "• 网络权限：连接到行车记录仪设备\n" +
                    "• 存储权限：保存截图和照片")
            .setPositiveButton("确定") { _, _ ->
                checkPermissions()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }
}