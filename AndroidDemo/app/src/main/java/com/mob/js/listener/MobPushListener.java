package com.mob.js.listener;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

import com.mob.js.MobPushUtils;
import com.mob.pushsdk.MobPushCustomMessage;
import com.mob.pushsdk.MobPushNotifyMessage;
import com.mob.pushsdk.MobPushReceiver;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.UIHandler;

import java.util.HashMap;

/**
 * Created by jychen on 2018/2/24.
 */

public class MobPushListener implements MobPushReceiver {
    private Callback callback;
    private String seqId;
    private String jsCallback;
    private String oriCallback;
    private String api;
    private Hashon hashon;
    //自定义action : 0:透传  1:接收通知  2:打开通知  3:Tags  4:Alias

    public MobPushListener() {
        hashon = new Hashon();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }

    public void setJsCallback(String jsCallback) {
        this.jsCallback = jsCallback;
    }

    public void setOriCallback(String oriCallback) {
        this.oriCallback = oriCallback;
    }

    public void setApi(String api) {
        this.api = api;
    }

    @Override
    public void onCustomMessageReceive(Context context, MobPushCustomMessage mobPushCustomMessage) {
        System.out.println("onCustomMessageReceive:" + mobPushCustomMessage.toString());
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (mobPushCustomMessage != null) {
            map.put("result", mobPushCustomMessageToMap(mobPushCustomMessage));
        }
        map.put("seqId", seqId);
        map.put("state", 1);
        map.put("method", api);
        map.put("callback", oriCallback);

        Message msg = new Message();
        msg.what = MobPushUtils.MSG_LOAD_URL;
        msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(map) + ");";
        UIHandler.sendMessage(msg, callback);
    }

    @Override
    public void onNotifyMessageReceive(Context context, MobPushNotifyMessage mobPushNotifyMessage) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (mobPushNotifyMessage != null) {
            map.put("result", mobPushNotifyMessageToMap(mobPushNotifyMessage));
        }
        map.put("seqId", seqId);
        map.put("state", 1);
        map.put("method", api);
        map.put("callback", oriCallback);

        System.out.println("onNotifyMessageReceive:" + map);
        Message msg = new Message();
        msg.what = MobPushUtils.MSG_LOAD_URL;
        msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(map) + ");";
        UIHandler.sendMessage(msg, callback);
    }

    @Override
    public void onNotifyMessageOpenedReceive(Context context, MobPushNotifyMessage mobPushNotifyMessage) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (mobPushNotifyMessage != null) {
            map.put("result", mobPushNotifyMessageToMap(mobPushNotifyMessage));
        }
        map.put("seqId", seqId);
        map.put("state", 1);
        map.put("method", api);
        map.put("callback", oriCallback);

        System.out.println("onNotifyMessageOpenedReceive:" + map);
        Message msg = new Message();
        msg.what = MobPushUtils.MSG_LOAD_URL;
        msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(map) + ");";
        UIHandler.sendMessage(msg, callback);
    }

    @Override
    public void onTagsCallback(Context context, String[] tags, int operation, int errorCode) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (tags != null) {
            map.put("tags", arrayToStr(tags));
        }
        map.put("operation", operation);
        map.put("errorCode", errorCode);

        map.put("seqId", seqId);
        map.put("state", 1);
        map.put("method", api);
        map.put("callback", oriCallback);

        System.out.println("onTagsCallback:" + map);
        Message msg = new Message();
        msg.what = MobPushUtils.MSG_LOAD_URL;
        msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(map) + ");";
        UIHandler.sendMessage(msg, callback);
    }

    @Override
    public void onAliasCallback(Context context, String alias, int operation, int errorCode) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("alias", alias);
        map.put("operation", operation);
        map.put("errorCode", errorCode);

        map.put("seqId", seqId);
        map.put("state", 1);
        map.put("method", api);
        map.put("callback", oriCallback);

        System.out.println("onAliasCallback:" + map);
        Message msg = new Message();
        msg.what = MobPushUtils.MSG_LOAD_URL;
        msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(map) + ");";
        UIHandler.sendMessage(msg, callback);
    }

    private HashMap<String, Object> mobPushCustomMessageToMap(MobPushCustomMessage message) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("messageId", message.getMessageId());
        map.put("content", message.getContent());
        map.put("extrasMap", message.getExtrasMap());
        map.put("timestamp", message.getTimestamp());
        return map;
    }

    private HashMap<String, Object> mobPushNotifyMessageToMap(MobPushNotifyMessage message) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("messageId", message.getMessageId());
        map.put("content", message.getContent());
        map.put("title", message.getTitle());
        map.put("style", message.getStyle());
        map.put("styleContent", message.getStyleContent());
        map.put("extrasMap", message.getExtrasMap());
        map.put("timestamp", message.getTimestamp());
        map.put("inboxStyleContent", message.getInboxStyleContent());
        map.put("channel", message.getChannel());
        return map;
    }

    /**
     * 数组转成逗号分隔的字符串
     *
     * @param array
     * @return
     */
    public String arrayToStr(String[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if ((i + 1) != array.length) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
