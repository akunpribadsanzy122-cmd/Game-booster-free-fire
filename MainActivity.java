package com.example.gamebooster;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_CODE = 1001;
    private boolean isServiceRunning = false;

    private final Shizuku.OnRequestPermissionResultListener onRequestPermissionResultListener = 
            (requestCode, grantResult) -> {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Izin Shizuku Diterima!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Izin Shizuku Ditolak!", Toast.LENGTH_SHORT).show();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener);
        checkAndRequestShizuku();

        Button btnToggle = findViewById(R.id.btn_toggle_service);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(v -> {
                if (!isServiceRunning) {
                    // LOGIKA PERTAMA: MENYALAKAN (ON)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Izinkan Overlay Terlebih Dahulu!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } else {
                        startOverlayService();
                        btnToggle.setText("STOP FLOATING APPS");
                        btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Warna Hijau
                        isServiceRunning = true;
                    }
                } else {
                    // LOGIKA KEDUA: MEMATIKAN (OFF)
                    stopOverlayService();
                    btnToggle.setText("START FLOATING APPS");
                    btnToggle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0033"))); // Kembali Merah ROG
                    isServiceRunning = false;
                }
            });
        }
    }

    private void checkAndRequestShizuku() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_CODE);
                } else {
                    Toast.makeText(this, "Shizuku Terhubung & Diizinkan!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Shizuku belum berjalan!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startOverlayService() {
        Intent serviceIntent = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Game Booster ROG Aktif!", Toast.LENGTH_SHORT).show();
    }

    private void stopOverlayService() {
        Intent serviceIntent = new Intent(this, FloatingService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Game Booster ROG Dinonaktifkan!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener);
    }
}
