package com.also.vision;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.MediaPlayer.PlayM4.Player;
import org.MediaPlayer.PlayM4.PlayerCallBack;
import org.hik.np.NPClient;
import org.hik.np.NPClientCB;

public class RealtimeVideoPlayActivity extends AppCompatActivity implements NPClientCB.NPCDataCB, NPClientCB.NPCMsgCB {
    private static final String TAG = "RealtimeVideoPlay";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Player mPlayer;
    private int mPort = -1;
    private NPClient mNPClient;
    private int mClientID = -1;
    private boolean mIsPlaying = false;
    private String mStreamUrl;
    private Handler mHandler = new Handler();
    private NPClient.NPCSignalProtocol mProtocol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_video);

        // 获取传入的流地址
        mStreamUrl = getIntent().getStringExtra("stream_url");
        if (mStreamUrl == null || mStreamUrl.isEmpty()) {
            mStreamUrl = "rtsp://192.168.42.1/ch1/sub/av_stream"; // 默认值
        }

        // 初始化视图
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startPlay();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stopPlay();
            }
        });
    }

    private void startPlay() {
        // 初始化播放器
        if (mPlayer == null) {
            mPlayer = Player.getInstance();
        }

        if (mPort == -1) {
            mPort = mPlayer.getPort();
            if (mPort == -1) {
                Log.e(TAG, "获取播放端口失败");
                return;
            }
        }

        // 初始化NPClient
        if (mNPClient == null) {
            mNPClient = NPClient.getInstance();
        }

        if (mProtocol == null) {
            mProtocol = new NPClient.NPCSignalProtocol();
        }

        // 创建客户端连接
        mClientID = mNPClient.npcCreate(mStreamUrl, NPClient.NPCSignalProtocol.NPC_PRO_AUTO);
        if (mClientID < 0) {
            Log.e(TAG, "创建NPC客户端失败: " + mClientID);
            return;
        }

        // 设置消息回调
        int setMsgResult = mNPClient.npcSetMsgCallBack(mClientID, this, null);
        if (setMsgResult != 0) {
            Log.e(TAG, "设置消息回调失败: " + setMsgResult);
            return;
        }

        // 设置流模式
        if (!mPlayer.setStreamOpenMode(mPort, 0)) {
            Log.e(TAG, "设置流模式失败: " + mPlayer.getLastError(mPort));
            releaseResources();
            return;
        }

        // 打开流
        if (!mPlayer.openStream(mPort, null, 40, 20971520)) {
            Log.e(TAG, "打开流失败: " + mPlayer.getLastError(mPort));
            releaseResources();
            return;
        }

        // 尝试设置硬解码
        mPlayer.setHardDecode(mPort, 1);

        // 设置显示回调
        mPlayer.setDisplayCB(mPort, new PlayerCallBack.PlayerDisplayCB() {
            @Override
            public void onDisplay(int port, byte[] data, int dataLen, int width, int height, int frameType, int timestamp, int useId) {
                // 可以在这里处理每一帧显示
            }
        });

        // 开始播放
        if (!mPlayer.play(mPort, mSurfaceHolder)) {
            Log.e(TAG, "播放失败: " + mPlayer.getLastError(mPort));
            releaseResources();
            return;
        }

        // 打开网络连接
        int openResult = mNPClient.npcOpen(mClientID, this, null);
        if (openResult != 0) {
            Log.e(TAG, "打开NPC连接失败: " + openResult);
            releaseResources();
            return;
        }

        mIsPlaying = true;
    }

    private void stopPlay() {
        if (mNPClient != null && mClientID != -1) {
            mNPClient.npcClose(mClientID);
            mNPClient.npcDestroy(mClientID);
            mClientID = -1;
        }

        if (mPlayer != null && mPort != -1) {
            mPlayer.stop(mPort);
            mPlayer.closeStream(mPort);
            mPlayer.freePort(mPort);
            mPort = -1;
        }

        mIsPlaying = false;
    }

    private void releaseResources() {
        if (mNPClient != null && mClientID != -1) {
            mNPClient.npcDestroy(mClientID);
            mClientID = -1;
        }

        if (mPlayer != null && mPort != -1) {
            mPlayer.freePort(mPort);
            mPort = -1;
        }
    }

    @Override
    public void onNPCData(int port, int dataType, byte[] data, int dataLen, byte[] info) {
        if (mPlayer == null || mPort == -1) {
            return;
        }

        switch (dataType) {
            case 0: // SDP数据
                Log.d(TAG, "接收到SDP数据");
                if (!mPlayer.setStreamOpenMode(mPort, 0)) {
                    Log.e(TAG, "设置流模式失败");
                    return;
                }

                Player.SESSION_INFO sessionInfo = new Player.SESSION_INFO();
                sessionInfo.nInfoLen = dataLen;
                sessionInfo.nInfoType = 1;

                if (!mPlayer.openStreamAdvanced(mPort, 1, sessionInfo, data, 1048576)) {
                    Log.e(TAG, "打开高级流失败: " + mPlayer.getLastError(mPort));
                    return;
                }

                if (!mPlayer.setHardDecode(mPort, 1)) {
                    Log.e(TAG, "设置硬解码失败: " + mPlayer.getLastError(mPort));
                }

                if (!mPlayer.play(mPort, mSurfaceHolder)) {
                    Log.e(TAG, "播放失败");
                }
                break;

            case 1: // 视频数据
            case 3: // 复合数据
                if (!mPlayer.inputData(mPort, data, dataLen)) {
                    Log.d(TAG, "输入数据失败: " + dataLen);
                }
                break;

            default:
                Log.d(TAG, "接收到其他类型数据: " + dataType);
                break;
        }
    }

    @Override
    public void onNPCMsg(int port, int msgType, byte[] data, int dataLen, byte[] info) {
        if (msgType == 1) { // 流关闭消息
            Log.d(TAG, "流关闭");
            if (mPlayer != null && mPort != -1) {
                mPlayer.inputData(mPort, null, -1);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsPlaying) {
            mPlayer.pause(mPort, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsPlaying && mPlayer != null && mPort != -1) {
            mPlayer.pause(mPort, 0);
        }
    }

    @Override
    protected void onDestroy() {
        stopPlay();
        super.onDestroy();
    }
}