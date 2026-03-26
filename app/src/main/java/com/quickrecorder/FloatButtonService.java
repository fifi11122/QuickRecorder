package com.quickrecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;

public class FloatButtonService extends Service {

    private static final String TAG = "FloatButtonService";
    private WindowManager windowManager;
    private View floatView;
    private boolean isRecording = false;

    private BroadcastReceiver recordStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RECORD_STATE_CHANGED".equals(intent.getAction())) {
                isRecording = intent.getBooleanExtra("isRecording", false);
                updateButtonState();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createFloatButton();
        registerReceiver(recordStateReceiver, new IntentFilter("RECORD_STATE_CHANGED"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createFloatButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatView = new ImageView(this);
        ((ImageView) floatView).setImageResource(android.drawable.ic_btn_speak_now);
        floatView.setBackground(ContextCompat.getDrawable(this, android.drawable.btn_default));

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        // 点击事件
        floatView.setOnClickListener(v -> toggleRecording());

        // 拖动事件
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // 如果移动距离很小且时间短，认为是点击
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);
                        long duration = System.currentTimeMillis() - touchStartTime;
                        if (deltaX < 10 && deltaY < 10 && duration < 300) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatView, params);
    }

    private void toggleRecording() {
        Intent intent = new Intent(this, RecordService.class);
        intent.setAction("TOGGLE_RECORD");
        startService(intent);
    }

    private void updateButtonState() {
        if (floatView instanceof ImageView) {
            ImageView imageView = (ImageView) floatView;
            if (isRecording) {
                imageView.setImageResource(android.R.drawable.ic_media_pause);
                imageView.setAlpha(0.8f);
            } else {
                imageView.setImageResource(android.R.drawable.ic_btn_speak_now);
                imageView.setAlpha(1.0f);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (floatView != null) {
            windowManager.removeView(floatView);
        }
        unregisterReceiver(recordStateReceiver);
        super.onDestroy();
    }
}