package com.mob.js.listener;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

import com.mob.js.MobPushUtils;
import com.mob.pushsdk.MobPushCallback;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.UIHandler;

import java.util.HashMap;

/**
 * Created by jychen on 2018/5/8.
 */

public class MobPushRegIdCallback implements MobPushCallback<String> {
	private Callback callback;
	private String seqId;
	private String jsCallback;
	private String oriCallback;
	private String api;
    private Hashon hashon;

	public MobPushRegIdCallback(){
	    hashon = new Hashon();
	}

	public void setJsCallback(String jsCallback) {
		this.jsCallback = jsCallback;
	}

	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}

	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public void setOriCallback(String oriCallback) {
		this.oriCallback = oriCallback;
	}

	public void setApi(String api) {
		this.api = api;
	}

	@Override
	public void onCallback(String regId) {
		System.out.println("MobPushRegIdCallback-getRegId:" + regId);
		HashMap<String, Object> resp = new HashMap<String, Object>();
		resp.put("seqId", seqId);
		resp.put("state", 1);
		resp.put("method", api);
		resp.put("registrationID", regId);
		resp.put("callback", oriCallback);

		Message msg = new Message();
		msg.what = MobPushUtils.MSG_LOAD_URL;
		msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(resp) + ");";
		UIHandler.sendMessage(msg, callback);
	}
}
