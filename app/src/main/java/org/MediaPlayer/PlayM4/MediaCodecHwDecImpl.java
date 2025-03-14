package org.MediaPlayer.PlayM4;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class MediaCodecHwDecImpl {
    private String TAG = "MediaCodecHwDecImpl";
    private boolean bInit;
    private boolean bStart;
    private MediaCodec.BufferInfo bufferInfo;
    private int inputBufferIndex;
    private ByteBuffer[] inputBuffers;
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private int outputBufferIndex;
    private ByteBuffer[] outputBuffers;

    public MediaCodecHwDecImpl() {
        this.mediaCodec = null;
        this.mediaFormat = null;
        this.inputBuffers = null;
        this.outputBuffers = null;
        this.bufferInfo = null;
        this.outputBufferIndex = -1;
        this.inputBufferIndex = -1;
        this.bInit = false;
        this.bStart = false;
        this.mediaCodec = null;
        this.mediaFormat = null;
        this.inputBuffers = null;
        this.outputBuffers = null;
        this.bufferInfo = null;
        this.outputBufferIndex = -1;
        this.inputBufferIndex = -1;
        this.bInit = false;
        this.bStart = false;
    }

    public int Init(int i, int i2, int i3) {
        if (i < 0 || i2 <= 0 || i3 <= 0) {
            return Constants.MEDIACODEC_PARAM_INVALID;
        }
        String str = BuildConfig.FLAVOR;
        switch (i) {
            case 0:
                str = "video/avc";
                Log.i(this.TAG, "Create H264 MediaCodec and MediaFormat");
                break;
            case 1:
                str = "video/hevc";
                Log.i(this.TAG, "Create H265 MediaCodec and MediaFormat");
                break;
            case 2:
                str = "video/mp4v-es";
                Log.i(this.TAG, "Create MPEG4 MediaCodec and MediaFormat");
                break;
            default:
                Log.i(this.TAG, "other type is not support set mime null");
                break;
        }
        if (str.isEmpty()) {
            return Constants.MEDIACODEC_MIME_NOT_SUPPORT;
        }
        if (this.bInit) {
            return Constants.MEDIACODEC_WRONG_ORDER;
        }
        try {
            this.mediaFormat = MediaFormat.createVideoFormat(str, i2, i3);
            if (this.mediaFormat == null) {
                return Constants.MEDIACODEC_CREATE_MEDIAFORMAT_FAIL;
            }
            try {
                this.mediaCodec = MediaCodec.createDecoderByType(str);
                if (this.mediaCodec == null) {
                    return Constants.MEDIACODEC_CREATE_MEDIACODEC_FAIL;
                }
                this.bInit = true;
                return 0;
            } catch (Exception e) {
                return Constants.MEDIACODEC_TRY_CATCH_ERR;
            }
        } catch (Exception e2) {
            return Constants.MEDIACODEC_TRY_CATCH_ERR;
        }
    }

    public int OutputDataFromCodec(TimeStruct timeStruct) {
        if (!this.bStart) {
            return Constants.MEDIACODEC_WRONG_ORDER;
        }
        this.bufferInfo = new MediaCodec.BufferInfo();
        if (this.bufferInfo == null) {
            return Constants.MEDIACODEC_OBJ_NULL;
        }
        try {
            this.outputBufferIndex = this.mediaCodec.dequeueOutputBuffer(this.bufferInfo, 0L);
            if (this.outputBufferIndex >= 0) {
                timeStruct.value = this.bufferInfo.presentationTimeUs;
                return 0;
            } else if (-3 == this.outputBufferIndex) {
                this.outputBuffers = this.mediaCodec.getOutputBuffers();
                return Constants.MEDIACODEC_OUTPUT_BUFFERS_CHANGED;
            } else if (-2 != this.outputBufferIndex) {
                return -1 == this.outputBufferIndex ? Constants.MEDIACODEC_TRY_AGAIN_LATER : Constants.MEDIACODEC_OUTPUTBUFFER_INDEX_INVALID;
            } else {
                Log.e(this.TAG, "Output format changed: " + this.mediaCodec.getOutputFormat());
                return Constants.MEDIACODEC_OUTPUT_FORMAT_CHANGED;
            }
        } catch (Exception e) {
            return Constants.MEDIACODEC_TRY_CATCH_ERR;
        }
    }

    public int OutputDataRender() {
        if (!this.bStart) {
            return Constants.MEDIACODEC_WRONG_ORDER;
        }
        if (this.outputBufferIndex < 0) {
            return Constants.MEDIACODEC_OUTPUTBUFFER_INDEX_INVALID;
        }
        try {
            this.mediaCodec.releaseOutputBuffer(this.outputBufferIndex, true);
            return 0;
        } catch (Exception e) {
            return Constants.MEDIACODEC_TRY_CATCH_ERR;
        }
    }

    public int PushDataIntoCodec(byte[] bArr, int i, int i2, int i3) {
        if (!this.bStart) {
            return Constants.MEDIACODEC_WRONG_ORDER;
        }
        if (bArr == null || i == 0) {
            return Constants.MEDIACODEC_PARAM_INVALID;
        }
        try {
            this.inputBufferIndex = this.mediaCodec.dequeueInputBuffer(i3);
            if (this.inputBufferIndex < 0) {
                return Constants.MEDIACODEC_INPUTBUFFER_INDEX_INVALID;
            }
            try {
                ByteBuffer byteBuffer = this.inputBuffers[this.inputBufferIndex];
                byteBuffer.clear();
                byteBuffer.put(bArr, 0, i);
                this.mediaCodec.queueInputBuffer(this.inputBufferIndex, 0, i, i2, 0);
                return 0;
            } catch (Exception e) {
                return Constants.MEDIACODEC_TRY_CATCH_ERR;
            }
        } catch (Exception e2) {
            return Constants.MEDIACODEC_TRY_CATCH_ERR;
        }
    }

    public int Start(Surface surface) {
        if (this.bInit && !this.bStart) {
            if (surface == null || this.mediaFormat == null || this.mediaCodec == null) {
                return Constants.MEDIACODEC_OBJ_NULL;
            }
            try {
                this.mediaCodec.configure(this.mediaFormat, surface, (MediaCrypto) null, 0);
                this.mediaCodec.start();
                this.inputBuffers = this.mediaCodec.getInputBuffers();
                this.outputBuffers = this.mediaCodec.getOutputBuffers();
                if (this.inputBuffers == null || this.outputBuffers == null) {
                    return Constants.MEDIACODEC_BYTEBUFFER_NULL;
                }
                this.bStart = true;
                return 0;
            } catch (Exception e) {
                return Constants.MEDIACODEC_TRY_CATCH_ERR;
            }
        }
        return Constants.MEDIACODEC_WRONG_ORDER;
    }

    public int Stop() {
        if (!this.bStart) {
            return Constants.MEDIACODEC_WRONG_ORDER;
        }
        try {
            this.mediaCodec.stop();
            this.mediaCodec.release();
            this.mediaCodec = null;
            return 0;
        } catch (Exception e) {
            return Constants.MEDIACODEC_TRY_CATCH_ERR;
        }
    }
}
