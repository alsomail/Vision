package com.also.vision.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.also.vision.MainActivity
import com.also.vision.R
import com.also.vision.VisionClient
import com.also.vision.model.DeviceInfo
import com.also.vision.model.SDCardInfo

class LiveFragment : Fragment() {
    private var client: VisionClient? = null
    private var surfaceView: SurfaceView? = null
    private var btnPlay: Button? = null
    private var btnSnapshot: Button? = null
    private var btnPhoto: Button? = null

    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_live, container, false)

        // 初始化视图
        initViews(view)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 获取MainActivity中的客户端实例
        client = (activity as MainActivity).getClient()
    }

    private fun initViews(view: View) {
        surfaceView = view.findViewById(R.id.surfaceView)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnSnapshot = view.findViewById(R.id.btnSnapshot)
        btnPhoto = view.findViewById(R.id.btnPhoto)

        // 设置按钮点击事件
        btnPlay?.setOnClickListener {
            if (!isPlaying) {
                client?.startVideoPlay()
                btnPlay?.text = "停止"
                isPlaying = true
            } else {
                client?.stopVideoPlay()
                btnPlay?.text = "播放"
                isPlaying = false
            }
        }

        btnSnapshot?.setOnClickListener {
            if (isPlaying) {
                client?.takeSnapshot()
            } else {
                showToast("请先开始播放")
            }
        }

        btnPhoto?.setOnClickListener {
            client?.takePhoto()
        }

        // 设置SurfaceView
        client?.initVideoPlayer(surfaceView)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // 更新连接状态
    fun updateConnectionStatus(isConnected: Boolean) {
        if (!isConnected) {
            isPlaying = false
            btnPlay?.text = "播放"
        }

        btnPlay?.isEnabled = isConnected
        btnSnapshot?.isEnabled = isConnected
        btnPhoto?.isEnabled = isConnected
    }

    companion object {
        fun newInstance(): LiveFragment {
            return LiveFragment()
        }
    }
} 