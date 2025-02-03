package com.example.final_app2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppCategories extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AppUsageCategories.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USEFUL_APPS = "useful_apps";
    public static final String TABLE_MID_APPS = "mid_apps";
    public static final String TABLE_HARMFUL_APPS = "harmful_apps";

    public static final String COLUMN_APP_NAME = "app_name";

    // SQL statements to create each table
    private static final String CREATE_USEFUL_APPS_TABLE =
            "CREATE TABLE " + TABLE_USEFUL_APPS + " (" +
                    COLUMN_APP_NAME + " TEXT PRIMARY KEY);";

    private static final String CREATE_MID_APPS_TABLE =
            "CREATE TABLE " + TABLE_MID_APPS + " (" +
                    COLUMN_APP_NAME + " TEXT PRIMARY KEY);";

    private static final String CREATE_HARMFUL_APPS_TABLE =
            "CREATE TABLE " + TABLE_HARMFUL_APPS + " (" +
                    COLUMN_APP_NAME + " TEXT PRIMARY KEY);";

    public AppCategories(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USEFUL_APPS_TABLE);
        db.execSQL(CREATE_MID_APPS_TABLE);
        db.execSQL(CREATE_HARMFUL_APPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USEFUL_APPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MID_APPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HARMFUL_APPS);
        onCreate(db);
    }

    // Method to insert an app name into a specified table
    public void insertAppName(String tableName, String appName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_APP_NAME, appName);

        // Insert the app name; ignore if already exists due to PRIMARY KEY constraint
        db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    // Retrieve all app names from a specified table
    public Cursor getAllAppNames(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(tableName, new String[]{COLUMN_APP_NAME}, null, null, null, null, COLUMN_APP_NAME + " ASC");
    }

    @SuppressLint("Range")
    public List<String> getHarmfulApps() {
        List<String> harmfulApps = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HARMFUL_APPS, new String[]{COLUMN_APP_NAME}, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                harmfulApps.add(cursor.getString(cursor.getColumnIndex(COLUMN_APP_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return harmfulApps;
    }

    @SuppressLint("Range")
    public List<String> getUsefulApps() {
        List<String> usefulApps = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USEFUL_APPS, new String[]{COLUMN_APP_NAME}, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                usefulApps.add(cursor.getString(cursor.getColumnIndex(COLUMN_APP_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return usefulApps;
    }

    @SuppressLint("Range")
    public List<String> getMidApps() {
        List<String> midApps = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MID_APPS, new String[]{COLUMN_APP_NAME}, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                midApps.add(cursor.getString(cursor.getColumnIndex(COLUMN_APP_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return midApps;
    }

}
