package org.MediaPlayer.PlayM4;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/* loaded from: classes.dex */
public class SurfaceCallBack implements SurfaceHolder.Callback {
    private static final String TAG = "SurfaceCallBack";
    private int mPort;

    private SurfaceCallBack(int i) {
        this.mPort = -1;
        this.mPort = i;
    }

    private native void SurfaceChanged(int i, int i2, int i3);

    private native void SurfaceCreated(int i, Surface surface);

    private native void SurfaceDestroyed(int i);

    public static SurfaceCallBack getCallBack(int i) {
        if (i < 0 || i > 15) {
            return null;
        }
        return new SurfaceCallBack(i);
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.i(TAG, "surfaceChanged " + this.mPort);
        SurfaceChanged(this.mPort, i2, i3);
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i(TAG, "surfaceCreated " + this.mPort);
        SurfaceCreated(this.mPort, surfaceHolder.getSurface());
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.i(TAG, "surfaceDestroyed " + this.mPort);
        SurfaceDestroyed(this.mPort);
    }
}
