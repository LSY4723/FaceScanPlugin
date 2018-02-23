package org.apache.cordova.FaceScanPlugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.FaceScanPlugin.CustomCameraActivity;
import org.json.JSONArray;
import org.json.JSONException;
public class FacePlugin extends CordovaPlugin {
    public static final String ERROR_MESSAGE = "ERROR";
    public CallbackContext callbackContext;
    private Intent intent;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!hasCamera()) {
            callbackContext.error("No camera detected");
            return false;
        }
        this.callbackContext = callbackContext;
        if (action.equals("faceScan")) {
            Context context = cordova.getActivity().getApplicationContext();
            intent = new Intent(context, CustomCameraActivity.class);
            this.faceScan(args, callbackContext);
            return true;
        }
        return false;
    }

    /**
     * 检查是否有摄像头
     *
     * @return
     */
    private boolean hasCamera() {
        Context context = cordova.getActivity().getApplicationContext();
        PackageManager manager = context.getPackageManager();
        return manager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    public void faceScan(final JSONArray args, CallbackContext callbackContext) {
        this.cordova.startActivityForResult(this, intent, 1);
    }

    public String byte2Base64String(byte[] b) {
        return Base64.encodeToString(b, 0);
    }

    public String byte2String(byte[] b) {
        String res = new String(b);
        return res;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            final byte[] datas = intent.getExtras().getByteArray("Data");
            final String data = byte2Base64String(datas);
            callbackContext.success(data);
        } else if(resultCode==Activity.RESULT_CANCELED){
        	callbackContext.error("请检查相机相关权限是否打开！");
           // this.faceScan(new JSONArray(), callbackContext);
        }else{
            Bundle extras = intent.getExtras();
            String strError = extras.getString(ERROR_MESSAGE);
            if (strError != null) {
                callbackContext.error(strError);
            } else {
                callbackContext.error("Unknown error");
            }
        }
    }
}
