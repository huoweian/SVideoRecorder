package us.pinguo.svideoDemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

//import com.libyuv.LibyuvUtil;
import com.libyuv.util.YuvUtil;

import us.pinguo.svideo.bean.VideoInfo;
import us.pinguo.svideo.interfaces.ICameraProxyForRecord;
import us.pinguo.svideo.interfaces.ISVideoRecorder;
import us.pinguo.svideo.interfaces.OnRecordListener;
import us.pinguo.svideo.interfaces.PreviewDataCallback;
import us.pinguo.svideo.interfaces.PreviewSurfaceListener;
import us.pinguo.svideo.interfaces.SurfaceCreatedCallback;
import us.pinguo.svideo.recorder.SAbsVideoRecorder;
import us.pinguo.svideo.recorder.SMediaCodecRecorder;
import us.pinguo.svideo.utils.RL;
import us.pinguo.svideoDemo.ui.BottomMenuView;
import us.pinguo.svideoDemo.ui.IBottomMenuView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class YuvRecordActivity extends Activity implements Camera.PreviewCallback, View.OnClickListener, OnRecordListener, IBottomMenuView, TextureView.SurfaceTextureListener {

    private Camera mCamera;
    private int mCameraId;

    private SAbsVideoRecorder mRecorder;
    private PreviewDataCallback mCallback;
    private Camera.Size mPreviewSize;
    private BottomMenuView mBottomMenuView;
    private ImageView mSwitchImg;
    private TextureView mTextureView;
    public int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private ImageView imageView;
    boolean showImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RL.setLogEnable(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yuvrecord);
        //imageView = findViewById(R.id.imageView);
        mBottomMenuView = findViewById(R.id.record_bottom_layout);
        mSwitchImg = findViewById(R.id.switch_camera);
        mSwitchImg.setOnClickListener(this);
        mTextureView = findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);
        ICameraProxyForRecord cameraProxyForRecord = new ICameraProxyForRecord() {

            @Override
            public void addSurfaceDataListener(PreviewSurfaceListener listener, SurfaceCreatedCallback callback) {

            }

            @Override
            public void removeSurfaceDataListener(PreviewSurfaceListener listener) {

            }

            @Override
            public void addPreviewDataCallback(PreviewDataCallback callback) {
                mCallback = callback;
            }

            @Override
            public void removePreviewDataCallback(PreviewDataCallback callback) {
                mCallback = null;
            }

            @Override
            public int getPreviewWidth() {
                return mPreviewSize.width;
            }

            @Override
            public int getPreviewHeight() {
                return mPreviewSize.height;
            }

            @Override
            public int getVideoRotation() {
                return mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? 90 : 270;
            }
        };
        mRecorder = new SMediaCodecRecorder(this, cameraProxyForRecord);
        mRecorder.addRecordListener(this);
        mBottomMenuView.setBottomViewCallBack(this);
        mBottomMenuView.enableSVideoTouch(true);
        mBottomMenuView.enableVideoProgressLayout();
    }

    private Camera.Size getPreviewSize() {
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        for (int i = 0; i < sizeList.size(); i++) {
            Camera.Size size = sizeList.get(i);
            if (size.width == 640 || size.width == 960 || size.width == 1280) {
                return size;
            }
        }
        return sizeList.get(0);
    }

    private void openCamera() {
        if (mCamera != null) {
            return;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int k = 0; k < Camera.getNumberOfCameras(); k++) {
            Camera.getCameraInfo(k, info);
            if (info.facing == mCameraFacing) {
                mCameraId = k;
                mCamera = Camera.open(k);
                break;
            }
        }
        if (mCamera == null) {
            throw new RuntimeException("Can't open frontal camera");
        }
    }

