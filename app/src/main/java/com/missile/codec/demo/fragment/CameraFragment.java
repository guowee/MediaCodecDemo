package com.missile.codec.demo.fragment;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.missile.codec.demo.R;
import com.missile.codec.demo.record.CameraSensor;
import com.missile.codec.demo.record.MediaMuxerThread;
import com.missile.codec.demo.util.CameraUtil;
import com.missile.codec.demo.util.ImageUtil;
import com.missile.codec.demo.util.PermisstionUtil;
import com.missile.codec.demo.util.StorageUtil;
import com.missile.codec.demo.widget.CameraFocusView;
import com.missile.codec.demo.widget.CameraProgressButton;
import com.missile.codec.demo.widget.CameraSwitchView;

public class CameraFragment extends Fragment implements CameraProgressButton.Listener, CameraSensor.CameraSensorListener, MediaMuxerThread.MediaMuxerCallback, TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    private final static int CAMERA_REQUEST_CODE = 1;
    private final static int STORE_REQUEST_CODE = 2;
    private TextureView mCameraView;
    private CameraSensor mCameraSensor;
    private CameraProgressButton mProgressBtn;
    private CameraFocusView mFocusView;
    private CameraSwitchView mSwitchView;
    // 是否正在对焦
    private boolean isFocusing;
    private Size mPreviewSize = null;
    private boolean isTakePhoto;
    private boolean isRecording;

    private MediaMuxerThread mMediaMuxer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_camera, container, false);
        initView(contentView);
        return contentView;
    }

    private void initView(View contentView) {
        isFocusing = false;
        isTakePhoto = false;
        isRecording = false;

        mCameraView = contentView.findViewById(R.id.camera_view);
        mProgressBtn = contentView.findViewById(R.id.progress_btn);
        mFocusView = contentView.findViewById(R.id.focus_view);
        mSwitchView = contentView.findViewById(R.id.switch_view);

        mCameraSensor = new CameraSensor(getContext());
        mCameraSensor.setCameraSensorListener(this);
        mCameraView.setSurfaceTextureListener(this);
        mProgressBtn.setProgressListener(this);

        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    focus((int) event.getX(), (int) event.getY(), false);
                    return true;
                }
                return false;
            }
        });
        mSwitchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFocusView.cancelFocus();
                if (mPreviewSize != null) {
                    CameraUtil.switchCamera(getActivity(), !CameraUtil.isBackCamera(), mCameraView.getSurfaceTexture(), mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }
            }
        });
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mPreviewSize = new Size(camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height);

        if (isTakePhoto) {
            String dirPath = StorageUtil.getImagePath();
            StorageUtil.checkDirExist(dirPath);
            boolean result = ImageUtil.saveNV21(data, mPreviewSize.getWidth(), mPreviewSize.getHeight(), dirPath + "image.jpg");
            isTakePhoto = false;
            if (result) {
               /* Intent intent = new Intent(getContext(), ImageActivity.class);
                intent.putExtra("path", dirPath + "image.jpg");
                startActivity(intent);*/
            }
        } else if (isRecording) {
            mMediaMuxer.frame(data);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e("TAG", "width * height: " + width + " * " + height);
        mPreviewSize = new Size(width, height);
        startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        releasePreview();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onShortPress() {
        if (requestPermission()) {
            takePicture();
        }
    }

    @Override
    public void onStartLongPress() {
        if (requestPermission()) {
            beginRecord();
        }
    }

    @Override
    public void onEndLongPress() {
        endRecord();
    }

    @Override
    public void onEndMaxProgress() {
        endRecord();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPreview();
    }


    public void startPreview() {
        if (requestPermission()) {
            if (CameraUtil.getCamera() == null) {
                CameraUtil.openCamera();
            }
            if (mPreviewSize != null) {
                CameraUtil.startPreview(getActivity(), mCameraView.getSurfaceTexture(), mPreviewSize.getWidth(), mPreviewSize.getHeight());
                CameraUtil.setPreviewCallback(this);
                mCameraSensor.start();
                mSwitchView.setOrientation(mCameraSensor.getX(), mCameraSensor.getY(), mCameraSensor.getZ());
            }
        }
    }

    public void releasePreview() {
        CameraUtil.setPreviewCallback(null);
        CameraUtil.releaseCamera();
        mCameraSensor.stop();
        mFocusView.cancelFocus();
    }

    public void takePicture() {
        isTakePhoto = true;
    }

    private void beginRecord() {
        if (mPreviewSize != null) {
            isRecording = true;
            String dirPath = StorageUtil.getVideoPath();
            StorageUtil.checkDirExist(dirPath);
            mMediaMuxer = new MediaMuxerThread(dirPath + "video.mp4");
            mMediaMuxer.setMediaMuxerCallback(this);
            mMediaMuxer.begin(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        }
    }

    private void endRecord() {
        if (isRecording) {
            isRecording = false;
            mMediaMuxer.end();
        }
    }


    @SuppressWarnings("deprecation")
    private void focus(final int x, final int y, final boolean isAutoFocus) {
        if (!CameraUtil.isBackCamera()) {
            return;
        }
        if (isFocusing && isAutoFocus) {
            return;
        }
        isFocusing = true;
        Point focusPoint = new Point(x, y);
        android.util.Size screenSize = new android.util.Size(mCameraView.getWidth(), mCameraView.getHeight());
        if (!isAutoFocus) {
            mFocusView.beginFocus(x, y);
        }
        CameraUtil.newCameraFocus(focusPoint, screenSize, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                isFocusing = false;
                if (!isAutoFocus) {
                    mFocusView.endFocus(success);
                }
            }
        });
    }


    private boolean requestPermission() {
        return requestCameraPermission() && requestAudioPermission() && requestStoragePermission();
    }

    private boolean requestCameraPermission() {
        return PermisstionUtil.checkPermissionsAndRequest(getContext(), PermisstionUtil.CAMERA, CAMERA_REQUEST_CODE, "请求相机权限被拒绝");
    }

    private boolean requestStoragePermission() {
        return PermisstionUtil.checkPermissionsAndRequest(getContext(), PermisstionUtil.STORAGE, STORE_REQUEST_CODE, "请求访问SD卡权限被拒绝");
    }

    private boolean requestAudioPermission() {
        return PermisstionUtil.checkPermissionsAndRequest(getContext(), PermisstionUtil.MICROPHONE, STORE_REQUEST_CODE, "请求访问SD卡权限被拒绝");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestPermission()) {
            startPreview();
        }
    }

    @Override
    public void onFinishMediaMutex(String path) {

    }

    @Override
    public void onRock() {
        if (CameraUtil.isBackCamera() && CameraUtil.getCamera() != null) {
            focus(mCameraView.getWidth() / 2, mCameraView.getHeight() / 2, true);
        }
        mSwitchView.setOrientation(mCameraSensor.getX(), mCameraSensor.getY(), mCameraSensor.getZ());
    }
}
