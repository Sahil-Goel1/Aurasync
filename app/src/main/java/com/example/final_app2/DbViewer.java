package com.example.final_app2;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DbViewer extends AppCompatActivity {
    private static final String TAG = "DbViewer";
    private TextView tvUsageStats;
    private Handler handler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 3000; // Update every 3 seconds
    private DatabaseHelper dbHelper;
    private AppCategories appcategoriser;
    private Z_scoreDB zscorer;
    private RecyclerView recyclerView;
    private AppUsageAdapter appUsageAdapter;
    private List<AppUsage> appUsageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.timeviewer);

            dbHelper = new DatabaseHelper(this);
            appcategoriser=new AppCategories(this);
            zscorer=new Z_scoreDB(this);

            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            appUsageList = new ArrayList<>();
            appUsageAdapter = new AppUsageAdapter(appUsageList);
            recyclerView.setAdapter(appUsageAdapter);
            startPeriodicUpdates();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void OpenActivity(View view ){
        Intent intent=new Intent(DbViewer.this,Timedbviewer.class);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null && updateRunnable != null) {
            handler.post(updateRunnable);
        }
    }

    private void displayUsageStats() {
        try {
            Map<String, Double> usageMap = getUsageStats();
            StringBuilder statsBuilder = new StringBuilder();
            PackageManager packageManager = getPackageManager();
            appUsageList.clear();

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
            totalMidUsageTime = totalMidUsageTime / 3600000.0; // Convert to hours

            double Zscore = 0.0;
            Zscore += (totalHarmfulUsageTime * 5.9 + totalUsefulUsageTime * 1.45 + totalMidUsageTime * 2.9);

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            zscorer.insertZscore(currentDate, Zscore);

            List<Map.Entry<String, Double>> sortedUsageList = new ArrayList<>(usageMap.entrySet());
            sortedUsageList.sort((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()));

            for (Map.Entry<String, Double> entry : sortedUsageList) {
                String packageName = entry.getKey();
                if (isAppInstalled(packageName)) {
                    String appName;
                    try {
                        appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                    }
                    double minutes = entry.getValue() / 60000.0; // Convert milliseconds to minutes
                    appUsageList.add(new AppUsage(appName, minutes));
                    // Update or insert in database
                    dbHelper.insertOrUpdateAppUsage(appName, (long)minutes, currentDate);
                }
            }

            appUsageAdapter.notifyDataSetChanged();
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
