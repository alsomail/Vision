package com.also.vision;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.MediaPlayer.PlayM4.Player;
import org.MediaPlayer.PlayM4.PlayerCallBack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class FileVideoPlayActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "FileVideoPlay";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Player mPlayer;
    private int mPort = -1;
    private String mVideoPath;
    private boolean mIsPlaying = false;
    private boolean mIsPaused = false;

    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private ImageButton mPlayPauseButton;

    private Timer mTimer;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100: // 显示回调
                    updatePlayingStatus();
                    break;
            }
        }
    };

    // 文件播放结束回调
    private PlayerCallBack.PlayerPlayEndCB mPlayEndCB = new PlayerCallBack.PlayerPlayEndCB() {
        @Override
        public void onPlayEnd(int port) {
            Log.d(TAG, "播放结束，端口: " + port);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopPlay();
                    mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
                }
            });
        }
    };

    // 文件参考回调
    private PlayerCallBack.PlayerFileRefCB mFileRefCB = new PlayerCallBack.PlayerFileRefCB() {
        @Override
        public void onFileRefDone(int port) {
            Log.d(TAG, "文件参考完成, 端口: " + port);
        }
    };

    // 显示回调
    private PlayerCallBack.PlayerDisplayCB mDisplayCB = new PlayerCallBack.PlayerDisplayCB() {
        @Override
        public void onDisplay(int port, byte[] data, int width, int height, int stamp, int type, int reserved1, int reserved2) {
            mHandler.sendEmptyMessage(100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_video);

        // 获取传入的视频路径
        mVideoPath = getIntent().getStringExtra("video_path");
        if (mVideoPath == null || mVideoPath.isEmpty()) {
            Toast.makeText(this, "视频路径无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        initViews();

        // 初始化播放器
        initPlayer();
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.surface_view);
        mSeekBar = findViewById(R.id.seek_bar);
        mCurrentTime = findViewById(R.id.current_time);
        mTotalTime = findViewById(R.id.total_time);
        mPlayPauseButton = findViewById(R.id.btn_play_pause);

        mPlayPauseButton.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mIsPlaying) {
                    mPlayer.setPlayPos(mPort, progress / (float)seekBar.getMax());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mIsPlaying && mPlayer != null && mPort != -1) {
                    mPlayer.play(mPort, holder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
    }

    private void initPlayer() {
        mPlayer = Player.getInstance();
        mPort = mPlayer.getPort();

        if (mPort < 0 || mPort > 15) {
            Toast.makeText(this, "获取播放端口失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置文件结束回调
        boolean setPlayEndCB = mPlayer.setFileEndCB(mPort, mPlayEndCB);
        Log.d(TAG, "设置文件结束回调结果: " + setPlayEndCB);
        if (!setPlayEndCB) {
            Log.e(TAG, "设置文件结束回调失败: " + mPlayer.getLastError(mPort));
        }

        // 设置文件参考回调
        boolean setFileRefCB = mPlayer.setFileRefCB(mPort, mFileRefCB);
        Log.d(TAG, "设置文件参考回调结果: " + setFileRefCB);
        if (!setFileRefCB) {
            Log.e(TAG, "设置文件参考回调失败: " + mPlayer.getLastError(mPort));
        }

        // 打开文件
        boolean openFile = mPlayer.openFile(mPort, mVideoPath);
        Log.d(TAG, "打开文件结果: " + openFile);
        if (!openFile) {
            Log.e(TAG, "打开文件失败: " + mPlayer.getLastError(mPort));
            Toast.makeText(this, "打开文件失败", Toast.LENGTH_SHORT).show();
            releasePlayer();
            return;
        }

        // 尝试使用硬解码
        if (!mPlayer.setHardDecode(mPort, 1)) {
            Log.e(TAG, "设置硬解码失败: " + mPlayer.getLastError(mPort));
        }

        // 设置显示回调
        boolean setDisplayCB = mPlayer.setDisplayCB(mPort, mDisplayCB);
        Log.d(TAG, "设置显示回调结果: " + setDisplayCB);
        if (!setDisplayCB) {
            Log.e(TAG, "设置显示回调失败: " + mPlayer.getLastError(mPort));
        }

        // 获取文件总时长并设置到界面
        long totalTime = mPlayer.getFileTime(mPort);
        mSeekBar.setMax((int)totalTime);
        mTotalTime.setText(formatTime(totalTime));

        // 播放声音
        boolean playSound = mPlayer.playSound(mPort);
        Log.d(TAG, "播放声音结果: " + playSound);

        // 开始播放
        startPlay();
    }

    private void startPlay() {
        if (mPlayer != null && mPort != -1 && !mIsPlaying) {
            boolean play = mPlayer.play(mPort, mSurfaceHolder);
            Log.d(TAG, "开始播放结果: " + play);
            if (play) {
                mIsPlaying = true;
                mIsPaused = false;
                mPlayPauseButton.setImageResource(android.R.drawable.ic_media_pause);

                // 启动定时器更新进度
                startUpdateTimer();
            } else {
                Log.e(TAG, "开始播放失败: " + mPlayer.getLastError(mPort));
                Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pausePlay() {
        if (mPlayer != null && mPort != -1 && mIsPlaying && !mIsPaused) {
            boolean pause = mPlayer.pause(mPort, 1);
            if (pause) {
                mIsPaused = true;
                mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);

                // 停止更新进度
                stopUpdateTimer();
            }
        }
    }

    private void resumePlay() {
        if (mPlayer != null && mPort != -1 && mIsPlaying && mIsPaused) {
            boolean resume = mPlayer.pause(mPort, 0);
            if (resume) {
                mIsPaused = false;
                mPlayPauseButton.setImageResource(android.R.drawable.ic_media_pause);

                // 重新开始更新进度
                startUpdateTimer();
            }
        }
    }

    private void stopPlay() {
        stopUpdateTimer();

        if (mIsPlaying && mPlayer != null && mPort != -1) {
            mPlayer.stop(mPort);
            mIsPlaying = false;
            mIsPaused = false;
        }
    }

    private void releasePlayer() {
        stopPlay();

        if (mPlayer != null && mPort != -1) {
            mPlayer.closeFile(mPort);
            mPlayer.freePort(mPort);
            mPort = -1;
        }
    }

    private void startUpdateTimer() {
        stopUpdateTimer();

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updatePlayingStatus();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopUpdateTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void updatePlayingStatus() {
        if (mPlayer != null && mPort != -1 && mIsPlaying && !mIsPaused) {
            // 获取当前播放时间
            int playedTime = mPlayer.getPlayedTime(mPort);
            mSeekBar.setProgress(playedTime);
            mCurrentTime.setText(formatTime(playedTime));
        }
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        return format.format(date);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_play_pause) {
            if (mIsPlaying) {
                if (mIsPaused) {
                    resumePlay();
                } else {
                    pausePlay();
                }
            } else {
                startPlay();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsPlaying && !mIsPaused) {
            pausePlay();
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}