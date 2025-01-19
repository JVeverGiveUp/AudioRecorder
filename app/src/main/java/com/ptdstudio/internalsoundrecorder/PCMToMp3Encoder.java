package com.ptdstudio.internalsoundrecorder;


public class PCMToMp3Encoder {
    static {
        System.loadLibrary("Mp3Codec");
    }

    public static native int init(String pcmFilePath, int audioChannels, int bitRate,
                                  int sampleRate, String mp3FilePath);

    public static native void encode();

    public static native void destroy();
}
