package com.example.final_app2;

import static android.content.ContentValues.TAG;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvUsageStats;
    private Handler handler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 3000; // Update every 3 seconds
    private DatabaseHelper dbHelper;
    private AppCategories appcategoriser;
    private Z_scoreDB zscorer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            tvUsageStats = findViewById(R.id.tvUsageStats);
            dbHelper = new DatabaseHelper(this);
            appcategoriser=new AppCategories(this);
            zscorer=new Z_scoreDB(this);
            appcategoriser.insertAppName(AppCategories.TABLE_HARMFUL_APPS,"com.instagram.android" );
            appcategoriser.insertAppName(AppCategories.TABLE_HARMFUL_APPS,"com.facebook.katana" );
            appcategoriser.insertAppName(AppCategories.TABLE_MID_APPS,"org.telegram.messenger" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"org.phonepe.app" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"net.one97.paytm" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"imagetopdf.pdfconverter.jpgtopdf.pdfeditor" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"in.redbus.android" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.google.android.apps.docs" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.google.android.gm" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.google.android.apps.maps" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.google.android.apps.tachyon" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.moodle.moodlemobile" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"com.example.final_app2" );
            appcategoriser.insertAppName(AppCategories.TABLE_USEFUL_APPS,"org.miui.gallery" );
            appcategoriser.insertAppName(AppCategories.TABLE_MID_APPS,"com.linkedin.android" );
            appcategoriser.insertAppName(AppCategories.TABLE_MID_APPS,"com.whatsapp" );
            appcategoriser.insertAppName(AppCategories.TABLE_HARMFUL_APPS,"com.google.android.youtube" );
            appcategoriser.insertAppName(AppCategories.TABLE_MID_APPS,"in.amazon.mShop.android.shopping" );
            appcategoriser.insertAppName(AppCategories.TABLE_MID_APPS,"com.android.chrome" );


            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            }

            if (!hasUsageStatsPermission()) {
                Log.i(TAG, "Requesting Usage Stats permission");
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            } else {
                Log.i(TAG, "Has Usage Stats permission, starting periodic updates");
                startPeriodicUpdates();
            }

            // Schedule both daily and periodic alarms
            schedulePeriodicAlarm();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupBarChart(double totalHarmfulUsageTime,double totalMidUsageTime,double totalUsefulUsageTime) {
        BarChart barChart = findViewById(R.id.barChart);

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float)(totalUsefulUsageTime * 1.84)));
        entries.add(new BarEntry(1, (float)(totalMidUsageTime * 2.9)));
        entries.add(new BarEntry(2, (float)(totalHarmfulUsageTime * 5.9)));

        BarDataSet dataSet = new BarDataSet(entries, "App Usage Scores");
        dataSet.setColors(Color.GREEN, Color.YELLOW, Color.RED);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Customize X-axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Useful", "Mid", "Harmful"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);

        // Customize Y-axis
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);

        // Other customization
        barChart.setDescription(null);
        barChart.getLegend().setEnabled(false);
        barChart.setBackgroundColor(Color.BLACK);
        barChart.animateY(1000);

        barChart.invalidate();
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void schedulePeriodicAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set alarm to trigger every 1 minutes
        long intervalMillis = 1 * 60 * 1000; // 1 minutes
        long startMillis = System.currentTimeMillis();

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startMillis,
                intervalMillis, pendingIntent);
    }

    public void OpenWatchActivity(View view ){
        Intent intent=new Intent(MainActivity.this,DbViewer.class);
        startActivity(intent);
    }

    public void OpenFrequencyActivity(View view ){
        Intent intent=new Intent(MainActivity.this,FrequencyViewer.class);
        startActivity(intent);
    }

    public void OpenZscoreActivity(View view ){
        Intent intent=new Intent(MainActivity.this,Zscoredbviewer.class);
        startActivity(intent);
    }

    private void startPeriodicUpdates() {
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                displayUsageStats();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    private void displayUsageStats() {
        try {
            Map<String, Double> usageMap = getUsageStats();

            List<String> harmfulApps = appcategoriser.getHarmfulApps();
            List<String> usefulApps = appcategoriser.getUsefulApps();
            List<String> midApps = appcategoriser.getMidApps();

            double totalHarmfulUsageTime = 0.0;
            for (String harmfulApp : harmfulApps) {
                Double usageTime = usageMap.get(harmfulApp);
                if (usageTime != null) {
                    totalHarmfulUsageTime += usageTime;
                }
            }

            totalHarmfulUsageTime = totalHarmfulUsageTime / 3600000.0; // Convert to hours

            double totalUsefulUsageTime = 0.0;
            for (String usefulApp : usefulApps) {
                Double usageTime = usageMap.get(usefulApp);
                if (usageTime != null) {
                    totalUsefulUsageTime += usageTime;
                }
            }

            totalUsefulUsageTime = totalUsefulUsageTime / 3600000.0; // Convert to hours

            double totalMidUsageTime = 0.0;
            for (String midApp : midApps) {
                Double usageTime = usageMap.get(midApp);
                if (usageTime != null) {
                    totalMidUsageTime += usageTime;
                }
            }
            totalMidUsageTime = totalMidUsageTime / 3600000.0;// Convert to hours

            setupBarChart(totalHarmfulUsageTime,totalMidUsageTime,totalUsefulUsageTime);

            double Zscore = 0.0;
            Zscore += (totalHarmfulUsageTime * 5.9 + totalUsefulUsageTime * 1.84 + totalMidUsageTime * 2.9);

            if(Zscore<14.75)
            {tvUsageStats.setTextColor(Color.parseColor("#388E3C"));}
            else if(Zscore>=14.75 && Zscore<17.7)
            {tvUsageStats.setTextColor(Color.parseColor("#FFF59D"));}
            else
            {tvUsageStats.setTextColor(Color.parseColor("#D32F2F"));}

            tvUsageStats.setText(String.format(Locale.US, "%.2f", Zscore));
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            zscorer.insertZscore(currentDate, Zscore);

        } catch (Exception e) {
            Log.e(TAG, "Error displaying usage stats", e);
            Toast.makeText(this, "Error displaying usage stats: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Map<String, Double> getUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

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
        long queryStartTime = resetTime - 12 * 60 * 60 * 1000; // Start query 12 hours before reset time

        UsageEvents usageEvents = usageStatsManager.queryEvents(queryStartTime, currentTime);
        UsageEvents.Event event = new UsageEvents.Event();
        Map<String, Double> usageMap = new HashMap<>();
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
                        double duration = (double)(endTime - startTime);
                        usageMap.put(packageName, usageMap.getOrDefault(packageName, 0.0) + duration);
                    }
                    lastStartMap.remove(packageName);
                }
            }
        }
        // Handle apps still in foreground
        for (Map.Entry<String, Long> entry : lastStartMap.entrySet()) {
            String packageName = entry.getKey();
            long startTime = entry.getValue();
            if (startTime < currentTime) {
                double duration = (double)(currentTime - startTime);
                usageMap.put(packageName, usageMap.getOrDefault(packageName, 0.0) + duration);
            }
        }
        return usageMap;
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}