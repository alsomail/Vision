package com.also.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * 视频流管理类
 * 负责处理RTSP视频流的连接和播放
 */
public class VideoStreamManager {
    private static final String TAG = "VideoStreamManager";
    private static VideoStreamManager instance;
    
    // RTSP地址
    private static final String RTSP_URL = "rtsp://192.168.42.1/ch1/sub/av_stream";
    
    private boolean isPlaying = false;
    private SurfaceView surfaceView;
    private VideoCallback callback;
    private Context context;
    
    // ExoPlayer相关
    private ExoPlayer player;
    private TextureView textureView;
    
    private boolean pendingPlay = false;
    
    private VideoStreamManager() {
        // 初始化视频播放相关组件
    }
    
    public static VideoStreamManager getInstance() {
        if (instance == null) {
            synchronized (VideoStreamManager.class) {
                if (instance == null) {
                    instance = new VideoStreamManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化视频管理器
     * @param callback 视频回调
     */
    public void init(VideoCallback callback) {
        this.callback = callback;
        // 初始化其他组件，但不设置SurfaceView
    }
    
    /**
     * 设置视频显示视图
     * @param surfaceView 用于显示视频的SurfaceView
     */
    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        if (isPlaying) {
            // 如果已经在播放，则重新设置解码器输出
            setupDecoderOutput();
        }
    }
    
    /**
     * 移除视频显示视图
     */
    public void removeSurfaceView() {
        if (this.surfaceView != null) {
            // 清理资源
            if (isPlaying) {
                // 如果正在播放，暂时停止渲染但保持解码
                pauseRendering();
            }
            this.surfaceView = null;
        }
    }
    
    /**
     * 初始化视频播放器
     */
    private void initPlayer() {
        try {
            // 创建ExoPlayer实例
            player = new ExoPlayer.Builder(context).build();
            
            // 创建TextureView
            textureView = new TextureView(context);
            
            // 替换SurfaceView
            ViewGroup parent = (ViewGroup) surfaceView.getParent();
            if (parent != null) {
                int index = parent.indexOfChild(surfaceView);
                int width = surfaceView.getLayoutParams().width;
                int height = surfaceView.getLayoutParams().height;
                
                parent.removeView(surfaceView);
                
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        width, height);
                textureView.setLayoutParams(params);
                
                parent.addView(textureView, index);
            }
            
            // 设置播放器的输出视图
            player.setVideoTextureView(textureView);
            
            // 设置播放器监听
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        isPlaying = true;
                        if (callback != null) {
                            callback.onPlayStarted();
                        }
                    } else if (state == Player.STATE_ENDED) {
                        isPlaying = false;
                        if (callback != null) {
                            callback.onPlayStopped();
                        }
                    }
                }
                
                @Override
                public void onPlayerError(PlaybackException error) {
                    isPlaying = false;
                    Log.e(TAG, "播放错误: " + error.getMessage());
                    
                    if (callback != null) {
                        callback.onPlayError("播放错误: " + error.getMessage());
                    }
                }
            });
            
            Log.d(TAG, "播放器初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "播放器初始化失败: " + e.getMessage());
            e.printStackTrace();
            
            if (callback != null) {
                callback.onPlayError("播放器初始化失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 开始播放视频
     */
    public void startPlay() {
        if (surfaceView == null) {
            // 如果没有设置SurfaceView，记录状态但不实际开始播放
            Log.w(TAG, "尝试开始播放但SurfaceView未设置");
            pendingPlay = true;
            return;
        }
        
        // 开始播放逻辑
        // ...
        
        isPlaying = true;
        pendingPlay = false;
        
        if (callback != null) {
            callback.onPlayStarted();
        }
    }
    
    /**
     * 停止播放视频流
     */
    public void stopPlay() {
        if (!isPlaying || player == null) {
            return;
        }
        
        try {
            player.stop();
            isPlaying = false;
            Log.d(TAG, "停止播放视频流");
            
            if (callback != null) {
                callback.onPlayStopped();
            }
        } catch (Exception e) {
            Log.e(TAG, "停止播放失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 截图
     */
    public void takeSnapshot() {
        if (!isPlaying || player == null) {
            Log.e(TAG, "截图失败: 视频未播放");
            return;
        }
        
        try {
            // 创建截图保存目录
            File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File cameraDir = new File(dcimDir, "Camera");
            if (!cameraDir.exists()) {
                cameraDir.mkdirs();
            }
            
            // 生成文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "IMG_" + sdf.format(new Date()) + ".jpg";
            File imageFile = new File(cameraDir, fileName);
            String path = imageFile.getAbsolutePath();
            
            // 从TextureView获取Bitmap
            Bitmap bitmap = textureView.getBitmap();
            
            // 保存Bitmap到文件
            if (bitmap != null) {
                FileOutputStream fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                
                Log.d(TAG, "截图成功: " + path);
                
                if (callback != null) {
                    callback.onSnapshotTaken(path);
                }
            } else {
                Log.e(TAG, "截图失败: 无法获取视频帧");
            }
        } catch (Exception e) {
            Log.e(TAG, "截图失败: " + e.getMessage());
            e.printStackTrace();
            
            if (callback != null) {
                callback.onPlayError("截图失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理视频数据
     * 实际应用中需要实现此方法来处理视频数据
     */
    public void handleVideoData(int dataType, byte[] data, int dataLen) {
        // 使用ExoPlayer时，不需要手动处理视频数据
        Log.d(TAG, "收到视频数据，类型: " + dataType + "，长度: " + dataLen);
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        isPlaying = false;
    }
    
    /**
     * 视频回调接口
     */
    public interface VideoCallback {
        void onPlayStarted();
        void onPlayStopped();
        void onPlayError(String error);
        void onSnapshotTaken(String path);
    }
}