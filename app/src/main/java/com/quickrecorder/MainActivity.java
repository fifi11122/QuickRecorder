package com.quickrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;

    private Switch switchFloatButton;
    private TextView tvStatus;
    private TextView tvRecordList;
    private Button btnStartRecord;
    private Button btnStopRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkPermissions();
        updateRecordList();
    }

    private void initViews() {
        switchFloatButton = findViewById(R.id.switch_float_button);
        tvStatus = findViewById(R.id.tv_status);
        tvRecordList = findViewById(R.id.tv_record_list);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);

        btnStartRecord.setOnClickListener(v -> startRecording());
        btnStopRecord.setOnClickListener(v -> stopRecording());

        switchFloatButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatButton();
                } else {
                    switchFloatButton.setChecked(false);
                    requestOverlayPermission();
                }
            } else {
                stopFloatButton();
            }
        });
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要录音权限才能使用", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                switchFloatButton.setChecked(true);
                startFloatButton();
            } else {
                Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        Intent intent = new Intent(this, RecordService.class);
        intent.setAction("START_RECORD");
        startService(intent);
        tvStatus.setText("状态：录音中...");
    }

    private void stopRecording() {
        Intent intent = new Intent(this, RecordService.class);
        intent.setAction("STOP_RECORD");
        startService(intent);
        tvStatus.setText("状态：已停止");
        updateRecordList();
    }

    private void startFloatButton() {
        Intent intent = new Intent(this, FloatButtonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopFloatButton() {
        stopService(new Intent(this, FloatButtonService.class));
    }

    private void updateRecordList() {
        File recordDir = new File(getExternalFilesDir(null), "records");
        if (!recordDir.exists()) {
            tvRecordList.setText("暂无录音文件");
            return;
        }

        File[] files = recordDir.listFiles();
        if (files == null || files.length == 0) {
            tvRecordList.setText("暂无录音文件");
            return;
        }

        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        int count = Math.min(files.length, 10);
        for (int i = 0; i < count; i++) {
            File file = files[i];
            String date = sdf.format(new Date(file.lastModified()));
            String size = formatFileSize(file.length());
            sb.append("📱 ").append(file.getName()).append("\n")
              .append("   ").append(date).append(" | ").append(size).append("\n\n");
        }

        if (files.length > 10) {
            sb.append("... 还有 ").append(files.length - 10).append(" 个文件");
        }

        tvRecordList.setText(sb.toString());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}