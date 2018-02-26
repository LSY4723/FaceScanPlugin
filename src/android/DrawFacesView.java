package org.apache.cordova.FaceScanPlugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

public class DrawFacesView extends View {
    private Matrix matrix;
    private Paint paint;
    private Camera.Face[] faces;
    private boolean isClear;
    public DrawFacesView(Context context) {
        this(context, null);
    }

    public DrawFacesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawFacesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(12);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        //设置线条粗细
        paint.setAlpha(250);
        faces = new Camera.Face[]{};
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.setMatrix(matrix);
        for (Camera.Face face : faces) {
            if (face == null) break;

            if (face.rightEye != null)
                canvas.drawPoint(-face.rightEye.x, face.rightEye.y, paint);
            if (face.leftEye != null)
                canvas.drawPoint(-face.leftEye.x, face.leftEye.y, paint);
            if (face.mouth != null)
                canvas.drawPoint(-face.mouth.x, face.mouth.y, paint);
            //左上方(第一行竖)
            canvas.drawLine(-face.rightEye.x-200, face.rightEye.y+100, -face.rightEye.x-100, face.rightEye.y+100, paint);
            //第二行横
            canvas.drawLine(-face.rightEye.x-200, face.rightEye.y-100, -face.rightEye.x-200, face.rightEye.y+100, paint);
//            //左下方
            int i = Math.abs(face.rightEye.x) - Math.abs(face.mouth.x);
            canvas.drawLine(-face.mouth.x+100, face.rightEye.y+100, -face.mouth.x, face.rightEye.y+100, paint);
            canvas.drawLine(-face.mouth.x+100, face.rightEye.y-100, -face.mouth.x+100, face.rightEye.y+100, paint);
            //右上方
            canvas.drawLine(-face.leftEye.x-100, face.leftEye.y-100, -face.leftEye.x-200, face.leftEye.y-100, paint);
            canvas.drawLine(-face.leftEye.x-200, face.leftEye.y+100, -face.leftEye.x-200, face.leftEye.y-100, paint);
//            //右下方
            canvas.drawLine(-face.mouth.x+100, face.leftEye.y-100, -face.mouth.x, face.leftEye.y-100, paint);
            canvas.drawLine(-face.mouth.x+100, face.leftEye.y-100, -face.mouth.x+100, face.leftEye.y+100, paint);
        }
        if (isClear) {
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            isClear = false;
        }

    }

    /**
     * 绘制脸部方框
     *
     * @param matrix 旋转画布的矩阵
     * @param faces  脸部信息数组
     */
    public void updateFaces(Matrix matrix, Camera.Face[] faces) {
        this.matrix = matrix;
        this.faces = faces;
        invalidate();
    }

    /**
     * 清除已经画上去的框
     */
    public void removeRect() {
        isClear = true;
        invalidate();
    }
}
