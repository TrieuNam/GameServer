package com.SouthMillion.activity_service.entity;

public class DailyGift {
    private int id;
    private String name;
    private int days;

    public DailyGift(int id, String name, int days) {
        this.id = id;
        this.name = name;
        this.days = days;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public void giveGift(Object player) {
        // Placeholder until Player model is introduced.
    }
}