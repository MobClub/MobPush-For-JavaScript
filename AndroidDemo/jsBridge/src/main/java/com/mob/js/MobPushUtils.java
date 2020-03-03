package com.mob.js;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mob.MobSDK;
import com.mob.OperationCallback;
import com.mob.js.listener.MobPushDemoListener;
import com.mob.js.listener.MobPushListener;
import com.mob.js.listener.MobPushRegIdCallback;
import com.mob.pushsdk.MobPush;
import com.mob.pushsdk.MobPushCustomNotification;
import com.mob.pushsdk.MobPushLocalNotification;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.ResHelper;
import com.mob.tools.utils.UIHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class MobPushUtils extends WebViewClient implements Handler.Callback {
    private static final String GET_REGISTRATION_ID = "getRegistrationID";
    private static final String ADD_PUSH_RECEIVER = "addPushReceiver";
    public static final String SET_ALIAS = "setAlias";
    public static final String GET_ALIAS = "getAlias";
    public static final String DELETE_ALIAS = "deleteAlias";
    public static final String ADD_TAGS = "addTags";
    public static final String GET_TAGS = "getTags";
    public static final String DELETE_TAGS = "deleteTags";
    public static final String CLEAN_TAGS = "cleanAllTags";
    public static final String SEND_CUSTOM_MSG = "sendCustomMsg";
    public static final String SEND_APNS_MSG = "sendAPNsMsg";
    public static final String SEND_LOCAL_NOTIFY = "sendLocalNotify";
    public static final String SET_NOTIFY_ICON = "setNotifyIcon";
    public static final String SET_APP_FOREGROUND_HIDDEN_NOTIFICATION = "setAppForegroundHiddenNotification";
    public static final String PRIVACY_PERMISSION_STATUS = "privacyPermissionStatus";

    public static final int MSG_LOAD_URL = 1; // load js script
    public static final int MSG_JS_CALL = 2; // process js callback on ui thread

    private WebView webview;
    private SSDKWebViewClient wvClient;
    private Hashon hashon;
    private MobPushListener mobPushListener;


    public static MobPushUtils prepare(WebView webview, WebViewClient wvClient) {
        return new MobPushUtils(webview, wvClient);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private MobPushUtils(WebView webview, WebViewClient wbClient) {
        hashon = new Hashon();

        this.webview = webview;
        this.wvClient = new SSDKWebViewClient(this);
        this.wvClient.setWebViewClient(wbClient);
        this.webview.setWebViewClient(this.wvClient);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "JSInterfaceForPush");
    }

    /* process js init function */
    void onInit() {
        // platform type: 1 for android, 2 for ios
        Log.d("MobPushUtils ===", "initPushSDK");
        String script = "javascript:$mobpush.initPushSDK(1);";
        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = script;
        UIHandler.sendMessage(msg, this);
    }

    /**
     * receive js callback
     * <p>
     * respons: {
     * "seqId" : "111111",
     * "platform" : 1,
     * "state" : 1, // Success = 1, Fail = 2, Cancel = 3
     * "data" : "user or share response",
     * "method":"geiUserInfo",
     * "callback" : "function string",
     * "error" :
     * {
     * "error_level" : 1,
     * "error_code" : 11,
     * "error_msg" : "adsfdsaf",
     * }
     * }
     */
    @JavascriptInterface
    public void jsCallback(String seqId, String api, String data, String callback) {
        // this is in webview core thread, not in ui thread
        Message msg = new Message();
        msg.what = MSG_JS_CALL;
        msg.obj = new Object[]{seqId, api, data, callback};
        UIHandler.sendMessage(msg, this);
    }

    /**
     * receive js log
     */
    @JavascriptInterface
    public void jsLog(String msg) {
        Log.w("MobPush for JS", msg == null ? "" : msg);
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_LOAD_URL: {
                webview.loadUrl(String.valueOf(msg.obj));
            }
            break;
            case MSG_JS_CALL: {
                jsCallback((Object[]) msg.obj);
            }
            break;
        }
        return false;
    }

    /**
     * callback main method
     * Every request is allocated by this function call,
     * Including the number of requests, the callback and data
     *
     * @param objs
     */
    private void jsCallback(Object[] objs) {
        String seqId = (String) objs[0];
        String api = (String) objs[1];
        String data = (String) objs[2];
        String callback = (String) objs[3];

        HashMap<String, Object> req = null;
        try {
            req = hashon.fromJson(data);
            if (req == null) {
                Throwable t = new Throwable("wrong request data: " + data);
                onRequestFailed(seqId, api, callback, null, t);
                return;
            }
        } catch (Throwable t) {
            onRequestFailed(seqId, api, callback, null, t);
            return;
        }
        String oriCallback = (String) req.get("callback");
        HashMap<String, Object> resp = new HashMap<String, Object>();
        resp.put("seqId", seqId);
        resp.put("method", api);
        resp.put("callback", oriCallback);
        if (GET_REGISTRATION_ID.equals(api)) {
            getRegistrationId(seqId, api, callback, oriCallback);
            return;
        } else if (ADD_PUSH_RECEIVER.equals(api)) {
            addPushReceiver(seqId, api, callback, oriCallback);
        } else if (SET_ALIAS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                if (parmsMap != null && parmsMap.containsKey("alias")) {
                    String alias = (String) parmsMap.get("alias");
                    setAlias(alias);
                }
            }
        } else if (GET_ALIAS.equals(api)) {
            getAlias();
        } else if (DELETE_ALIAS.equals(api)) {
            deleteAlias();
        } else if (ADD_TAGS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                ArrayList<String> tags = (ArrayList<String>) parmsMap.get("tags");
                String[] ts = tags.toArray(new String[]{});
                addTags(ts);
            }
        } else if (GET_TAGS.equals(api)) {
            getTags();
        } else if (DELETE_TAGS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                ArrayList<String> tags = (ArrayList<String>) parmsMap.get("tags");
                String[] ts = tags.toArray(new String[]{});
                deleteTags(ts);
            }
        } else if (CLEAN_TAGS.equals(api)) {
            cleanTags();
        } else if (SEND_CUSTOM_MSG.equals(api) || SEND_APNS_MSG.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                String text = (String) parmsMap.get("content");
                int space = 0;
                if (parmsMap.containsKey("timedSpace")) {
                    space = (int) parmsMap.get("timedSpace");
                } else if (parmsMap.containsKey("space")) {
                    space = (int) parmsMap.get("space");
                }
                int msgType = (int) parmsMap.get("msgType");
                //extras 此处示例没有进行传参，如有需要可以通过js添加extras到msgParams中，进而进行解析去除extra传入到sendNotify中
                //String extras = (String)parmsMap.get("extras");
                sendNotify(msgType, text, space, null, seqId, api, callback, oriCallback);
                return;
            }
        } else if (SEND_LOCAL_NOTIFY.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                String text = (String) parmsMap.get("content");
                String title = (String) parmsMap.get("title");
                int space = 0;
                if (parmsMap.containsKey("space")) {
                    space = (int) parmsMap.get("space");
                }
                MobPushLocalNotification noti = new MobPushLocalNotification();
                noti.setTitle(title);
                noti.setContent(text);
                noti.setNotificationId(new Random().nextInt());
                noti.setTimestamp(space * 60 * 1000 + System.currentTimeMillis());
                addLocalNotification(noti);
                resp.put("content", text);
                resp.put("title", title);
                resp.put("subTitle", parmsMap.get("subTitle"));
                resp.put("sound", parmsMap.get("sound"));
                resp.put("badge", parmsMap.get("badge"));
            }
        } else if (SET_NOTIFY_ICON.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                if (parmsMap != null && parmsMap.containsKey("notifyIcon")) {
                    String notifyIcon = (String) parmsMap.get("notifyIcon");
                    setNotifyIcon(notifyIcon);
                }
            }
        } else if (SET_APP_FOREGROUND_HIDDEN_NOTIFICATION.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                if (parmsMap != null && parmsMap.containsKey("hidden")) {
                    boolean hidden = (boolean) parmsMap.get("hidden");
                    setAppForegroundHiddenNotification(hidden);
                }
            }
        } else if (PRIVACY_PERMISSION_STATUS.equals(api)) {
            if (req.containsKey("agree")) {
                boolean agree = (boolean) req.get("agree");
                submitPrivacyPermissionStatus(agree);
            }
        }

        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = "javascript:" + callback + "(" + hashon.fromHashMap(resp) + ");";
        UIHandler.sendMessage(msg, this);
    }

    private HashMap<String, Object> throwableToMap(Throwable t) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("msg", t.getMessage());
        ArrayList<HashMap<String, Object>> traces = new ArrayList<HashMap<String, Object>>();
        for (StackTraceElement trace : t.getStackTrace()) {
            HashMap<String, Object> element = new HashMap<String, Object>();
            element.put("cls", trace.getClassName());
            element.put("method", trace.getMethodName());
            element.put("file", trace.getFileName());
            element.put("line", trace.getLineNumber());
            traces.add(element);
        }
        map.put("stack", traces);
        Throwable cause = t.getCause();
        if (cause != null) {
            map.put("cause", throwableToMap(cause));
        }
        return map;
    }

    //request fail
    private void onRequestFailed(String seqId, String api, String callback, String oriCallback, Throwable t) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        resp.put("seqId", seqId);
        resp.put("state", 2);
        resp.put("method", api);
        resp.put("callback", oriCallback);
        HashMap<String, Object> error = new HashMap<String, Object>();
        error.put("error_level", 1);
        error.put("error_code", 0);
        error.put("error_msg", t.getMessage());
        error.put("error_detail", throwableToMap(t));
        resp.put("error", error);

        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = "javascript:" + callback + "(" + hashon.fromHashMap(resp) + ");";
        UIHandler.sendMessage(msg, this);
    }

    // ============================ Java Actions ============================

    private void getRegistrationId(String seqId, String api, String callback, String oriCallback) {
        MobPushRegIdCallback idCallback = new MobPushRegIdCallback();
        idCallback.setApi(api);
        idCallback.setSeqId(seqId);
        idCallback.setCallback(this);
        idCallback.setJsCallback(callback);
        idCallback.setOriCallback(oriCallback);
        MobPush.getRegistrationId(idCallback);
    }

    private void addPushReceiver(String seqId, String api, String callback, String oriCallback) {
        System.out.println("addPushReceiver>>>>>>>>>>" + mobPushListener);
        if (mobPushListener == null) {
            mobPushListener = new MobPushListener();
            MobPush.addPushReceiver(mobPushListener);
            mobPushListener.setCallback(this);
        }
    }

    private void setAlias(String alias) {
        MobPush.setAlias(alias);
    }

    private void getAlias() {
        MobPush.getAlias();
    }

    private void deleteAlias() {
        MobPush.deleteAlias();
    }

    private void addTags(String[] tags) {
        MobPush.addTags(tags);
    }

    private void getTags() {
        MobPush.getTags();
    }

    private void deleteTags(String[] tags) {
        MobPush.deleteTags(tags);
    }

    private void cleanTags() {
        MobPush.cleanTags();
    }

    private void addLocalNotification(MobPushLocalNotification var0) {
        MobPush.addLocalNotification(var0);
    }

    /**
     * 模拟推送消息
     *
     * @param type        消息类型：1、通知测试；2、内推测试；3、定时
     * @param text        模拟发送内容，500字节以内，UTF-8
     * @param space       仅对定时消息有效，单位分钟，默认1分钟
     * @param extras      消息附加字段，json字符串格式
     * @param seqId
     * @param api
     * @param callback
     * @param oriCallback
     */
    private void sendNotify(int type, String text, int space, String extras, String seqId, String api, String callback, String oriCallback) {
        MobPushDemoListener mobPushDemoListener = new MobPushDemoListener();
        mobPushDemoListener.setApi(api);
        mobPushDemoListener.setSeqId(seqId);
        mobPushDemoListener.setCallback(this);
        mobPushDemoListener.setJsCallback(callback);
        mobPushDemoListener.setOriCallback(oriCallback);
        //extras json字符串，可以添加一些推送的附加字段，让推送内容更丰富，
        SimulateRequest.sendPush(type, text, space, extras, mobPushDemoListener);
    }


    private void setNotifyIcon(String resIcon) {
        if (!TextUtils.isEmpty(resIcon)) {
            MobPush.setNotifyIcon(ResHelper.getBitmapRes(MobSDK.getContext(), resIcon));
        }
    }

    private void setAppForegroundHiddenNotification(boolean hidden) {
        System.out.println("setAppForegroundHiddenNotification>>>>>>>>>" + hidden);
        MobPush.setAppForegroundHiddenNotification(hidden);
    }

    private void submitPrivacyPermissionStatus(boolean agree) {
        System.out.println("privacyPermissionStatus>>>>>>>>>" + agree);
        MobSDK.submitPolicyGrantResult(agree, new OperationCallback<Void>() {
            @Override
            public void onComplete(Void aVoid) {
                System.out.println("privacyPermissionStatus>>>>>>>>>onComplete");
            }

            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("privacyPermissionStatus>>>>>>>>>onFailure:" + throwable.getMessage());
            }
        });
    }
}
