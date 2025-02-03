package com.example.final_app2;

public class AppUsage2 {
    private String appName;
    private int usageTime; // in minutes

    public AppUsage2(String appName, int usageTime) {
        this.appName = appName;
        this.usageTime = usageTime;
    }

    public String getAppName() {
        return appName;
    }

    public double getUsageTime() {
        return usageTime;
    }
}
