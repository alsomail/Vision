package com.hikvision.iso_demux;

/**
 * Time:2025/3/14 16:15
 * Author: Also
 * Description:
 */
public class IsoDemux {
    private static IsoDemux instance = new IsoDemux();

    static {
        System.loadLibrary("IsoDemux");
    }

    private IsoDemux() {
    }

    public static IsoDemux a() {
        return instance;
    }

    public native int getFileIsoOutputType(String str);
}
