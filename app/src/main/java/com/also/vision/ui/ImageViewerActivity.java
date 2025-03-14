package com.also.vision.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.also.vision.R;
import com.also.vision.utils.FileDownloader;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ImageViewerActivity extends AppCompatActivity {
    private static final String TAG = "ImageViewerActivity";
    
    private PhotoView photoView;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private ImageButton btnBack;
    private ImageButton btnDownload;
    
    private String imageUrl;
    private String imageName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        
        // 初始化视图
        photoView = findViewById(R.id.photoView);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        btnBack = findViewById(R.id.btnBack);
        btnDownload = findViewById(R.id.btnDownload);
        
        // 获取图片URL和名称
        if (getIntent().hasExtra("imageUrl") && getIntent().hasExtra("imageName")) {
            imageUrl = getIntent().getStringExtra("imageUrl");
            imageName = getIntent().getStringExtra("imageName");
            tvTitle.setText(imageName);
        } else {
            Toast.makeText(this, "图片信息不完整", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
        
        // 设置下载按钮
        btnDownload.setOnClickListener(v -> downloadImage());
        
        // 加载图片
        loadImage();
    }
    
    private void loadImage() {
        progressBar.setVisibility(View.VISIBLE);
        
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        
        Glide.with(this)
                .load(imageUrl)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImageViewerActivity.this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(photoView);
    }
    
    private void downloadImage() {
        FileDownloader downloader = new FileDownloader(this);
        downloader.downloadFile(imageUrl, imageName, new FileDownloader.DownloadCallback() {
            @Override
            public void onDownloadStart(String fileName) {
                Toast.makeText(ImageViewerActivity.this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onDownloadProgress(int progress) {
                // 可以在这里更新进度条
            }
            
            @Override
            public void onDownloadComplete(File file) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ImageViewerActivity.this, "下载完成: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDownloadFailed(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ImageViewerActivity.this, "下载失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 