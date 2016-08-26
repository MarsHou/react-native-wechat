package com.reactnativewechat;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Mars on 8/26/16 15:44.
 */
public class WechatModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    public static boolean isWechatPay = false;
    public static int payCode;
    private static final String WECHAT = "Wechat";
    private String wechatPayEvent = "wechatPayEvent";
    private ReactContext mReactContext;
    private IWXAPI mIwxapi;
    public static String mAppId;

    public WechatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mReactContext = reactContext;
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return WECHAT;
    }

    @ReactMethod
    public void registerApp(String appId, Promise promise) {
        if (TextUtils.isEmpty(appId)) {
            promise.resolve(false);
            return;
        }
        mAppId = appId;
        mIwxapi = WXAPIFactory.createWXAPI(mReactContext, appId, true);
        promise.resolve(mIwxapi.registerApp(appId));
    }

    @ReactMethod
    public void wechatPay(ReadableMap map, String payEvent, Promise promise) {
        if (mIwxapi != null) {
            this.wechatPayEvent = payEvent;
            PayReq req = new PayReq();
            req.packageValue = "Sign=WXPay";
            if (map.getString("appId") != null && map.getString("partnerId") != null && map.getString("prepayId") != null && map.getString("nonceStr") != null && map.getString("timeStamp") != null && map.getString("sign") != null) {
                req.appId = map.getString("appId");
                req.partnerId = map.getString("partnerId");
                req.prepayId = map.getString("prepayId");
                req.nonceStr = map.getString("nonceStr");
                req.timeStamp = map.getString("timeStamp");
                req.sign = map.getString("sign");
            } else if (map.getString("key") != null && map.getString("partnerId") != null && map.getString("prepayId") != null && mAppId != null) {
                req.appId = mAppId;
                req.partnerId = map.getString("partnerId");
                req.prepayId = map.getString("prepayId");
                req.nonceStr = getNonceStr();
                req.timeStamp = String.valueOf(getTimeStamp());
                req.sign = getSign(req.appId, req.nonceStr, req.packageValue, req.partnerId, req.prepayId, req.timeStamp, map.getString("key"));
            } else {
                Toast.makeText(mReactContext, "Whether to register? or at least container 'partnerId','partnerId','key'.", Toast.LENGTH_SHORT).show();
            }
            promise.resolve(mIwxapi.sendReq(req));
        } else {
            Toast.makeText(mReactContext, "Whether to register?", Toast.LENGTH_SHORT).show();
            promise.resolve(false);
        }
    }


    @ReactMethod
    public void openWechat() {
        if (mIwxapi != null) {
            mIwxapi.openWXApp();
        } else {
            Toast.makeText(mReactContext, "Whether to register?", Toast.LENGTH_SHORT).show();
        }
    }

    @ReactMethod
    public void isWechatInstalled(Promise promise) {
        if (mIwxapi != null) {
            promise.resolve(mIwxapi.isWXAppInstalled());
        } else {
            Toast.makeText(mReactContext, "Whether to register? ", Toast.LENGTH_SHORT).show();
            promise.resolve(false);
        }
    }


    @Override
    public void onHostResume() {
        if (isWechatPay) {
            isWechatPay = false;
            WritableMap params = Arguments.createMap();
            params.putInt("wechatStatus", payCode);
            if (payCode == 0) {
                params.putString("wechatMsg", "success");
            } else {
                params.putString("wechatMsg", "error");
            }
            sendEvent(getReactApplicationContext(), wechatPayEvent, params);
        } else {
        }
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {

    }


    /**
     * 获取32位大写签名
     *
     * @return
     */
    private String getSign(String appid, String noncestr, String packageVlue, String partnerid, String prepayid, String timestamp, String key) {
        String sign = "appid=" + appid + "&noncestr=" + noncestr + "&package=" + packageVlue + "&partnerid=" + partnerid + "&prepayid=" + prepayid + "&timestamp=" + timestamp + "&key=" + key;
        return encryption(sign).toUpperCase();
    }

    /**
     * 随机生成32位字符串
     *
     * @return
     */
    private String getNonceStr() {
        return encryption(String.valueOf(new Random().nextInt(10000))).toUpperCase();
    }

    /**
     * 10位数时间戳 单位秒
     *
     * @return
     */
    private long getTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * md5 加密 返回32位小数
     *
     * @param plain
     * @return
     */
    private static String encryption(String plain) {
        String md5String = new String();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plain.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuffer stringBuffer = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    stringBuffer.append("0");
                stringBuffer.append(Integer.toHexString(i));
            }

            md5String = stringBuffer.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5String;
    }
}
