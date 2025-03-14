package com.also.vision.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.also.vision.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private ImageButton btnBack;
    
    private String videoUrl;
    private String videoName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        // 初始化视图
        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        btnBack = findViewById(R.id.btnBack);
        
        // 获取视频URL和名称
        if (getIntent().hasExtra("videoUrl") && getIntent().hasExtra("videoName")) {
            videoUrl = getIntent().getStringExtra("videoUrl");
            videoName = getIntent().getStringExtra("videoName");
            tvTitle.setText(videoName);
        } else {
            Toast.makeText(this, "视频信息不完整", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
        
        // 初始化播放器
        initializePlayer();
    }
    
    private void initializePlayer() {
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        
        // 创建媒体源
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "VisionClient"));
        
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoUrl));
        
        // 准备播放器
        player.prepare(mediaSource);
        
        // 设置播放器监听器
        player.addListener(new Player.Listener(){
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(VideoPlayerActivity.this, "播放错误: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 自动播放
        player.setPlayWhenReady(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
    
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
} 