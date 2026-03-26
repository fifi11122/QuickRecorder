package com.quickrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordService extends Service {

    private static final String TAG = "RecordService";
    private static final String CHANNEL_ID = "record_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentFilePath;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if ("START_RECORD".equals(action)) {
            startRecording();
        } else if ("STOP_RECORD".equals(action)) {
            stopRecording();
        } else if ("TOGGLE_RECORD".equals(action)) {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            File recordDir = new File(getExternalFilesDir(null), "records");
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "REC_" + sdf.format(new Date()) + ".m4a";
            currentFilePath = new File(recordDir, fileName).getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            showRecordingNotification();

            Log.d(TAG, "开始录音: " + currentFilePath);

        } catch (IOException e) {
            Log.e(TAG, "录音失败", e);
            isRecording = false;
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            isRecording = false;
            stopForeground(true);
            stopSelf();

            Log.d(TAG, "停止录音，文件: " + currentFilePath);

        } catch (Exception e) {
            Log.e(TAG, "停止录音失败", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "录音服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("录音服务正在运行");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showRecordingNotification() {
        Intent stopIntent = new Intent(this, RecordService.class);
        stopIntent.setAction("STOP_RECORD");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在录音")
            .setContentText("点击停止录音")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        super.onDestroy();
    }
}