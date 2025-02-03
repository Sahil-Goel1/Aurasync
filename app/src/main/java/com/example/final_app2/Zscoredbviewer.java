package com.example.final_app2;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Zscoredbviewer extends AppCompatActivity {
    TextView textView;
    DatabaseHelper dbHelper;
    Z_scoreDB zscorer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zscoredb);
        textView = findViewById(R.id.tvUsageStats);
        dbHelper = new DatabaseHelper(this);
        zscorer = new Z_scoreDB(this);
        displayData();
    }

    private void displayData() {
        StringBuilder dataBuilder = new StringBuilder();

        // Display Z-scores data
        displayZscore(dataBuilder);

        // Set the text in the TextView once
        textView.setText(dataBuilder.toString());
    }

    private void displayZscore(StringBuilder dataBuilder) {
        Cursor cursor = zscorer.getAllZscores();

        List<String> sortedDates = new ArrayList<>();
        Map<String, Double> zScoreData = new HashMap<>();  // Changed to store Double instead of Long

        // Loop to retrieve each date and its Z-score
        while (cursor.moveToNext()) {
            @SuppressLint("Range") double zscore = cursor.getDouble(cursor.getColumnIndex(Z_scoreDB.COLUMN_SCORE));  // Changed to getDouble
            @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(Z_scoreDB.COLUMN_DATE));

            zScoreData.put(date, zscore);
            sortedDates.add(date);
        }

        cursor.close();

        // Sort dates in descending order
        sortedDates.sort(Comparator.reverseOrder());

        dataBuilder.append("Z-score Data:\n\n");

        // Build the text view content for each date with its Z-score
        // Format the double to show 2 decimal places
        for (String date : sortedDates) {
            double zScore = zScoreData.get(date);
            dataBuilder.append(date)
                    .append(": Z-score = ")
                    .append(String.format("%.2f", zScore))  // Format to 2 decimal places
                    .append("\n\n");
        }
    }
}