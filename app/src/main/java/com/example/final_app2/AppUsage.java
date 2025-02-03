package com.example.final_app2;

public class AppUsage {
    private String appName;
    private double usageTime; // in minutes

    public AppUsage(String appName, double usageTime) {
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
