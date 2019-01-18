package com.missile.codec.demo.record;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class MutexBean {

    private ByteBuffer byteBuffer;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isVideo;


    public MutexBean(byte[] bytes, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        this.byteBuffer = ByteBuffer.wrap(bytes);
        this.bufferInfo = bufferInfo;
        this.isVideo = isVideo;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }

    public boolean isVideo() {
        return isVideo;
    }
}
