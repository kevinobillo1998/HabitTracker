package com.example.habittracker;

public class Habit {
    private int id;
    private String name;
    private String notes;
    private String createdDate;

    public Habit(int id, String name, String notes, String createdDate) {
        this.id = id;
        this.name = name;
        this.notes = notes;
        this.createdDate = createdDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}
