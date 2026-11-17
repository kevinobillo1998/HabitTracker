package com.example.habittracker;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habittracker.db";
    private static final int DATABASE_VERSION = 1;

    // habits table
    private static final String TABLE_HABITS = "habits";
    private static final String COL_HABIT_ID = "id";
    private static final String COL_HABIT_NAME = "name";
    private static final String COL_HABIT_NOTES = "notes";
    private static final String COL_HABIT_CREATED = "created_date";

    // completions table
    private static final String TABLE_COMPLETIONS = "completions";
    private static final String COL_COMP_ID = "id";
    private static final String COL_COMP_HABIT_ID = "habit_id";
    private static final String COL_COMP_TIMESTAMP = "timestamp";
    private static final String COL_COMP_LAT = "latitude";
    private static final String COL_COMP_LNG = "longitude";
    private static final String COL_COMP_LOCATION = "location_name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHabits = "CREATE TABLE " + TABLE_HABITS + " ("
                + COL_HABIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_HABIT_NAME + " TEXT, "
                + COL_HABIT_NOTES + " TEXT, "
                + COL_HABIT_CREATED + " TEXT)";

        String createCompletions = "CREATE TABLE " + TABLE_COMPLETIONS + " ("
                + COL_COMP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_COMP_HABIT_ID + " INTEGER, "
                + COL_COMP_TIMESTAMP + " TEXT, "
                + COL_COMP_LAT + " REAL, "
                + COL_COMP_LNG + " REAL, "
                + COL_COMP_LOCATION + " TEXT, "
                + "FOREIGN KEY(" + COL_COMP_HABIT_ID + ") REFERENCES " + TABLE_HABITS + "(" + COL_HABIT_ID + "))";

        db.execSQL(createHabits);
        db.execSQL(createCompletions);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPLETIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITS);
        onCreate(db);
    }

    // Add a new habit
    public boolean addHabit(String name, String notes, String createdDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HABIT_NAME, name);
        values.put(COL_HABIT_NOTES, notes);
        values.put(COL_HABIT_CREATED, createdDate);
        long result = db.insert(TABLE_HABITS, null, values);
        return result != -1;
    }

    // Get all habits
    public ArrayList<Habit> getAllHabits() {
        ArrayList<Habit> habits = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HABITS, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String notes = cursor.getString(2);
                String created = cursor.getString(3);
                habits.add(new Habit(id, name, notes, created));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return habits;
    }

    // Get a single habit by id
    public Habit getHabit(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HABITS + " WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            Habit habit = new Habit(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            );
            cursor.close();
            return habit;
        }
        cursor.close();
        return null;
    }

    // Update a habit
    public boolean updateHabit(int id, String name, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HABIT_NAME, name);
        values.put(COL_HABIT_NOTES, notes);
        int rows = db.update(TABLE_HABITS, values, "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Delete a habit and its completions
    public boolean deleteHabit(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COMPLETIONS, COL_COMP_HABIT_ID + " = ?", new String[]{String.valueOf(id)});
        int rows = db.delete(TABLE_HABITS, "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Add a completion record
    public boolean addCompletion(int habitId, String timestamp, double lat, double lng, String locationName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_COMP_HABIT_ID, habitId);
        values.put(COL_COMP_TIMESTAMP, timestamp);
        values.put(COL_COMP_LAT, lat);
        values.put(COL_COMP_LNG, lng);
        values.put(COL_COMP_LOCATION, locationName);
        long result = db.insert(TABLE_COMPLETIONS, null, values);
        return result != -1;
    }

    // Check if habit was completed today
    public boolean isCompletedToday(int habitId, String today) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_COMPLETIONS + " WHERE " + COL_COMP_HABIT_ID + " = ? AND " + COL_COMP_TIMESTAMP + " LIKE ?",
                new String[]{String.valueOf(habitId), today + "%"}
        );
        boolean completed = cursor.getCount() > 0;
        cursor.close();
        return completed;
    }

    // Get streak count for a habit (consecutive days completed)
    public int getStreak(int habitId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT substr(" + COL_COMP_TIMESTAMP + ", 1, 10) FROM " + TABLE_COMPLETIONS
                        + " WHERE " + COL_COMP_HABIT_ID + " = ? ORDER BY " + COL_COMP_TIMESTAMP + " DESC",
                new String[]{String.valueOf(habitId)}
        );

        int streak = 0;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        if (cursor.moveToFirst()) {
            do {
                String dateStr = cursor.getString(0);
                String expectedDate = sdf.format(cal.getTime());
                if (dateStr.equals(expectedDate)) {
                    streak++;
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
                } else {
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return streak;
    }

    // Get weekly completion count (last 7 days)
    public int getWeeklyCompletions(int habitId) {
        SQLiteDatabase db = this.getReadableDatabase();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
        String sevenDaysAgo = sdf.format(cal.getTime());

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT substr(" + COL_COMP_TIMESTAMP + ", 1, 10)) FROM " + TABLE_COMPLETIONS
                        + " WHERE " + COL_COMP_HABIT_ID + " = ? AND substr(" + COL_COMP_TIMESTAMP + ", 1, 10) >= ?",
                new String[]{String.valueOf(habitId), sevenDaysAgo}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Get last completion location for a habit
    public String getLastLocation(int habitId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_COMP_LOCATION + " FROM " + TABLE_COMPLETIONS
                        + " WHERE " + COL_COMP_HABIT_ID + " = ? ORDER BY " + COL_COMP_TIMESTAMP + " DESC LIMIT 1",
                new String[]{String.valueOf(habitId)}
        );
        String location = "";
        if (cursor.moveToFirst()) {
            location = cursor.getString(0);
        }
        cursor.close();
        return location;
    }
}