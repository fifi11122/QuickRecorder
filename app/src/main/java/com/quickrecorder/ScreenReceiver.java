package com.quickrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "收到广播: " + action);

        switch (action) {
            case Intent.ACTION_SCREEN_OFF:
                // 锁屏时启动悬浮球服务，方便一键录音
                startFloatButtonIfNeeded(context);
                break;
            case Intent.ACTION_SCREEN_ON:
                // 点亮屏幕时可以做一些处理
                break;
            case Intent.ACTION_USER_PRESENT:
                // 解锁成功
                break;
        }
    }

    private void startFloatButtonIfNeeded(Context context) {
        Intent floatIntent = new Intent(context, FloatButtonService.class);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(floatIntent);
            } else {
                context.startService(floatIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "启动悬浮球失败", e);
        }
    }
}