//    public static int setCameraDisplayOrientation(Activity activity,
//                                                  int cameraId, Camera camera) {
//        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }
//        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            //前置摄像头需要镜像,转化后进行设置
//            camera.setDisplayOrientation((360 - result) % 360);
//        } else {
//            result = (info.orientation - degrees + 360) % 360;
//            //后置摄像头直接进行显示
//            camera.setDisplayOrientation(result);
//        }
//        return result;
//    }

    private int getCameraDisplayOrientation(Activity activity, int cameraId) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private void startPreview(SurfaceTexture surfaceTexture) {
        openCamera();
        //setCameraDisplayOrientation(this,mCameraId,mCamera);
        mCamera.setDisplayOrientation(getCameraDisplayOrientation(this,mCameraId));//handle需要根据当前是前置还是后置摄像头和当前的屏幕方向进行设置
        //mCamera.setDisplayOrientation(90);
        Camera.Parameters parameters = mCamera.getParameters();
        mPreviewSize = getPreviewSize();
        mPreviewSize.width = 1280;
        mPreviewSize.height = 720;
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPreviewFormat(ImageFormat.NV21);
        List<String> focusModes = parameters.getSupportedFocusModes();
        for(String s :focusModes){
            if(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(s)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                break;
            }
        }
        mCamera.addCallbackBuffer(new byte[(int) (mPreviewSize.width * mPreviewSize.height * 1.5f)]);
        mCamera.setPreviewCallbackWithBuffer(this);
        mCamera.setParameters(parameters);
        adjustPreviewSize();
//        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    private void adjustPreviewSize() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTextureView.getLayoutParams();
        params.width = displayMetrics.widthPixels;
        params.height = (int) (mPreviewSize.width / (float) mPreviewSize.height * params.width);
        mTextureView.setLayoutParams(params);
//        if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//
//            mTextureView.setScaleX(-1);//用作水平镜像翻转，防止生成的视频图像水平颠倒；造成的问题是：前置摄像头横屏的时候图像上下翻转了
//        } else {
//            mTextureView.setScaleX(1);
//        }

//        mTextureView.setScaleX(1);

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        if (mCallback != null) {
            long timeUs = System.nanoTime() / 1000;

            //data = rotateYUVDegree90(data,mPreviewSize.height,mPreviewSize.width);

//            int scaleWidth = 720;
//            int scaleHeight = 1280;
//            int scaleWidth = 1920;
//            int scaleHeight = 1080;

            int scaleWidth = mPreviewSize.height;
            int scaleHeight = mPreviewSize.width;

            int cropWidth = scaleWidth;
            int cropHeight = scaleHeight;


            byte[] nv21 = data;
            byte[] nv12 = new byte[nv21.length];
            byte[] yuvI420 = new byte[nv21.length];
            byte[] tempYuvI420 = new byte[nv21.length];




            //final int rotation = setCameraDisplayOrientation(this,mCameraId,mCamera);
//            final boolean isFrontCamera = mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
//            final int rotation;
//            if(isFrontCamera){
//                rotation = 270;
//            }else{
//                rotation = 90;
//            }
//
//            LibyuvUtil.convertNV21ToI420(nv21, yuvI420, videoWidth, videoHeight);
//            //LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420, videoWidth, videoHeight, rotation, rotation == 270);
//            //LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12, videoWidth, videoHeight);
//
//
//            LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420 ,videoHeight,videoWidth, rotation, rotation == 270);
//            LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12,videoWidth,videoHeight);
//
//            mCallback.onPreviewData(nv12, timeUs);


            //test
            //进行yuv数据的缩放，旋转镜像缩放等操作
//            final byte[] dstData = new byte[scaleWidth * scaleHeight * 3 / 2];
//            final int morientation = 270;//setCameraDisplayOrientation(this,mCameraId,mCamera);
//            //YuvUtil.compressYUV(data, mPreviewSize.width, mPreviewSize.height, dstData, scaleHeight, scaleWidth, 0, morientation, morientation == 270);
//            YuvUtil.compressYUV(data, mPreviewSize.width, mPreviewSize.height, dstData, scaleHeight, scaleWidth, 0, morientation, true);
//
//            //进行yuv数据裁剪的操作
////                    final byte[] cropData = new byte[cropWidth * cropHeight * 3 / 2];
////                    YuvUtil.cropYUV(dstData, scaleWidth, scaleHeight, cropData, cropWidth, cropHeight, cropStartX, cropStartY);
//
//            //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
//            final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
//            YuvUtil.yuvI420ToNV21(dstData, nv21Data, cropWidth, cropHeight);
//
//
//            //final byte[] nv12Data = new byte[cropWidth * cropHeight * 3 / 2];
//            //YuvUtil.NV21ToNV12(nv21Data,nv12Data,cropWidth,cropHeight,nv12Data.length);
//
//
//            mCallback.onPreviewData(nv21Data, timeUs);
//
//


            //end test

            //test
//            final byte[] dstData = new byte[scaleWidth * scaleHeight * 3 / 2];
//            int morientation = setCameraDisplayOrientation(this,mCameraId,mCamera);
//
//            YuvUtil.compressYUV(data, mPreviewSize.width, mPreviewSize.height, dstData, mPreviewSize.height, mPreviewSize.width, 0, morientation, morientation == 270);
//            //YuvUtil.compressYUV(data, 1920, 1080, dstData, scaleHeight, scaleWidth, 0, morientation, morientation == 270);
//
//            //进行yuv数据裁剪的操作
////                    final byte[] cropData = new byte[cropWidth * cropHeight * 3 / 2];
////                    YuvUtil.cropYUV(dstData, scaleWidth, scaleHeight, cropData, cropWidth, cropHeight, 0, 0);
//
//            //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
//            final byte[] nv21Data = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2];
//            YuvUtil.yuvI420ToNV21(dstData, nv21Data, mPreviewSize.width, mPreviewSize.height);
//            //YuvUtil.yuvI420ToNV21(dstData, nv21Data, mPreviewSize.height, mPreviewSize.width);
//
//            mCallback.onPreviewData(nv21Data, timeUs);

            //end test

//            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, cropWidth, cropHeight, null);
//            ByteArrayOutputStream fOut = new ByteArrayOutputStream();
//            yuvImage.compressToJpeg(new Rect(0, 0, cropWidth, cropHeight), 100, fOut);

            showImage = true;
            if(showImage) {
                //将byte生成bitmap
//                byte[] bitData = fOut.toByteArray();
//                final Bitmap bitmap = BitmapFactory.decodeByteArray(bitData, 0, bitData.length);
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        imageView.setImageBitmap(bitmap);
//                    }
//                });


                int videoWidth = mPreviewSize.width;
                int videoHeight = mPreviewSize.height;

//                videoWidth = mPreviewSize.height;
//                videoHeight = mPreviewSize.width;

//                videoWidth = 1280;
//                videoHeight = 720;



//                //test
//
//                //Mirror(data,videoWidth, videoHeight);
//                //nv21 = rotateYUVDegree270AndMirror(data,videoWidth, videoHeight);
//                LibyuvUtil.convertNV21ToI420(nv21, yuvI420, videoWidth, videoHeight);
//                //LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420, videoWidth, videoHeight, rotation, rotation == 270);
////                LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420,  videoHeight, videoWidth,rotation, rotation == 270);
//                LibyuvUtil.convertI420ToNV12(yuvI420, nv12, videoWidth, videoHeight);
//                //LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12, videoWidth, videoHeight);
//
////                LibyuvUtil.convertNV21ToI420(nv21, yuvI420, videoWidth, videoHeight);
////                LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420, videoWidth, videoHeight, rotation, rotation == 270);
////                LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12, videoWidth, videoHeight);
//
//
//                //end test



//                YuvUtil.compressYUV(nv21, videoWidth,videoHeight, yuvI420, videoHeight, videoWidth, 0, rotation, rotation == 270);
//
//                //final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
//
//
//                //进行yuv数据裁剪的操作
//                final byte[] cropData = new byte[videoWidth * videoHeight * 3 / 2];
//                YuvUtil.cropYUV(yuvI420, videoWidth, videoHeight, cropData, videoWidth, videoHeight, 0, 0);
//
//                YuvUtil.yuvI420ToNV21(yuvI420, nv12, videoWidth, videoHeight);
//                //YuvUtil.yuvI420ToNV12(yuvI420, nv12, videoWidth, videoHeight);



                //mCallback.onPreviewData(nv12, timeUs);


//                YuvUtil.compressYUV(nv21, videoWidth,videoHeight, yuvI420,videoWidth, videoHeight,  0, rotation, rotation == 270);
//
//                //进行yuv数据裁剪的操作
//                final byte[] cropData = new byte[videoWidth * videoHeight * 3 / 2];
//                YuvUtil.cropYUV(yuvI420, videoWidth, videoHeight, cropData, videoWidth, videoHeight, 0, 0);
//
//
//                final byte[] nv21Data = new byte[videoWidth * videoHeight * 3 / 2];
//                YuvUtil.yuvI420ToNV12(yuvI420, nv12, videoWidth, videoHeight);






                //YuvUtil.yuvI420ToNV21(cropData,nv21Data,videoWidth, videoHeight);



//                YuvUtil.compressYUV(srcData, mCameraUtil.getCameraWidth(), mCameraUtil.getCameraHeight(), dstData, scaleHeight, scaleWidth, 0, morientation, morientation == 270);
//
//                //进行yuv数据裁剪的操作
//                final byte[] cropData = new byte[cropWidth * cropHeight * 3 / 2];
//                YuvUtil.cropYUV(dstData, scaleWidth, scaleHeight, cropData, cropWidth, cropHeight, cropStartX, cropStartY);
//
//                //这里将yuvi420转化为nv21，因为yuvimage只能操作nv21和yv12，为了演示方便，这里做一步转化的操作
//                final byte[] nv21Data = new byte[cropWidth * cropHeight * 3 / 2];
//                YuvUtil.yuvI420ToNV21(cropData, nv21Data, cropWidth, cropHeight);




                final boolean isFrontCamera = mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
                final int rotation;
                if(isFrontCamera){
                    rotation = 270;
                }else{
                    rotation = 90;
                }



                YuvUtil.compressYUV(nv21, videoWidth,videoHeight, yuvI420,videoWidth,videoHeight,   0, rotation, rotation == 270);

                //进行yuv数据裁剪的操作
//                final byte[] cropData = new byte[videoWidth * videoHeight * 3 / 2];
//                YuvUtil.cropYUV(yuvI420, videoWidth, videoHeight, cropData, videoWidth, videoHeight, 0, 0);


                //final byte[] nv21Data = new byte[videoWidth * videoHeight * 3 / 2];
                YuvUtil.yuvI420ToNV12(yuvI420, nv12, videoWidth, videoHeight);




//                LibyuvUtil.convertNV21ToI420(nv21, yuvI420, videoWidth, videoHeight);
//                LibyuvUtil.compressI420(yuvI420, videoWidth, videoHeight, tempYuvI420, videoWidth, videoHeight, rotation, isFrontCamera);
//                LibyuvUtil.convertI420ToNV12(tempYuvI420, nv12, videoWidth, videoHeight);



//
                mCallback.onPreviewData(nv12, timeUs);

                //mCallback.onPreviewData(data, timeUs);

//
                videoWidth = mPreviewSize.height;
                videoHeight = mPreviewSize.width;

//                LibyuvUtil.convertNV21ToI420(nv21, yuvI420, mPreviewSize.width, mPreviewSize.height);
//                LibyuvUtil.compressI420(yuvI420, mPreviewSize.width, mPreviewSize.height, tempYuvI420,
//                        mPreviewSize.width, mPreviewSize.height, rotation, rotation == 270);
//
//
//
//
//
//                final Bitmap bitmap2 = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888);
//
//                LibyuvUtil.convertI420ToBitmap(tempYuvI420, bitmap2, videoWidth, videoHeight);
//
////                String photoPath = LanSongFileUtil.DEFAULT_DIR+System.currentTimeMillis()+".jpeg";
////                FileOutputStream fos = null;
////                try {
////                    fos = new FileOutputStream(photoPath);
////                } catch (FileNotFoundException e) {
////                    e.printStackTrace();
////                }
////                bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        imageView.setImageBitmap(bitmap2);
//                    }
//                });

            }
            showImage = false;




            //Mirror(data,mPreviewSize.width, mPreviewSize.height);


            //mTextureView.setScaleX(-1);

            //mCallback.onPreviewData(data, timeUs);
        }

    }

    private byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate and mirror the Y luma
        int i = 0;
        int maxY = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            maxY = imageWidth * (imageHeight - 1) + x * 2;
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[maxY - (y * imageWidth + x)];
                i++;
            }
        }
        // Rotate and mirror the U and V color components
        int uvSize = imageWidth * imageHeight;
        i = uvSize;
        int maxUV = 0;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
                i++;
                yuv[i] = data[maxUV - (y * imageWidth + x)];
                i++;
            }
        }
        return yuv;
    }

    private void Mirror(byte[] src, int w, int h) { //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
                b--;
            }
        }

        // mirror u and v
        index = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w;
            b = (i + 1) * w - 2;
            while (a < b) {
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;

                temp = src[a + index + 1];
                src[a + index + 1] = src[b + index + 1];
                src[b + index + 1] = temp;
                a+=2;
                b-=2;
            }
        }
    }

    private byte[] rotateYUVDegree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    @Override
    public void onClick(View v) {
        if (v == mSwitchImg) {
            switchCamera();
        }
    }

    private void switchCamera() {
        mCameraFacing = mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
//        openCamera();
        startPreview(mTextureView.getSurfaceTexture());
    }

    @Override
    public void onRecordSuccess(VideoInfo videoInfo) {
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(PreviewActivity.FILEPATH, videoInfo.getVideoPath());
        startActivity(intent);
    }

    @Override
    public void onRecordStart() {
        Log.i("hwLog", "onRecordStart");
    }

    @Override
    public void onRecordFail(Throwable t) {
        Log.e("hwLog", Log.getStackTraceString(t));
    }

    @Override
    public void onRecordStop() {
    }

    @Override
    public void onRecordPause() {

    }

    @Override
    public void onRecordResume() {

    }

    @Override
    public ISVideoRecorder requestRecordListener() {
        return mRecorder;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBottomMenuView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBottomMenuView.onResume();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startPreview(surface);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
