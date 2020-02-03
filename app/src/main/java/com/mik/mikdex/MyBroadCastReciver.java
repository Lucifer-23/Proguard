package com.mik.mikdex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadCastReciver extends BroadcastReceiver {
    public static final String TAG = "MyBroadCastReciver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "reciver:" + context);
    }
}