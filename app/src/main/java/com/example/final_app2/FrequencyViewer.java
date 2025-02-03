package com.example.final_app2;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class FrequencyViewer extends AppCompatActivity {
    private static final String TAG = "FrequencyViewer";
    private TextView tvUsageStats;
    private Handler handler;
    private Runnable updateRunnable;
    private static final long UPDATE_INTERVAL = 5000; // Update every 5 seconds
    private TimeOpenerDB dbHelper;
    private RecyclerView recyclerView;
    private AppUsageAdapter2 appUsageAdapter;
    private List<AppUsage2> appUsageList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.frequencyviewer);
            dbHelper = new TimeOpenerDB(this);

            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            appUsageList = new ArrayList<>();
            appUsageAdapter = new AppUsageAdapter2(appUsageList);
            recyclerView.setAdapter(appUsageAdapter);

            if (!hasUsageStatsPermission()) {
                Log.i(TAG, "Requesting Usage Stats permission");
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            } else {
                Log.i(TAG, "Has Usage Stats permission, starting periodic updates");
                startPeriodicUpdates();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void OpenActivity(View view ){
        Intent intent=new Intent(FrequencyViewer.this,Timeopener.class);
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
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            appUsageList.clear();
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

            UsageEvents usageEvents = usageStatsManager.queryEvents(resetTime, currentTime);
            UsageEvents.Event event = new UsageEvents.Event();
            Map<String, Integer> openCountMap = new HashMap<>();

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                String packageName = event.getPackageName();

                // Count MOVE_TO_FOREGROUND events
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    openCountMap.put(packageName, openCountMap.getOrDefault(packageName, 0) + 1);
                }
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // Convert the map to a list and sort it by the count in descending order
            List<Map.Entry<String, Integer>> sortedOpenCountList = new ArrayList<>(openCountMap.entrySet());
            sortedOpenCountList.sort((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()));

            StringBuilder statsBuilder = new StringBuilder();
            PackageManager packageManager = getPackageManager();
            for (Map.Entry<String, Integer> entry : sortedOpenCountList) {
                String packageName = entry.getKey();

                int timesOpened = entry.getValue();

                if (packageName.equals("com.example.final_app2")) {
                    timesOpened /= 2;  // Halve the timesOpened
                }
                if (isAppInstalled(packageName)) {
                    String appName;
                    try {
                        appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                    }
                    dbHelper.insertOrUpdateAppUsage(appName, timesOpened, currentDate);
                    appUsageList.add(new AppUsage2(appName, timesOpened));                }
            }

            appUsageAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "Error displaying usage stats", e);
            Toast.makeText(this, "Error displaying usage stats: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
}
