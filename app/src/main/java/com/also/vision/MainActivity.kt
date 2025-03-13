package com.also.vision

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.also.vision.adapter.TabPagerAdapter
import com.also.vision.model.DeviceFile
import com.also.vision.model.DeviceInfo
import com.also.vision.model.SDCardInfo
import com.also.vision.ui.LiveFragment
import com.also.vision.ui.PhotoFragment
import com.also.vision.ui.VideoFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.widget.LinearLayout
import com.also.vision.callback.BaseVisionCallback

class MainActivity : AppCompatActivity() {
    private var client: VisionClient? = null
    private var btnConnect: Button? = null

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabPagerAdapter: TabPagerAdapter

    private var liveFragment: LiveFragment? = null
    private var videoFragment: VideoFragment? = null
    private var photoFragment: PhotoFragment? = null

    private var isConnected = false

    private var loadingDialog: AlertDialog? = null
    private var connectionTimeoutHandler = Handler(Looper.getMainLooper())
    private val CONNECTION_TIMEOUT = 5000L // 5秒超时

    override fun onCreate(savedInstanceState: Bundle?) {
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
        btnConnect = findViewById(R.id.btnConnect)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // 初始化Fragment
        liveFragment = LiveFragment.newInstance()
        videoFragment = VideoFragment.newInstance()
        photoFragment = PhotoFragment.newInstance()

        // 设置ViewPager适配器
        tabPagerAdapter = TabPagerAdapter(this)
        tabPagerAdapter.addFragment(liveFragment!!, "实时")
        tabPagerAdapter.addFragment(videoFragment!!, "视频")
        tabPagerAdapter.addFragment(photoFragment!!, "图片")

        viewPager.adapter = tabPagerAdapter

        // 连接TabLayout和ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabPagerAdapter.getPageTitle(position)
        }.attach()

        // 设置页面切换监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 如果已连接设备且切换到视频或图片Tab，加载相应数据
                if (isConnected) {
                    when (position) {
                        1 -> videoFragment?.loadVideoList()
                        2 -> photoFragment?.loadPhotoList()
                    }
                }
            }
        })

        // 设置按钮点击事件
        btnConnect?.setOnClickListener {
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
                            // 连接设备
                            connectDevice()
                        }
                    }
                })
            } else {
                disconnectDevice()
                btnConnect?.text = "连接"
            }
        }
    }

    private fun initClient() {
        client = VisionClient.getInstance()
        client?.init(this, VisionCallback())
    }

    // 提供获取客户端实例的方法，供Fragment使用
    fun getClient(): VisionClient? {
        return client
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

        // 更新Fragment的连接状态
        liveFragment?.updateConnectionStatus(false)
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
        // 断开连接
        client!!.disconnect()
        isConnected = false

        // 更新Fragment的连接状态
        liveFragment?.updateConnectionStatus(false)
    }

    private fun showLoadingDialog(message: String) {
        dismissLoadingDialog() // 确保没有其他对话框在显示

        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setCancelable(false)

        // 添加一个进度条
        val progressBar = ProgressBar(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = android.view.Gravity.CENTER
        progressBar.layoutParams = params

        builder.setView(progressBar)

        loadingDialog = builder.create()
        loadingDialog!!.show()
    }

    private fun dismissLoadingDialog() {
        connectionTimeoutHandler.removeCallbacksAndMessages(null) // 移除超时回调

        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog!!.dismiss()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
    }

    private fun checkWifiConnection() {
        if (!isWifiConnected()) {
            showNoWifiDialog()
        } else {
            // 检查是否连接到设备WiFi
            DeviceConnection.isConnectedToDeviceWifiAsync(this, object : DeviceConnection.DeviceConnectionCallback {
                override fun onResult(isConnected: Boolean) {
                    if (!isConnected) {
                        showNoDeviceDialog()
                    }
                }
            })
        }
    }

    private fun showNoWifiDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("未连接WiFi")
            .setMessage("请先连接到WiFi网络")
            .setPositiveButton("去连接") { _, _ ->
                openWifiSettings()
            }
            .setNegativeButton("取消") { _, _ ->
                showToast("未连接WiFi，无法使用设备功能")
            }
            .setCancelable(false)
            .show()
    }

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
        if (hasRequiredPermissions()) {
            if (isWifiConnected()) {
                DeviceConnection.isConnectedToDeviceWifiAsync(this, object : DeviceConnection.DeviceConnectionCallback {
                    override fun onResult(isConnected: Boolean) {
                        if (isConnected && !this@MainActivity.isConnected) {
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

        // 存储权限列表（根据 Android 版本不同而不同）
        val storagePermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        // 合并权限列表
        val allPermissions = basePermissions + storagePermissions

        // 过滤出未授权的权限
        return allPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (!allGranted) {
                showToast("部分权限被拒绝，应用可能无法正常工作")
            }
        }
    }

    private inner class VisionCallback : BaseVisionCallback() {
        override fun onConnected() {
            runOnUiThread {
                dismissLoadingDialog() // 连接成功，关闭加载对话框
                isConnected = true
                btnConnect!!.text = "断开"
                showToast("设备连接成功")
                Log.d(TAG, "设备连接成功")

                // 更新Fragment的连接状态
                liveFragment?.updateConnectionStatus(true)

                // 如果当前是视频或图片Tab，加载相应数据
                when (viewPager.currentItem) {
                    1 -> videoFragment?.loadVideoList()
                    2 -> photoFragment?.loadPhotoList()
                }
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
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }
}