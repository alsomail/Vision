package com.also.vision.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.also.vision.MainActivity
import com.also.vision.R
import com.also.vision.VisionClient
import com.also.vision.adapter.FileListAdapter
import com.also.vision.callback.BaseVisionCallback
import com.also.vision.model.DeviceFile
import com.also.vision.ui.VideoPlayerActivity

class VideoFragment : Fragment() {
    private var client: VisionClient? = null
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var surfaceView: View

    private var fileList = ArrayList<DeviceFile>()
    private var adapter: FileListAdapter? = null
    private var isLoading = false
    private var currentOffset = 0
    private var totalFiles = 0
    private val pageSize = 20

    private val callback = object : BaseVisionCallback() {
        override fun onFileListReceived(files: List<DeviceFile>, total: Int) {
            isLoading = false
            progressBar.visibility = View.GONE

            totalFiles = total
            currentOffset += files.size

            if (currentOffset == 0 && files.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "没有找到视频文件"
            } else {
                tvEmpty.visibility = View.GONE
                fileList.addAll(files.filter { it.fileType == DeviceFile.TYPE_VIDEO })
                adapter?.notifyDataSetChanged()
            }
        }

        override fun onFileListFailed(reason: String) {
            isLoading = false
            progressBar.visibility = View.GONE
            Toast.makeText(context, "获取视频列表失败: $reason", Toast.LENGTH_SHORT).show()
        }

        override fun onFileDeleted() {
            progressBar.visibility = View.GONE
            Toast.makeText(context, "视频已删除", Toast.LENGTH_SHORT).show()

            // 重新加载文件列表
            loadVideoList()
        }

        override fun onFileDeleteFailed(reason: String) {
            progressBar.visibility = View.GONE
            Toast.makeText(context, "删除视频失败: $reason", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)

        // 获取SurfaceView
        surfaceView = view.findViewById(R.id.video_surface)

        // 设置视频显示视图
        VisionClient.getInstance().setVideoSurface(surfaceView)

        // 初始化视图
        initViews(view)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 获取MainActivity中的客户端实例
        client = (activity as MainActivity).getClient()
        client?.addCallback(callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        client?.removeCallback(callback)

        // 移除视频显示视图
        VisionClient.getInstance().removeVideoSurface()
    }

    private fun initViews(view: View) {
        listView = view.findViewById(R.id.listView)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        // 初始化适配器
        adapter = FileListAdapter(requireContext(), fileList)
        listView.adapter = adapter

        // 设置点击事件
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val file = fileList[position]
            // 打开视频播放界面
            val intent = Intent(activity, VideoPlayerActivity::class.java)
            intent.putExtra("videoUrl", file.fileUrl)
            intent.putExtra("videoName", file.fileName)
            startActivity(intent)
        }

        // 设置长按事件（删除文件）
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            val file = fileList[position]
            deleteFile(file)
            true
        }
    }

    // 加载视频列表
    fun loadVideoList() {
        if (isLoading) {
            return
        }

        isLoading = true
        progressBar.visibility = View.VISIBLE

        // 清空现有数据
        fileList.clear()
        adapter?.notifyDataSetChanged()
        currentOffset = 0

        // 获取视频列表
        client?.getFileList(DeviceFile.TYPE_VIDEO, currentOffset, pageSize)
    }

    // 删除文件
    private fun deleteFile(file: DeviceFile) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("删除文件")
            .setMessage("确定要删除视频 ${file.fileName} 吗？")
            .setPositiveButton("删除") { _, _ ->
                progressBar.visibility = View.VISIBLE
                client?.deleteFile(file.fileName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    companion object {
        fun newInstance(): VideoFragment {
            return VideoFragment()
        }
    }
} 