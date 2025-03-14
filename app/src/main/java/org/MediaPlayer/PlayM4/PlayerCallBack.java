package org.MediaPlayer.PlayM4;

/* loaded from: classes.dex */
public class PlayerCallBack {

    /* loaded from: classes.dex */
    public interface PlayerAdditionalCB {
        void onAdditional(int i, int i2, int i3, int i4, int i5, byte[] bArr);
    }

    /* loaded from: classes.dex */
    public interface PlayerDecodeCB {
        void onDecode(int i, byte[] bArr, int i2, int i3, int i4, int i5, int i6, int i7);
    }

    /* loaded from: classes.dex */
    public interface PlayerDisplayCB {
        void onDisplay(int i, byte[] bArr, int i2, int i3, int i4, int i5, int i6, int i7);
    }

    /* loaded from: classes.dex */
    public interface PlayerEncTypeChgCB {
        void onEncTypeChg(int i);
    }

    /* loaded from: classes.dex */
    public interface PlayerFileRefCB {
        void onFileRefDone(int i);
    }

    /* loaded from: classes.dex */
    public interface PlayerHSDetectCB {
        void onHSDetect(int i, int i2);
    }

    /* loaded from: classes.dex */
    public interface PlayerPlayEndCB {
        void onPlayEnd(int i);
    }

    /* loaded from: classes.dex */
    public interface PlayerPreRecordCB {
        void onPreRecord(int i, byte[] bArr, int i2);
    }
}
