package com.missile.codec.demo.record;

import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MediaMuxerThread extends Thread {

    private Queue<MutexBean> mMutexBeanQueue;
    private boolean isRecording;
    private VideoRecordThread mVideoRecordThread;
    private AudioRecordThread mAudioRecordThread;
    private int mVideoTrack;
    private int mAudioTrack;
    private String path;
    private MediaMuxer mMediaMuxer;
    private MediaMuxerCallback mMediaMuxerCallback;
    private boolean isMediaMuxerStart;

    public MediaMuxerThread(String path) {
        this.isRecording = false;
        this.isMediaMuxerStart = false;
        this.path = path;
        this.mMutexBeanQueue = new ArrayBlockingQueue(100);
        this.mMediaMuxerCallback = null;
    }

    private void prepareMediaMuxer(int width, int height) {
        try {
            mAudioTrack = -1;
            mVideoTrack = -1;
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mAudioRecordThread = new AudioRecordThread(this);
            mVideoRecordThread = new VideoRecordThread(this, width, height);
            mAudioRecordThread.prepare();
            mVideoRecordThread.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMediaMuxer() {
        if (!isMediaMuxerStart && isVideoTrackExist() && isAudioTrackExist()) {
            mMediaMuxer.start();
            isMediaMuxerStart = true;
            start();
        }
    }

    public void addAudioTrack(MediaFormat mediaFormat) {
        if (mMediaMuxer == null) {
            return;
        }
        mAudioTrack = mMediaMuxer.addTrack(mediaFormat);
        startMediaMuxer();
    }

    public void addVideoTrack(MediaFormat mediaFormat) {
        if (mMediaMuxer == null) {
            return;
        }
        mVideoTrack = mMediaMuxer.addTrack(mediaFormat);
        startMediaMuxer();
    }

    public boolean isAudioTrackExist() {
        return mAudioTrack >= 0;
    }

    public boolean isVideoTrackExist() {
        return mVideoTrack >= 0;
    }

    public void begin(int width, int height) {
        prepareMediaMuxer(width, height);
        isRecording = true;
        isMediaMuxerStart = false;
        mAudioRecordThread.begin();
        mVideoRecordThread.begin();
    }

    public void frame(byte[] data) {
        if (isRecording) {
            mVideoRecordThread.frame(data);
        }
    }

    public void end() {
        try {
            isRecording = false;
            mVideoRecordThread.end();
            mVideoRecordThread.join();
            mVideoRecordThread = null;
            mAudioRecordThread.end();
            mAudioRecordThread.join();
            mAudioRecordThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addMutexData(MutexBean data) {
        mMutexBeanQueue.offer(data);
    }

    @Override
    public void run() {
        while (true) {
            if (!mMutexBeanQueue.isEmpty()) {
                MutexBean data = mMutexBeanQueue.poll();
                if (data.isVideo()) {
                    mMediaMuxer.writeSampleData(mVideoTrack, data.getByteBuffer(), data.getBufferInfo());
                } else {
                    mMediaMuxer.writeSampleData(mAudioTrack, data.getByteBuffer(), data.getBufferInfo());
                }
            } else {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isRecording && mMutexBeanQueue.isEmpty()) {
                    break;
                }
            }
        }
        release();
        if (mMediaMuxerCallback != null) {
            mMediaMuxerCallback.onFinishMediaMutex(path);
        }
    }

    private void release() {
        if (mMediaMuxer != null && isMediaMuxerStart) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public boolean isMediaMuxerStart() {
        return isMediaMuxerStart;
    }

    public void setMediaMuxerCallback(MediaMuxerCallback callback) {
        this.mMediaMuxerCallback = callback;
    }

    public interface MediaMuxerCallback {
        void onFinishMediaMutex(String path);
    }


}
