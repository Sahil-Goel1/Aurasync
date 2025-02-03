package com.example.final_app2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Z_scoreDB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Z_score.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_Z_SCORE = "z_score";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_DATE = "date";
    public static final String TABLE_WEEKLY_Z_SCORE = "weekly_z_score";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_WEEKLY_SCORE = "weekly_score";

    // Modified SQL statements to use REAL instead of LONG for storing double values
    private static final String CREATE_TABLE_Z_SCORE =
            "CREATE TABLE " + TABLE_Z_SCORE + " (" +
                    COLUMN_DATE + " TEXT PRIMARY KEY, " +
                    COLUMN_SCORE + " REAL);";

    private static final String CREATE_TABLE_WEEKLY_Z_SCORE =
            "CREATE TABLE " + TABLE_WEEKLY_Z_SCORE + " (" +
                    COLUMN_START_DATE + " TEXT, " +
                    COLUMN_END_DATE + " TEXT, " +
                    COLUMN_WEEKLY_SCORE + " REAL);";

    public Z_scoreDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_Z_SCORE);
        db.execSQL(CREATE_TABLE_WEEKLY_Z_SCORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_Z_SCORE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEEKLY_Z_SCORE);
        onCreate(db);
    }

    // Modified method to accept double instead of long
    public void insertZscore(String date, double zScore) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_SCORE, zScore);

        // Check if an entry for the given date already exists
        int rowsAffected = db.update(TABLE_Z_SCORE, values, COLUMN_DATE + " = ?", new String[]{date});

        // If no rows were updated, insert a new row
        if (rowsAffected == 0) {
            db.insert(TABLE_Z_SCORE, null, values);
        }
        checkAndUpdateWeeklyScore(date);
        db.close();
    }

    private void checkAndUpdateWeeklyScore(String currentDate) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_WEEKLY_Z_SCORE, new String[]{COLUMN_START_DATE, COLUMN_END_DATE},
                null, null, null, null, null);

        String startDate, endDate;

        if (cursor.moveToFirst()) {
            startDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE));
            endDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_DATE));
        } else {
            startDate = currentDate;
            endDate = calculateEndDate(startDate);

            ContentValues values = new ContentValues();
            values.put(COLUMN_START_DATE, startDate);
            values.put(COLUMN_END_DATE, endDate);
            values.put(COLUMN_WEEKLY_SCORE, 0.0); // Changed to 0.0 for consistency
            db.insert(TABLE_WEEKLY_Z_SCORE, null, values);
        }
        cursor.close();

        if (currentDate.compareTo(endDate) >= 0) {
            // Calculate score for the completed week
            double weeklyScore = calculateWeeklyScore(db, startDate, endDate);

            // Prepare new dates for the next week
            String newStartDate = endDate;
            String newEndDate = calculateEndDate(newStartDate);

            // Update everything in a single operation
            ContentValues values = new ContentValues();
            values.put(COLUMN_START_DATE, newStartDate);
            values.put(COLUMN_END_DATE, newEndDate);
            values.put(COLUMN_WEEKLY_SCORE, weeklyScore);

            // Update the single row in the database
            // Note: We can use "1" as the whereClause since we maintain only one row
            db.update(TABLE_WEEKLY_Z_SCORE, values, "1", null);

        }

        db.close();
    }

    private double calculateWeeklyScore(SQLiteDatabase db, String startDate, String endDate) {
        Cursor cursor = db.query(TABLE_Z_SCORE,
                new String[]{"SUM(" + COLUMN_SCORE + ") AS total"},
                COLUMN_DATE + " BETWEEN ? AND ?",
                new String[]{startDate, endDate},
                null, null, null);

        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        return total;
    }

    private String calculateEndDate(String startDate) {
        // Assuming date format is "yyyy-MM-dd"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(sdf.parse(startDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        calendar.add(Calendar.DAY_OF_YEAR, 7);
        return sdf.format(calendar.getTime());
    }

    // Retrieve all z-scores, ordered by date
    public Cursor getAllZscores() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_Z_SCORE, new String[]{COLUMN_DATE, COLUMN_SCORE},
                null, null, null, null, COLUMN_DATE + " ASC");
    }

    public double getWeeklyScore() {
        SQLiteDatabase db = this.getReadableDatabase();
        double weeklyScore = 0.0;

        Cursor cursor = db.query(
                TABLE_WEEKLY_Z_SCORE,
                new String[]{COLUMN_WEEKLY_SCORE},
                null,  // No WHERE clause
                null,  // No WHERE clause arguments
                null,  // No GROUP BY
                null,  // No HAVING
                COLUMN_START_DATE + " DESC",  // ORDER BY start_date DESC
                "1"   // LIMIT 1 to get the most recent entry
        );

        if (cursor != null && cursor.moveToFirst()) {
            weeklyScore = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEEKLY_SCORE));
            cursor.close();
        }

        db.close();
        return weeklyScore;
    }
}