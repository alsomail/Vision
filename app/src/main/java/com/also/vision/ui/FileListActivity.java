package com.also.vision.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.also.vision.R;
import com.also.vision.VisionClient;
import com.also.vision.adapter.FileListAdapter;
import com.also.vision.model.DeviceFile;
import com.also.vision.model.DeviceInfo;
import com.also.vision.model.SDCardInfo;
import com.also.vision.utils.FileDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity {
    private static final String TAG = "FileListActivity";
    
    private ListView listView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FileListAdapter adapter;
    private List<DeviceFile> fileList = new ArrayList<>();
    
    private VisionClient client;
    private String fileType = "all"; // 默认获取所有文件
    private int currentOffset = 0;
    private int pageSize = 20;
    private int totalFiles = 0;
    private boolean isLoading = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        
        // 初始化视图
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        // 获取文件类型参数
        if (getIntent().hasExtra("fileType")) {
            fileType = getIntent().getStringExtra("fileType");
        }
        
        // 设置标题
        if ("video".equals(fileType)) {
            setTitle("视频列表");
        } else if ("photo".equals(fileType)) {
            setTitle("图片列表");
        } else {
            setTitle("文件列表");
        }
        
        // 初始化适配器
        adapter = new FileListAdapter(this, fileList);
        listView.setAdapter(adapter);
        
        // 设置点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceFile file = fileList.get(position);
                if (file.getFileType() == DeviceFile.TYPE_VIDEO) {
                    // 打开视频播放界面
                    Intent intent = new Intent(FileListActivity.this, VideoPlayerActivity.class);
                    intent.putExtra("videoUrl", file.getFileUrl());
                    intent.putExtra("videoName", file.getFileName());
                    startActivity(intent);
                } else {
                    // 打开图片查看界面
                    Intent intent = new Intent(FileListActivity.this, ImageViewerActivity.class);
                    intent.putExtra("imageUrl", file.getFileUrl());
                    intent.putExtra("imageName", file.getFileName());
                    startActivity(intent);
                }
            }
        });
        
        // 设置长按事件（删除文件）
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceFile file = fileList.get(position);
                deleteFile(file);
                return true;
            }
        });
        
        // 初始化客户端
        client = VisionClient.getInstance();
        client.init(this, new FileListCallback());
        
        // 加载文件列表
        loadFileList();
    }
    
    /**
     * 加载文件列表
     */
    private void loadFileList() {
        if (isLoading) {
            return;
        }
        
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        
        // 获取文件列表
        client.getFileList(fileType, currentOffset, pageSize);
    }
    
    /**
     * 删除文件
     * @param file 要删除的文件
     */
    private void deleteFile(DeviceFile file) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("删除文件")
            .setMessage("确定要删除文件 " + file.getFileName() + " 吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                client.deleteFile(file.getFileName());
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 下载文件
     * @param file 要下载的文件
     */
    private void downloadFile(DeviceFile file) {
        FileDownloader downloader = new FileDownloader(this);
        downloader.downloadFile(file.getFileUrl(), file.getFileName(), new FileDownloader.DownloadCallback() {
            @Override
            public void onDownloadStart(String fileName) {
                Toast.makeText(FileListActivity.this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDownloadProgress(int progress) {
                // 可以在这里更新进度条
            }
            
            @Override
            public void onDownloadComplete(File file) {
                Toast.makeText(FileListActivity.this, "下载完成: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDownloadFailed(String error) {
                Toast.makeText(FileListActivity.this, "下载失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 文件列表回调
     */
    private class FileListCallback implements VisionClient.VisionCallback {
        @Override
        public void onConnected() {
            // 已连接
        }
        
        @Override
        public void onConnectionFailed(String reason) {
            Toast.makeText(FileListActivity.this, "连接失败: " + reason, Toast.LENGTH_SHORT).show();
            finish();
        }
        
        @Override
        public void onSessionStarted() {
            // 会话已开始
        }
        
        @Override
        public void onSessionFailed(String reason) {
            Toast.makeText(FileListActivity.this, "会话失败: " + reason, Toast.LENGTH_SHORT).show();
            finish();
        }
        
        @Override
        public void onFileListReceived(List<DeviceFile> files, int total) {
            isLoading = false;
            progressBar.setVisibility(View.GONE);
            
            totalFiles = total;
            currentOffset += files.size();
            
            if (currentOffset == 0 && files.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("没有找到文件");
            } else {
                tvEmpty.setVisibility(View.GONE);
                fileList.addAll(files);
                adapter.notifyDataSetChanged();
            }
        }
        
        @Override
        public void onFileListFailed(String reason) {
            isLoading = false;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(FileListActivity.this, "获取文件列表失败: " + reason, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onFileDeleted() {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(FileListActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
            
            // 重新加载文件列表
            fileList.clear();
            currentOffset = 0;
            loadFileList();
        }
        
        @Override
        public void onFileDeleteFailed(String reason) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(FileListActivity.this, "删除文件失败: " + reason, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onFileDownloadUrl(String url) {
            // 不需要处理，因为我们已经有了文件URL
        }
        
        @Override
        public void onFileDownloadFailed(String reason) {
            Toast.makeText(FileListActivity.this, "获取下载链接失败: " + reason, Toast.LENGTH_SHORT).show();
        }
        
        // 其他必要的回调方法实现
        @Override public void onDeviceInfoReceived(DeviceInfo deviceInfo) {}
        @Override public void onSDCardInfoReceived(SDCardInfo sdInfo) {}
        @Override public void onVideoPlayStarted() {}
        @Override public void onVideoPlayStopped() {}
        @Override public void onVideoPlayError(String error) {}
        @Override public void onPhotoTaken() {}
        @Override public void onPhotoFailed(String reason) {}
        @Override public void onSnapshotTaken(String path) {}
        @Override public void onEventRecorded() {}
        @Override public void onEventRecordFailed(String reason) {}
        @Override public void onSDCardFormatted() {}
        @Override public void onSDCardFormatFailed(String reason) {}
        @Override public void onMessageReceived(int msgId, int result, String content) {}
    }
} 