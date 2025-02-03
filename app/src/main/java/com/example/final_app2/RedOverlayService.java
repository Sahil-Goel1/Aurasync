package com.example.final_app2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RedOverlayService extends Service {

    private static final int NOTIFICATION_ID = 2;
    private WindowManager windowManager;
    private View grayscaleOverlay;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        startForegroundService();
        applyGrayscaleOverlay();
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundService() {
        String channelId = "overlay_service_channel";
        String channelName = "Overlay Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Grayscale Overlay")
                .setContentText("RedScale overlay is active.Close the phone")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void applyGrayscaleOverlay() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        if (grayscaleOverlay == null) {
            grayscaleOverlay = new View(this);
            grayscaleOverlay.setBackgroundColor(Color.argb(240, 128, 128, 128)); // Red overlay color

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
            );

            windowManager.addView(grayscaleOverlay, params);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (grayscaleOverlay != null) {
            windowManager.removeView(grayscaleOverlay);
        }
        handler.removeCallbacksAndMessages(null); // Remove any pending posts to the handler
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
