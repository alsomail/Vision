package com.also.vision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
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
    private var btnBrowseFiles: Button? = null
    private var btnBrowseVideos: Button? = null
    private var btnBrowsePhotos: Button? = null

    private var isConnected = false
    private var isPlaying = false

    private var loadingDialog: AlertDialog? = null
    private var connectionTimeoutHandler = Handler(Looper.getMainLooper())
    private val CONNECTION_TIMEOUT = 5000L // 5秒超时

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查权限
        showPermissionExplanation()

        // 初始化视图
        initViews()

        // 初始化客户端
        initClient()

        // 检查WiFi连接
        checkWifiConnection()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surface_view)
        btnConnect = findViewById(R.id.btn_connect)
        btnPlay = findViewById(R.id.btn_play)
        btnSnapshot = findViewById(R.id.btn_snapshot)
        btnPhoto = findViewById(R.id.btn_photo)
        btnBrowseFiles = findViewById(R.id.btn_browse_files)
        btnBrowseVideos = findViewById(R.id.btn_browse_videos)
        btnBrowsePhotos = findViewById(R.id.btn_browse_photos)

        // 设置按钮点击事件
        btnConnect!!.setOnClickListener { v: View? ->
            if (!isConnected) {
                // 先检查WiFi连接状态
                if (!isWifiConnected()) {
                    showNoWifiDialog()
                    return@setOnClickListener
                }
                
                // 检查是否连接到设备WiFi
                DeviceConnection.isConnectedToDeviceWifiAsync(this, object : DeviceConnection.DeviceConnectionCallback {
                    override fun onResult(isConnected: Boolean) {
                        if (!isConnected) {
                            showNoDeviceDialog()
                        } else {
                            // 连接设备，但不立即改变按钮文本
                            connectDevice()
                            // 按钮文本将在连接成功的回调中更改
                        }
                    }
                })
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

        btnBrowseFiles?.setOnClickListener {
            openFileList("all")
        }

        btnBrowseVideos?.setOnClickListener {
            openFileList("video")
        }

        btnBrowsePhotos?.setOnClickListener {
            openFileList("photo")
        }
    }

    private fun initClient() {
        client = VisionClient.getInstance()
        client?.init(this, VisionCallback())
        client?.initVideoPlayer(surfaceView)
    }

    private fun handleConnectionTimeout() {
        dismissLoadingDialog()
        showToast("连接超时，请检查设备是否开启")

        // 确保清理所有连接资源
        if (client != null) {
            client!!.disconnect()
        }

        // 重置连接状态
        isConnected = false
        btnConnect!!.text = "连接"
    }

    private fun connectDevice() {
        // 显示加载对话框
        showLoadingDialog("正在连接设备...")

        // 设置连接超时
        connectionTimeoutHandler.postDelayed({
            handleConnectionTimeout()
        }, CONNECTION_TIMEOUT)

        // 连接设备
        client!!.connect()
    }

    private fun disconnectDevice() {
        dismissLoadingDialog()
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
                dismissLoadingDialog() // 连接成功，关闭加载对话框
                isConnected = true
                btnConnect!!.text = "断开"
                showToast("设备连接成功")
                Log.d(TAG, "设备连接成功")
            }
        }

        override fun onConnectionFailed(reason: String) {
            runOnUiThread {
                dismissLoadingDialog() // 连接失败，关闭加载对话框
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

        override fun onFileListReceived(fileList: List<DeviceFile>, total: Int) {
            Log.d(TAG, "收到文件列表: ${fileList.size}个文件，共${total}个")
        }

        override fun onFileListFailed(reason: String) {
            runOnUiThread {
                showToast("获取文件列表失败: $reason")
                Log.e(TAG, "获取文件列表失败: $reason")
            }
        }

        override fun onFileDeleted() {
            runOnUiThread {
                showToast("文件删除成功")
                Log.d(TAG, "文件删除成功")
            }
        }

        override fun onFileDeleteFailed(reason: String) {
            runOnUiThread {
                showToast("文件删除失败: $reason")
                Log.e(TAG, "文件删除失败: $reason")
            }
        }

        override fun onFileDownloadUrl(url: String) {
            Log.d(TAG, "文件下载URL: $url")
        }

        override fun onFileDownloadFailed(reason: String) {
            runOnUiThread {
                showToast("获取文件下载链接失败: $reason")
                Log.e(TAG, "获取文件下载链接失败: $reason")
            }
        }
    }

    private fun showPermissionExplanation() {
        // 检查是否有需要申请的权限
        val permissionsToRequest = getPermissionsToRequest()

        // 只有当确实需要申请权限时才显示弹窗
        if (permissionsToRequest.isNotEmpty()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("权限申请")
                .setMessage(
                    "此应用需要以下权限才能正常工作：\n" +
                        "• 网络权限：连接到行车记录仪设备\n" +
                        "• 存储权限：保存截图和照片"
                )
                .setPositiveButton("确定") { _, _ ->
                    requestPermissions(permissionsToRequest)
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun getPermissionsToRequest(): Array<String> {
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

        return permissionsToRequest.toTypedArray()
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            PERMISSION_REQUEST_CODE
        )
    }

    private fun checkWifiConnection() {
        // 首先检查是否连接到WiFi
        if (!isWifiConnected()) {
            showNoWifiDialog()
        } 
        // 如果连接了WiFi，异步检查设备连接
        else {
            DeviceConnection.isConnectedToDeviceWifiAsync(this, object : DeviceConnection.DeviceConnectionCallback {
                override fun onResult(isConnected: Boolean) {
                    if (!isConnected) {
                        showNoDeviceDialog()
                    }
                }
            })
        }
    }

    /**
     * 检查是否连接到任何WiFi
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null &&
            activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI &&
            activeNetworkInfo.isConnected
    }

    /**
     * 显示未连接WiFi的对话框
     */
    private fun showNoWifiDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("WiFi未连接")
            .setMessage("请连接到WiFi网络才能使用此应用")
            .setPositiveButton("去连接") { _, _ ->
                openWifiSettings()
            }
            .setNegativeButton("取消") { _, _ ->
                showToast("未连接WiFi，部分功能将无法使用")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 显示未检测到设备的对话框
     */
    private fun showNoDeviceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("未检测到设备")
            .setMessage("已连接WiFi，但未检测到行车记录仪设备(192.168.42.1)，请确保连接到正确的WiFi网络")
            .setPositiveButton("去连接") { _, _ ->
                openWifiSettings()
            }
            .setNegativeButton("取消") { _, _ ->
                showToast("未连接到设备，部分功能将无法使用")
            }
            .setCancelable(false)
            .show()
    }

    private fun openWifiSettings() {
        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }

    // 修改onResume方法，同样使用异步检查
    override fun onResume() {
        super.onResume()

        // 如果已经有权限，检查WiFi连接
        if (hasRequiredPermissions()) {
            if (isWifiConnected()) {
                DeviceConnection.isConnectedToDeviceWifiAsync(this, object : DeviceConnection.DeviceConnectionCallback {
                    override fun onResult(isConnected: Boolean) {
                        if (isConnected && !isConnected) {
                            // 如果WiFi已连接且设备可达，但设备未连接，尝试连接设备
                            connectDevice()
                        }
                    }
                })
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun showLoadingDialog(message: String) {
        dismissLoadingDialog() // 确保没有其他对话框在显示

        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        val tvMessage = view.findViewById<TextView>(R.id.tv_loading_message)
        tvMessage.text = message

        val builder = AlertDialog.Builder(this)
        builder.setView(view)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
        // 移除超时回调
        connectionTimeoutHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 打开文件浏览界面
     * @param fileType 文件类型，"all"=所有文件，"video"=视频文件，"photo"=图片文件
     */
    private fun openFileList(fileType: String) {
        if (!isConnected) {
            showToast("请先连接设备")
            return
        }
        
        val intent = Intent(this, FileListActivity::class.java)
        intent.putExtra("fileType", fileType)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }
}