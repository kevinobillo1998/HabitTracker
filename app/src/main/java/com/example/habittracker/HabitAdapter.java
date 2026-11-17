package com.example.habittracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private Context context;
    private ArrayList<Habit> habitList;
    private DatabaseHelper db;
    private OnHabitClickListener listener;

    public interface OnHabitClickListener {
        void onHabitClick(Habit habit);
    }

    public HabitAdapter(Context context, ArrayList<Habit> habitList, OnHabitClickListener listener) {
        this.context = context;
        this.habitList = habitList;
        this.listener = listener;
        this.db = new DatabaseHelper(context);
    }

    @Override
    public HabitViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.tvName.setText(habit.getName());

        int streak = db.getStreak(habit.getId());
        holder.tvStreak.setText("Streak: " + streak + " days");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHabitClick(habit);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvStreak;

        public HabitViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHabitName);
            tvStreak = itemView.findViewById(R.id.tvStreak);
        }
    }
}
