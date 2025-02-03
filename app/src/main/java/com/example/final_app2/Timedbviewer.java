package com.example.final_app2;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timedbviewer extends AppCompatActivity {
    private LinearLayout cardsContainer;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timedbviewer);

        cardsContainer = findViewById(R.id.cardsContainer);
        dbHelper = new DatabaseHelper(this);

        displayData();
    }

    private void displayData() {
        Map<String, Map<String, Long>> aggregatedData = getAggregatedData();

        // Sort dates in descending order
        List<String> sortedDates = new ArrayList<>(aggregatedData.keySet());
        sortedDates.sort(Comparator.reverseOrder());

        // Create a card for each date
        for (String date : sortedDates) {
            View dateCard = createDateCard(date, aggregatedData.get(date));
            cardsContainer.addView(dateCard);
        }
    }

    private Map<String, Map<String, Long>> getAggregatedData() {
        Map<String, Map<String, Long>> aggregatedData = new HashMap<>();
        Cursor cursor = dbHelper.getAllAppUsage();

        while (cursor.moveToNext()) {
            @SuppressLint("Range") String appName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_APP_NAME));
            @SuppressLint("Range") long usageTime = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_USAGE_TIME));
            @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE));

            aggregatedData
                    .computeIfAbsent(date, k -> new HashMap<>())
                    .merge(appName, usageTime, Long::sum);
        }
        cursor.close();
        return aggregatedData;
    }

    private View createDateCard(String date, Map<String, Long> appUsageData) {
        View cardView = getLayoutInflater().inflate(R.layout.date_card2, cardsContainer, false);

        TextView dateText = cardView.findViewById(R.id.dateText);
        TextView appUsageText = cardView.findViewById(R.id.appUsageText);

        // Set date
        dateText.setText(date);

        // Set app usage data
        StringBuilder usageBuilder = new StringBuilder();
        if (appUsageData != null) {
            List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(appUsageData.entrySet());
            sortedApps.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Long> appEntry : sortedApps) {
                usageBuilder.append(appEntry.getKey())
                        .append(": ")
                        .append(appEntry.getValue())
                        .append(" minutes\n");
            }
        }
        appUsageText.setText(usageBuilder.toString());

        return cardView;
    }
}