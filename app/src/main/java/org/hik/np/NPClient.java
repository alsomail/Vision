package org.hik.np;

import android.util.Log;
import org.hik.np.NPClientCB;

/* loaded from: classes.dex */
public class NPClient {
    public int nProtocol;
    private static String TAG = "NPClient";
    private static NPClient mNPClient = null;
    private static int NPCLIENT_NO_ERR = 0;

    /* loaded from: classes.dex */
    public class NPCHttpInfo {
        public int method;
        public int nContentLen;

        public NPCHttpInfo() {
        }
    }

    /* loaded from: classes.dex */
    public static class NPCHttpMethod {
        public static int NPC_HTTP_UNKNOWN = 0;
        public static int NPC_HTTP_GET = 1;
        public static int NPC_HTTP_POST = 2;
        public static int NPC_HTTP_HEAD = 3;
        public static int NPC_HTTP_OPTIONS = 4;
        public static int NPC_HTTP_PUT = 5;
        public static int NPC_HTTP_DELETE = 6;
        public static int NPC_HTTP_TRACE = 7;
        public static int NPC_HTTP_CONNECT = 8;
    }

    /* loaded from: classes.dex */
    public class NPCOnvifInfo {
        public int nInputNo;
        public int nStreamNo;

        public NPCOnvifInfo() {
        }
    }

    /* loaded from: classes.dex */
    public class NPCRtspInfo {
        public double nNptEnd;
        public double nNptStart;
        public float nScale;

        public NPCRtspInfo() {
        }
    }

    /* loaded from: classes.dex */
    public static class NPCSignalProtocol {
        public static int NPC_PRO_AUTO = 0;
        public static int NPC_PRO_RTSP = 1;
        public static int NPC_PRO_RTMP = 2;
        public static int NPC_PRO_HLS = 3;
        public static int NPC_PRO_HTTP = 4;
        public static int NPC_PRO_ONVIF = 5;
    }

    /* loaded from: classes.dex */
    public static class NPCTransmit {
        public static int NPC_TRANSMIT_TCP = 0;
        public static int NPC_TRANSMIT_UDP = 1;
        public static int NPC_TRANSMIT_HTTP = 2;
    }

    static {
        System.loadLibrary("NPClient");
        System.loadLibrary("HIK_NPCClient");
    }

    private NPClient() {
    }

    private native int NPCChangeScale(int i, float f);

    private native int NPCClose(int i);

    private native int NPCCreate(byte[] bArr, int i);

    private native int NPCDestroy(int i);

    private native int NPCGetInfo(int i, NPCRtspInfo nPCRtspInfo);

    private native int NPCOpen(int i, NPClientCB.NPCDataCB nPCDataCB, byte[] bArr);

    private native int NPCOpenEx(int i, NPClientCB.NPCDataCB nPCDataCB, byte[] bArr, int i2);

    private native int NPCSetInfo(int i, NPCRtspInfo nPCRtspInfo);

    private native int NPCSetMsgCallBack(int i, NPClientCB.NPCMsgCB nPCMsgCB, byte[] bArr);

    private native int NPCSetTimeout(int i, int i2);

    private native int NPCSetTransmitMode(int i, int i2, int i3);

    private native int NPCSetUserAgent(int i, byte[] bArr);

    public static NPClient getInstance() {
        if (mNPClient == null) {
            try {
                mNPClient = new NPClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mNPClient;
    }

    public int npcChangeScale(int i, float f) {
        return NPCChangeScale(i, f);
    }

    public int npcClose(int i) {
        return NPCClose(i);
    }

    public int npcCreate(String str, int i) {
        byte[] bArr = null;
        if (str != null) {
            byte[] bytes = str.getBytes();
            if (bytes == null) {
                return -1;
            }
            try {
                bArr = new byte[bytes.length + 1];
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i2 = 0; i2 < bytes.length; i2++) {
                bArr[i2] = bytes[i2];
            }
            bArr[bArr.length - 1] = 0;
        }
        Log.i(TAG, "NPCCreate szUrl=" + bArr + ",rtspAddr=" + str);
        return NPCCreate(bArr, i);
    }

    public int npcDestroy(int i) {
        return NPCDestroy(i);
    }

    public int npcGetInfo(int i, NPCRtspInfo nPCRtspInfo) {
        return NPCGetInfo(i, nPCRtspInfo);
    }

    public int npcOpen(int i, NPClientCB.NPCDataCB nPCDataCB, byte[] bArr) {
        return NPCOpen(i, nPCDataCB, bArr);
    }

    public int npcOpenEx(int i, NPClientCB.NPCDataCB nPCDataCB, byte[] bArr, int i2) {
        return NPCOpenEx(i, nPCDataCB, bArr, i2);
    }

    public int npcSetInfo(int i, NPCRtspInfo nPCRtspInfo) {
        return NPCSetInfo(i, nPCRtspInfo);
    }

    public int npcSetMsgCallBack(int i, NPClientCB.NPCMsgCB nPCMsgCB, byte[] bArr) {
        return NPCSetMsgCallBack(i, nPCMsgCB, bArr);
    }

    public int npcSetTimeout(int i, int i2) {
        return NPCSetTimeout(i, i2);
    }

    public int npcSetTransmitMode(int i, int i2, int i3) {
        return NPCSetTransmitMode(i, i2, i3);
    }

    public int npcSetUserAgent(int i, String str) {
        byte[] bArr = null;
        if (str != null) {
            byte[] bytes = str.getBytes();
            try {
                bArr = new byte[bytes.length + 1];
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i2 = 0; i2 < bytes.length; i2++) {
                bArr[i2] = bytes[i2];
            }
            bArr[bArr.length - 1] = 0;
        }
        return NPCSetUserAgent(i, bArr);
    }
}
