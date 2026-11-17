package com.example.habittracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements HabitAdapter.OnHabitClickListener{

    RecyclerView recyclerView;
    HabitAdapter adapter;
    ArrayList<Habit> habitList;
    DatabaseHelper db;

    // runs when the screen is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // set up the toolbar at the top
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up the database helper
        db = new DatabaseHelper(this);
        habitList = new ArrayList<>();

        // set up the recycler view to show the list of habits
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(this, habitList, this);
        recyclerView.setAdapter(adapter);
    }

    // runs every time the screen comes back into view
    // used to refresh the list in case habits were added/edited/deleted
    @Override
    protected void onResume() {
        super.onResume();

        // clear the old list and load fresh data from the database
        habitList.clear();
        ArrayList<Habit> habitsFromDb = db.getAllHabits();
        for (int i = 0; i < habitsFromDb.size(); i++) {
            habitList.add(habitsFromDb.get(i));
        }
        // tell the adapter the data changed so it redraws the list
        adapter.notifyDataSetChanged();
    }

    // creates the menu in the app bar (the "Add Habit" item)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // runs when a menu item is tapped
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            // open the detail screen with no habit_id, so it knows this is a new habit
            Intent intent = new Intent(this, HabitDetailActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // runs when a habit in the list is tapped
    // opens the detail screen for that specific habit
    @Override
    public void onHabitClick(Habit habit) {
        Intent intent = new Intent(this, HabitDetailActivity.class);
        intent.putExtra("habit_id", habit.getId());
        startActivity(intent);
    }
}