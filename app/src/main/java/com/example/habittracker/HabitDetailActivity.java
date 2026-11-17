package com.example.habittracker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitDetailActivity extends AppCompatActivity {

    public static final int LOCATION_PERMISSION_REQUEST = 1;

    EditText etName;
    EditText etNotes;
    CheckBox cbComplete;
    TextView tvStreak;
    TextView tvLocation;
    ProgressBar progressBar;
    Button btnSave;
    Button btnDelete;

    DatabaseHelper db;
    LocationManager locationManager;
    int habitId = -1;
    boolean isNewHabit = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // set up the toolbar with a back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DatabaseHelper(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // connect all the variables to the views in the layout
        etName = findViewById(R.id.etName);
        etNotes = findViewById(R.id.etNotes);
        cbComplete = findViewById(R.id.cbComplete);
        tvStreak = findViewById(R.id.tvStreak);
        tvLocation = findViewById(R.id.tvLocation);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // check if we got a habit_id passed in
        // if yes, we're editing an existing habit
        // if no, we're adding a brand new habit
        if (getIntent().hasExtra("habit_id")) {
            habitId = getIntent().getIntExtra("habit_id", -1);
            isNewHabit = false;
            loadHabitData();
        } else {
            tvStreak.setText("Streak: 0 days");
            tvLocation.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            progressBar.setProgress(0);
        }

        // save button - either adds a new habit or updates an existing one
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveHabit();
            }
        });

        // delete button - removes the habit from the database
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteHabit();
            }
        });

        // checkbox - when checked, marks the habit complete and grabs GPS location
        cbComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbComplete.isChecked() && isNewHabit == false) {
                    getLocationAndSaveCompletion();
                }
            }
        });
    }

    // loads the habit's saved data from the database and fills in the screen
    public void loadHabitData() {
        Habit habit = db.getHabit(habitId);
        if (habit == null) {
            Toast.makeText(this, "Habit not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // fill in the name and notes fields
        etName.setText(habit.getName());
        etNotes.setText(habit.getNotes());

        // show the current streak
        int streak = db.getStreak(habitId);
        tvStreak.setText("Streak: " + streak + " days");

        // check if it was already completed today
        // if so, check the box and disable it so it can't be unchecked
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        boolean completedToday = db.isCompletedToday(habitId, today);

        if (completedToday) {
            cbComplete.setChecked(true);
            cbComplete.setEnabled(false);
        } else {
            cbComplete.setChecked(false);
            cbComplete.setEnabled(true);
        }

        // show the last location this habit was completed at, if any
        String lastLocation = db.getLastLocation(habitId);
        if (lastLocation.equals("")) {
            tvLocation.setVisibility(View.GONE);
        } else {
            tvLocation.setText("Near: " + lastLocation);
            tvLocation.setVisibility(View.VISIBLE);
        }

        // calculate the weekly completion percentage and animate the progress bar
        int weekly = db.getWeeklyCompletions(habitId);
        int percent = (weekly * 100) / 7;
        animateProgressBar(percent);

        // show the delete button since this habit already exists
        btnDelete.setVisibility(View.VISIBLE);
    }

    // saves the habit - adds a new one or updates the existing one
    public void saveHabit() {
        String name = etName.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // make sure the name field isn't empty
        if (name.equals("")) {
            etName.setError("Habit name is required.");
            return;
        }

        if (isNewHabit) {
            // adding a brand new habit, save today's date as the created date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());
            boolean success = db.addHabit(name, notes, today);

            if (success) {
                Toast.makeText(this, "Habit added!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving habit.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // updating an existing habit
            boolean success = db.updateHabit(habitId, name, notes);

            if (success) {
                Toast.makeText(this, "Habit updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating habit.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // deletes this habit and closes the screen
    public void deleteHabit() {
        boolean success = db.deleteHabit(habitId);

        if (success) {
            Toast.makeText(this, "Habit deleted.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error deleting habit.", Toast.LENGTH_SHORT).show();
        }
    }

    // gets the device's current GPS location and then saves the completion
    // if permission hasn't been granted yet, asks for it first
    public void getLocationAndSaveCompletion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        try {
            // ask for one location update
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    saveCompletion(lat, lng);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // GPS is off, save with no location instead of crashing
                    saveCompletion(0, 0);
                }
            }, null);
        } catch (Exception e) {
            // something went wrong getting the location, save without it
            Toast.makeText(this, "Could not get location.", Toast.LENGTH_SHORT).show();
            saveCompletion(0, 0);
        }
    }

    // saves a completion record with the given GPS coordinates
    // also converts the coordinates into a readable place name using Geocoder
    public void saveCompletion(double lat, double lng) {
        String locationName = "";

        if (lat != 0 && lng != 0) {
            try {
                // turn the lat/lng into a place name like "Saskatoon"
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);

                    if (address.getLocality() != null) {
                        locationName = address.getLocality();
                    } else if (address.getSubAdminArea() != null) {
                        locationName = address.getSubAdminArea();
                    }
                }
            } catch (Exception e) {
                // geocoding failed, just leave the location name blank
                locationName = "";
            }
        }

        // build a timestamp for right now
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // save the completion record to the database
        boolean success = db.addCompletion(habitId, timestamp, lat, lng, locationName);

        if (success) {
            Toast.makeText(this, "Habit marked complete!", Toast.LENGTH_SHORT).show();

            // update the location text on screen if we got one
            if (locationName.equals("") == false) {
                tvLocation.setText("Near: " + locationName);
                tvLocation.setVisibility(View.VISIBLE);
            }

            // update the progress bar and streak text
            int weekly = db.getWeeklyCompletions(habitId);
            int percent = (weekly * 100) / 7;
            animateProgressBar(percent);

            int streak = db.getStreak(habitId);
            tvStreak.setText("Streak: " + streak + " days");
        } else {
            Toast.makeText(this, "Error saving completion.", Toast.LENGTH_SHORT).show();
            cbComplete.setChecked(false);
        }
    }

    // animates the circular progress bar from 0 up to the target percentage
    public void animateProgressBar(int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress);
        animator.setDuration(1000);
        animator.start();
    }

    // runs after the user responds to the location permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission granted, try getting the location again
                getLocationAndSaveCompletion();
            } else {
                // permission denied, save the completion without a location
                Toast.makeText(this, "Location permission denied. Completion saved without location.", Toast.LENGTH_SHORT).show();
                saveCompletion(0, 0);
            }
        }
    }

    // runs when the back arrow in the toolbar is tapped
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;

    }
}
