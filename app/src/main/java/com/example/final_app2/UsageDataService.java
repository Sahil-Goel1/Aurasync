package com.example.final_app2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsageDataService extends Service {
    private static final String TAG = "UsageDataService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AppUsageService";
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::checkUsageStats).start();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Usage Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Usage Tracking")
                .setContentText("you are in green zone now")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }

    private Notification createWarningNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Usage Warning")
                .setContentText("You are in yellow zone now.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private Notification createDangerNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Usage Alert")
                .setContentText("You are in the red zone now.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
    }

    private String getCurrentForegroundApp() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000 * 60 * 60 *6; // Check for events in the past 6 hours

        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
        String lastForegroundPackage = null;
        UsageEvents.Event event = new UsageEvents.Event();

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.getPackageName();
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND
                    && lastForegroundPackage != null && lastForegroundPackage.equals(event.getPackageName())) {
                // Reset lastForegroundPackage if app moves to background
                lastForegroundPackage = null;
            }
        }

        return lastForegroundPackage; // Returns package name if an app is in the foreground without moving to background
    }

    @SuppressLint("ForegroundServiceType")
    private void checkUsageStats() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);

            AppCategories dbHelper = new AppCategories(this);
            List<String> harmfulApps = dbHelper.getHarmfulApps();
            List<String> usefulApps = dbHelper.getUsefulApps();
            List<String> midApps = dbHelper.getMidApps();
            DatabaseHelper dbHelper2 = new DatabaseHelper(this);
            Z_scoreDB zscorer = new Z_scoreDB(this);
            PackageManager packageManager = getPackageManager();

            Calendar currentCalendar = Calendar.getInstance();
            Calendar resetCalendar = (Calendar) currentCalendar.clone();
            resetCalendar.set(Calendar.HOUR_OF_DAY, 0);
            resetCalendar.set(Calendar.MINUTE, 0);
            resetCalendar.set(Calendar.SECOND, 0);
            resetCalendar.set(Calendar.MILLISECOND, 0);

            if (currentCalendar.before(resetCalendar)) {
                resetCalendar.add(Calendar.DAY_OF_YEAR, -1);
            }

            long resetTime = resetCalendar.getTimeInMillis();
            long currentTime = currentCalendar.getTimeInMillis();
            long queryStartTime = resetTime - 12 * 60 * 60 * 1000;

            UsageEvents usageEvents = usageStatsManager.queryEvents(queryStartTime, currentTime);
            UsageEvents.Event event = new UsageEvents.Event();
            Map<String, Long> usageMap = new HashMap<>();
            Map<String, Long> lastStartMap = new HashMap<>();

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                String packageName = event.getPackageName();
                long eventTime = event.getTimeStamp();

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (eventTime < resetTime) {
                        lastStartMap.put(packageName, resetTime);
                    } else {
                        lastStartMap.put(packageName, eventTime);
                    }
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    Long startTime = lastStartMap.get(packageName);
                    if (startTime != null) {
                        long endTime = Math.min(eventTime, currentTime);
                        if (endTime > resetTime && startTime < resetTime) {
                            startTime = resetTime;
                        }
                        if (endTime > startTime) {
                            long duration = endTime - startTime;
                            usageMap.put(packageName, usageMap.getOrDefault(packageName, 0L) + duration);
                        }
                        lastStartMap.remove(packageName);
                    }
                }
            }

            for (Map.Entry<String, Long> entry : lastStartMap.entrySet()) {
                String packageName = entry.getKey();
                long startTime = entry.getValue();
                if (startTime < currentTime) {
                    long duration = currentTime - startTime;
                    usageMap.put(packageName, usageMap.getOrDefault(packageName, 0L) + duration);
                }
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            for (Map.Entry<String, Long> entry : usageMap.entrySet()) {
                String packageName = entry.getKey();
                if (isAppInstalled(packageName)) {
                    String appName;
                    try {
                        appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                    }
                    long seconds = entry.getValue() / 60000;
                    dbHelper2.insertOrUpdateAppUsage(appName, seconds, currentDate);
                }
            }

            double totalHarmfulUsageTime = 0.0;
            for (String harmfulApp : harmfulApps) {
                Long usageTime = usageMap.get(harmfulApp);
                if (usageTime != null) {
                    totalHarmfulUsageTime += (double) usageTime;
                }
            }

            totalHarmfulUsageTime = totalHarmfulUsageTime / 3600000.0;

            double totalUsefulUsageTime = 0.0;
            for (String usefulApp : usefulApps) {
                Long usageTime = usageMap.get(usefulApp);
                if (usageTime != null) {
                    totalUsefulUsageTime += (double) usageTime;
                }
            }

            totalUsefulUsageTime = totalUsefulUsageTime / 3600000.0;

            double totalMidUsageTime = 0.0;
            for (String midApp : midApps) {
                Long usageTime = usageMap.get(midApp);
                if (usageTime != null) {
                    totalMidUsageTime += (double) usageTime;
                }
            }
            totalMidUsageTime = totalMidUsageTime / 3600000.0;

            double zScore = 0.0;
            zScore += (totalHarmfulUsageTime * 5.9 + totalUsefulUsageTime * 1.84 + totalMidUsageTime * 2.9);

            zscorer.insertZscore(currentDate, zScore);

            Intent greenintent = new Intent(this, OverlayService.class);
            Intent redintent = new Intent(this, RedOverlayService.class);

            if (zScore < 40) {
                Notification notification = createNotification();
                startForeground(NOTIFICATION_ID, notification);
                stopService(greenintent);
                stopService(redintent);

            } else if ( 40<=zScore && zScore < 60) {
                Notification warningNotification = createWarningNotification();
                startForeground(NOTIFICATION_ID, warningNotification);
                String currentApp=getCurrentForegroundApp();
                if (midApps.contains(currentApp) || harmfulApps.contains(currentApp)){
                    startService(greenintent);}
                else{
                    stopService(greenintent);
                }
            } else {
                Notification dangerNotification = createDangerNotification();
                startForeground(NOTIFICATION_ID, dangerNotification);
                stopService(greenintent);
                String currentApp = getCurrentForegroundApp();
                if (midApps.contains(currentApp) || harmfulApps.contains(currentApp)) {
                    startService(redintent);
                } else {
                    stopService(redintent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats", e);
        }
    }
    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

