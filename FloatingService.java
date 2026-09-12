package com.example.gamebooster;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;
import java.lang.reflect.Method;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View panelView;
    private View handleView;
    private View crosshairView;
    private boolean isCrosshairActive = false;
    private int layoutFlag;

    // Simpan Resolusi Asli Layar & Status Scaling
    private int nativeWidth = 0;
    private int nativeHeight = 0;
    private boolean isResolutionScaled = false;

    // Pilihan Warna Crosshair
    private final int[] crosshairColors = {Color.RED, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
    private int currentColorIndex = 0;

    // Pilihan Bentuk Crosshair
    private final int[] crosshairShapes = {R.drawable.ic_crosshair_target, R.drawable.ic_crosshair_double};
    private int currentShapeIndex = 0;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Ambil Resolusi Bawaan Fisik Layar HP
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        nativeWidth = metrics.widthPixels;
        nativeHeight = metrics.heightPixels;

        startAsForeground();

        layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        initViews();
        showHandle();
    }

    private void startAsForeground() {
        String channelId = "game_booster_channel";
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Game Booster Overlay Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("ROG Game Genie")
                .setContentText("Overlay aktif di background game")
                .setSmallIcon(R.drawable.ic_rog_logo)
                .build();

        startForeground(1001, notification);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void initViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        handleView = inflater.inflate(R.layout.layout_handle, null);
        panelView = inflater.inflate(R.layout.layout_rog_panel, null);

        handleView.setOnClickListener(v -> {
            hideHandle();
            showPanelOverlay();
        });

        handleView.setOnLongClickListener(v -> {
            stopSelf();
            return true;
        });

        setupListeners(panelView);
    }

    private void showHandle() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        windowManager.addView(handleView, params);
    }

    private void hideHandle() {
        if (handleView != null && handleView.isAttachedToWindow()) {
            windowManager.removeView(handleView);
        }
    }

    private void showPanelOverlay() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dpToPx(320),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        windowManager.addView(panelView, params);
        playOpenAnimation(panelView);
    }

    private void playOpenAnimation(View view) {
        ScaleAnimation scale = new ScaleAnimation(
                0.7f, 1.0f, 0.7f, 1.0f,
                AnimationSet.RELATIVE_TO_SELF, 0.5f,
                AnimationSet.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(180);

        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(180);

        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(scale);
        animSet.addAnimation(alpha);

        view.startAnimation(animSet);
    }

    private void hidePanelOverlay() {
        if (panelView != null && panelView.isAttachedToWindow()) {
            windowManager.removeView(panelView);
        }
    }

    private void setupListeners(View view) {
        Button btnUltraBoost = view.findViewById(R.id.btnUltraBoost);
        Button btnCrosshair = view.findViewById(R.id.btnCrosshair);
        Button btnResolution = view.findViewById(R.id.btnResolution);
        TextView btnClose = view.findViewById(R.id.btnClosePanel);

        // 1. ULTRA BOOST (SAFE LOGIC - TIDAK MEMATIKAN FREE FIRE)
        if (btnUltraBoost != null) {
            btnUltraBoost.setOnClickListener(v -> {
                Toast.makeText(getApplicationContext(), "⚡ Safe RAM Boost Executed!", Toast.LENGTH_SHORT).show();

                // Panggil Garbage Collector internal
                System.gc();

                new Thread(() -> {
                    try {
                        // Script Shell Safe: Trim memory aplikasi pihak ketiga, tapi LEWATI Free Fire & Game Booster
                        String safeCmd = "for pkg in $(pm list packages -3 | cut -d: -f2); do " +
                                "if [ \"$pkg\" != \"com.dts.freefireth\" ] && [ \"$pkg\" != \"com.dts.freefiremax\" ] && [ \"$pkg\" != \"" + getPackageName() + "\" ]; then " +
                                "am trim-memory \"$pkg\" COMPLETE; " +
                                "fi; done";

                        Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                        newProcessMethod.setAccessible(true);
                        Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", safeCmd}, null, null);
                        if (process != null) process.waitFor();
                    } catch (Exception ignored) {}
                }).start();
            });
        }

        // 2. TOGGLE CROSSHAIR (DITAMBAHKAN DI SINI)
        if (btnCrosshair != null) {
            // Set teks awal sesuai kondisi saat ini
            btnCrosshair.setText(isCrosshairActive ? "REMOVE CROSSHAIR" : "ADD CROSSHAIR");

            btnCrosshair.setOnClickListener(v -> {
                boolean enable = !isCrosshairActive;
                toggleCrosshair(enable);

                if (enable) {
                    btnCrosshair.setText("REMOVE CROSSHAIR");
                    Toast.makeText(getApplicationContext(), "🎯 Crosshair Diaktifkan!", Toast.LENGTH_SHORT).show();
                } else {
                    btnCrosshair.setText("ADD CROSSHAIR");
                    Toast.makeText(getApplicationContext(), "❌ Crosshair Dinonaktifkan!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 3. RESOLUSI 1.5X
        if (btnResolution != null) {
            btnResolution.setOnClickListener(v -> {
                new Thread(() -> {
                    try {
                        String command;
                        if (!isResolutionScaled) {
                            int targetW = Math.round(nativeWidth * 1.5f);
                            int targetH = Math.round(nativeHeight * 1.5f);
                            command = "wm size " + targetW + "x" + targetH;
                        } else {
                            command = "wm size reset";
                        }

                        Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                        newProcessMethod.setAccessible(true);
                        Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", command}, null, null);
                        if (process != null) process.waitFor();

                        isResolutionScaled = !isResolutionScaled;

                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (isResolutionScaled) {
                                btnResolution.setText("🖥️ RES 1.5X (ON)");
                                Toast.makeText(getApplicationContext(), "Resolusi diubah ke 1.5x!", Toast.LENGTH_SHORT).show();
                            } else {
                                btnResolution.setText("🖥️ RESOLUTION 1.5X");
                                Toast.makeText(getApplicationContext(), "Resolusi Kembali Normal!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(getApplicationContext(), "Gagal Mengubah Resolusi via Shizuku!", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            });
        }

        // 4. CLOSE PANEL
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                hidePanelOverlay();
                showHandle();
            });
        }
    }

    private void toggleCrosshair(boolean enable) {
        if (enable) {
            if (crosshairView != null && crosshairView.isAttachedToWindow()) {
                return; // Mencegah duplikasi overlay jika sudah ada
            }

            crosshairView = LayoutInflater.from(this).inflate(R.layout.overlay_crosshair, null);

            final WindowManager.LayoutParams crosshairParams = new WindowManager.LayoutParams(
                    dpToPx(56),
                    dpToPx(56),
                    layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            crosshairParams.gravity = Gravity.CENTER;

            updateCrosshairStyle();

            crosshairView.setOnTouchListener(new View.OnTouchListener() {
                private static final int CLICK_ACTION_THRESHOLD = 200;
                private long lastDownTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastDownTime = System.currentTimeMillis();
                            initialX = crosshairParams.x;
                            initialY = crosshairParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            crosshairParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                            crosshairParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(crosshairView, crosshairParams);
                            return true;

                        case MotionEvent.ACTION_UP:
                            long duration = System.currentTimeMillis() - lastDownTime;
                            if (duration < CLICK_ACTION_THRESHOLD) {
                                // Klik Cepat = Ganti Warna
                                currentColorIndex = (currentColorIndex + 1) % crosshairColors.length;
                                updateCrosshairStyle();
                                Toast.makeText(getApplicationContext(), "🎨 Warna Diganti", Toast.LENGTH_SHORT).show();
                            } else if (duration >= 600) {
                                // Tahan/Long Press = Ganti Bentuk Crosshair
                                currentShapeIndex = (currentShapeIndex + 1) % crosshairShapes.length;
                                updateCrosshairStyle();
                                Toast.makeText(getApplicationContext(), "🔄 Bentuk Crosshair Diganti", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                    }
                    return false;
                }
            });

            windowManager.addView(crosshairView, crosshairParams);
            isCrosshairActive = true;
        } else {
            if (crosshairView != null && crosshairView.isAttachedToWindow()) {
                windowManager.removeView(crosshairView);
            }
            isCrosshairActive = false;
        }
    }

    private void updateCrosshairStyle() {
        if (crosshairView != null) {
            ImageView imgCrosshair = crosshairView.findViewById(R.id.imgCrosshair);
            if (imgCrosshair != null) {
                imgCrosshair.setImageResource(crosshairShapes[currentShapeIndex]);
                imgCrosshair.setColorFilter(crosshairColors[currentColorIndex]);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isResolutionScaled) {
            new Thread(() -> {
                try {
                    Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                    newProcessMethod.setAccessible(true);
                    Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", "wm size reset"}, null, null);
                    if (process != null) process.waitFor();
                } catch (Exception ignored) {}
            }).start();
        }

        hideHandle();
        hidePanelOverlay();
        if (crosshairView != null && crosshairView.isAttachedToWindow()) {
            windowManager.removeView(crosshairView);
        }
    }
}
