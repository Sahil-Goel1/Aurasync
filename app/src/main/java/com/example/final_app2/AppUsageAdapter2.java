package com.example.final_app2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppUsageAdapter2 extends RecyclerView.Adapter<AppUsageAdapter2.ViewHolder> {
    private List<AppUsage2> appUsageList;

    public AppUsageAdapter2(List<AppUsage2> appUsageList) {
        this.appUsageList = appUsageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the current app usage item
        AppUsage2 appUsage = appUsageList.get(position);

        // Bind the data to the views
        holder.appNameTextView.setText(appUsage.getAppName());
        holder.usageTimeTextView.setText(String.format("%.1f times", appUsage.getUsageTime()));
    }

    @Override
    public int getItemCount() {
        return appUsageList.size(); // Return the size of the list
    }

    // ViewHolder class to hold the views for each item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameTextView;
        TextView usageTimeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(R.id.appName); // Reference to app name TextView
            usageTimeTextView = itemView.findViewById(R.id.timesOpened); // Reference to usage time TextView
        }
    }
}
