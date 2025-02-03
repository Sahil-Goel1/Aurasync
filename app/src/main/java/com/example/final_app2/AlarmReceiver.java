package com.example.final_app2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");

        // Start the UsageDataService in the foreground to avoid being killed by the system
        Intent serviceIntent = new Intent(context, UsageDataService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

}
