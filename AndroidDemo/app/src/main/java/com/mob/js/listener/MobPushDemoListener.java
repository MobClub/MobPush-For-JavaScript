package com.mob.js.listener;

import android.os.Handler;
import android.os.Message;

import com.mob.js.MobPushUtils;
import com.mob.pushsdk.MobPushCallback;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.UIHandler;

import java.util.HashMap;

/**
 * Created by jychen on 2018/5/4.
 */

public class MobPushDemoListener implements MobPushCallback<Boolean> {
	private Handler.Callback callback;
	private String seqId;
	private String jsCallback;
	private String oriCallback;
	private String api;
	private Hashon hashon;

	public MobPushDemoListener(){
		hashon = new Hashon();
	}

	public void setJsCallback(String jsCallback) {
		this.jsCallback = jsCallback;
	}

	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}

	public void setCallback(Handler.Callback callback) {
		this.callback = callback;
	}

	public void setOriCallback(String oriCallback) {
		this.oriCallback = oriCallback;
	}

	public void setApi(String api) {
		this.api = api;
	}

	@Override
	public void onCallback(Boolean b) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		if(b){
			result.put("action", 1);
		} else{
			result.put("action", 0);
		}

		result.put("seqId", seqId);
		result.put("state", 1);
		result.put("method", api);
		result.put("callback", oriCallback);

		System.out.println("MobPushDemoListener-Action:" + result);
		Message msg = new Message();
		msg.what = MobPushUtils.MSG_LOAD_URL;
		msg.obj = "javascript:" + jsCallback + "(" + hashon.fromHashMap(result) + ");";
		UIHandler.sendMessage(msg, callback);
	}
}
