package com.example.final_app2;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timeopener extends AppCompatActivity {
    private LinearLayout cardsContainer;
    private TimeOpenerDB dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freqdbviewer);

        cardsContainer = findViewById(R.id.cardsContainer);
        dbHelper = new TimeOpenerDB(this);

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
            @SuppressLint("Range") String appName = cursor.getString(cursor.getColumnIndex(TimeOpenerDB.COLUMN_APP_NAME));
            @SuppressLint("Range") long timesOpened = cursor.getLong(cursor.getColumnIndex(TimeOpenerDB.COLUMN_TIMES_OPEN));
            @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(TimeOpenerDB.COLUMN_DATE));

            // Aggregate data by date and app name
            aggregatedData
                    .computeIfAbsent(date, k -> new HashMap<>())
                    .merge(appName, timesOpened, Long::sum);
        }
        cursor.close();
        return aggregatedData;
    }

    private View createDateCard(String date, Map<String, Long> appFrequencyData) {
        View cardView = getLayoutInflater().inflate(R.layout.date_card, cardsContainer, false);

        TextView dateText = cardView.findViewById(R.id.dateText);
        TextView appFrequencyText = cardView.findViewById(R.id.appFrequencyText);

        // Set date
        dateText.setText(date);

        // Set app frequency data
        StringBuilder frequencyBuilder = new StringBuilder();
        if (appFrequencyData != null) {
            List<Map.Entry<String, Long>> sortedApps = new ArrayList<>(appFrequencyData.entrySet());
            sortedApps.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Long> appEntry : sortedApps) {
                frequencyBuilder.append(appEntry.getKey())
                        .append(": ")
                        .append(appEntry.getValue())
                        .append(" times\n");
            }
        }
        appFrequencyText.setText(frequencyBuilder.toString());

        return cardView;
    }
}
