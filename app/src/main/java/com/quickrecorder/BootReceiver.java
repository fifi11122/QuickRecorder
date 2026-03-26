package com.quickrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "开机启动，检查是否需要启动悬浮球");

            SharedPreferences prefs = context.getSharedPreferences("quick_recorder", Context.MODE_PRIVATE);
            boolean floatButtonEnabled = prefs.getBoolean("float_button_enabled", false);

            if (floatButtonEnabled) {
                Intent floatIntent = new Intent(context, FloatButtonService.class);
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(floatIntent);
                    } else {
                        context.startService(floatIntent);
                    }
                    Log.d(TAG, "开机启动悬浮球成功");
                } catch (Exception e) {
                    Log.e(TAG, "开机启动悬浮球失败", e);
                }
            }
        }
    }
}