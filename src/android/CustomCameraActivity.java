package org.apache.cordova.FaceScanPlugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import org.apache.cordova.CordovaActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;



public class CustomCameraActivity extends CordovaActivity {
    private static final String TAG = CustomCameraActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_CODE = 0x100;
    private SurfaceView surfaceView;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private DrawFacesView facesView;
    private RelativeLayout layout;
    private FrameLayout layoutCamera;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        layoutCamera = new FrameLayout(this);
        FrameLayout.LayoutParams layoutCameraParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutCamera.setLayoutParams(layoutCameraParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(!cameraIsCanUse()){
                //requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_CODE);
              	setResult(RESULT_CANCELED, new Intent());
                CustomCameraActivity.this.finish();
            }
        }
        layout.addView(layoutCamera);
        setContentView(layout);
        initViews();
        openSurfaceView();

    }
    /**
     * 通过尝试打开相机的方式判断有无拍照权限（在6.0以下使用拥有root权限的管理软件可以管理权限）
     *
     * @return
     */
    public static boolean cameraIsCanUse() {
        boolean isCanUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            Camera.Parameters mParameters = mCamera.getParameters();
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            isCanUse = false;
        }

        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
                return isCanUse;
            }
        }
        return isCanUse;
    }

    /**
     * 把摄像头的图像显示到SurfaceView
     */
    private void openSurfaceView() {
        mHolder = surfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mCamera == null) {
                    //调用前置摄像头
                    int cameraId = findFrontCamera();

                    Log.i(TAG, "==========前置摄像头cameraId===========" + cameraId);
                    if (cameraId != -1) {
                        mCamera = Camera.open(cameraId);
                    } else {
                        mCamera = Camera.open();
                    }
                    try {
                        mCamera.setFaceDetectionListener(new FaceDetectorListener());
                        mCamera.setPreviewDisplay(holder);
                        startFaceDetection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mHolder.getSurface() == null) {
                    Log.e(TAG, "mHolder.getSurface() == null");
                    return;
                }

                try {
                    mCamera.stopPreview();

                } catch (Exception e) {
                    Log.e(TAG, "Error stopping camera preview: " + e.getMessage());
                }
                try {
                    mCamera.setPreviewDisplay(mHolder);
                    int measuredWidth = surfaceView.getMeasuredWidth();
                    int measuredHeight = surfaceView.getMeasuredHeight();
                    setCameraParms(mCamera, measuredWidth, measuredHeight);
                    mCamera.startPreview();
                    startFaceDetection();

                } catch (Exception e) {
                    Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.lock();
                mCamera.release();
                mCamera = null;
            }
        });
    }

    private void initViews() {
        surfaceView = new SurfaceView(this);
        facesView = new DrawFacesView(this);
        addContentView(surfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        addContentView(facesView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /**
     * 防止自动拍照时候多次拍照
     */
    private boolean isCheckd = true;

    /**
     * 脸部检测接口
     */
    private class FaceDetectorListener implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    if (faces.length > 0) {
                        Camera.Face face = faces[0];
                        Rect rect = face.rect;
                        Matrix matrix = updateFaceRect();
                        facesView.updateFaces(matrix, faces);
                        Log.d("FaceDetection", "可信度：" + face.score + "---face detected: " + faces.length + " Face 1 Location X: " + rect.centerX() + "Y: " + rect.centerY() + "   " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom);
                        if (face.score >= 70) {
//                            int left = rect.left;
//                            int right = rect.right;
//                            int top = rect.top;
//                            int bottom = rect.bottom;
//                            Camera.Parameters parameters = camera.getParameters();
//                            Camera.Size previewSize = parameters.getPreviewSize();
//                            double width = (previewSize.width / 2) * 1.1;
//                            double height = (previewSize.height / 2) * 1.1;
//                            Log.d("FaceDetection", "可信度：" + face.score + "---face detected: " + faces.length + " Face 1 Location X--: " + rect.centerX() +
//                                    "Y--: " + rect.centerY() + " left:  " + left + "top: " + top + "right: " + right + "bottom: " + bottom);
//                            if (left >= -width && right <= width && top >= -height && bottom <= height) {
                                if (isCheckd) {
                                    camera.takePicture(null, null, new PicCallBacKImpl());
                                    Log.d(facesView.toString(), "脸部视图");
                                    isCheckd = false;
                                }
                            //}
                        }
                    } else {
                        // 只会执行一次
                        Log.e("tag", "【CustomCameraActivity】类的方法：【onFaceDetection】: " + "没有脸部");
                        facesView.removeRect();
                    }

                }
            });
        }
    }
    private class ShutCallBackImpl implements Camera.ShutterCallback {
        @Override
        public void onShutter() {

        }
    }
    /**
     * 拍照
     */
    private class PicCallBacKImpl implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            Intent intent = new Intent();
            Bitmap bitmap = Bytes2Bimap(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] datas = baos.toByteArray();
            savePicture(datas);
            intent.putExtra("Data",datas);
            setResult(RESULT_OK, intent);
            CustomCameraActivity.this.finish();
        }
    }
    public String savePicture(byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + "/111111.png";
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),System.currentTimeMillis()+".png");
            Log.i("---path----",Environment.getExternalStorageDirectory().getAbsolutePath()+"--");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return path;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            // 利用Bitmap对象创建缩略图
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 300, 300);
            return bitmap;
        } else {
            return null;
        }
    }
    /**
     * 摄像头
     *
     * @return
     */
    public int findFrontCamera() {
        int cameraId = -1;
        int numberCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    /**
     * 因为对摄像头进行了旋转，所以同时也旋转画板矩阵
     * 详细请查看{@link Camera.Face#rect}
     *
     * @return
     */
    private Matrix updateFaceRect() {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        boolean mirror = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        matrix.setScale(mirror ? -1 : 1, 1);
        matrix.postRotate(90);
        matrix.postScale(surfaceView.getWidth() / 1800f, surfaceView.getHeight() / 1600f);
        matrix.postTranslate(surfaceView.getWidth() / 2f, surfaceView.getHeight() / 2f);
        return matrix;
    }

    public void startFaceDetection() {
        Camera.Parameters params = mCamera.getParameters();
        if (params.getMaxNumDetectedFaces() > 0) {
            mCamera.startFaceDetection();
        } else {
            Log.e("tag", "【FaceDetectorActivity】类的方法：【startFaceDetection】: " + "不支持");
        }
    }

    /**
     * 在摄像头启动前设置参数
     *
     * @param camera
     * @param width
     * @param height
     */
    private void setCameraParms(Camera camera, int width, int height) {
        // 获取摄像头支持的pictureSize列表
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        // 从列表中选择合适的分辨率
        Camera.Size pictureSize = getProperSize(pictureSizeList, (float) height / width);
        if (null == pictureSize) {
            pictureSize = parameters.getPictureSize();
        }
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = pictureSize.width;
        float h = pictureSize.height;
        parameters.setPictureSize(pictureSize.width/2, pictureSize.height/2);
        parameters.set("orientation", "portrait");
        parameters.setRotation(270);
        surfaceView.setLayoutParams(new FrameLayout.LayoutParams((int) (height * (h / w)), height));

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = getProperSize(previewSizeList, (float) height / width);
        if (null != preSize) {
            parameters.setPreviewSize(preSize.width, preSize.height);
        }
        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // 连续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.cancelAutoFocus();
        camera.setDisplayOrientation(90);
        camera.setParameters(parameters);
    }


    private Camera.Size getProperSize(List<Camera.Size> pictureSizes, float screenRatio) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizes) {
            float currenRatio = ((float) size.width) / size.height;
            if (currenRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }
        if (null == result) {
            for (Camera.Size size : pictureSizes) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {
                    result = size;
                    break;
                }
            }
        }
        return result;
    }
}
