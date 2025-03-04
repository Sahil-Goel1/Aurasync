package com.example.final_app2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TimeOpenerDB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "OpenFrequency.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "app_frequency";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_APP_NAME = "app_name";
    public static final String COLUMN_TIMES_OPEN = "times_open";
    public static final String COLUMN_DATE = "date";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_APP_NAME + " TEXT, " +
                    COLUMN_TIMES_OPEN + " INTEGER, " +
                    COLUMN_DATE + " TEXT, " +
                    "UNIQUE(" + COLUMN_APP_NAME + ", " + COLUMN_DATE + "));";

    public TimeOpenerDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertOrUpdateAppUsage(String appName, long usageTime, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_APP_NAME, appName);
        values.put(COLUMN_TIMES_OPEN, usageTime);
        values.put(COLUMN_DATE, date);

        // Try to insert, if it fails due to unique constraint, update instead
        int rows = db.update(TABLE_NAME, values,
                COLUMN_APP_NAME + "=? AND " + COLUMN_DATE + "=?",
                new String[]{appName, date});
        if (rows == 0) {
            // No existing record, insert a new one
            db.insert(TABLE_NAME, null, values);
        }
        db.close();
    }

    public Cursor getAllAppUsage() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, COLUMN_DATE + " DESC, " + COLUMN_TIMES_OPEN + " DESC");
    }
}
