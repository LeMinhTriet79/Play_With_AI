package com.minhtriet.model;

public class ApiKey {
    private int id;
    private String name;
    private String value;
    private boolean isActive;

    public ApiKey(int id, String name, String value, boolean isActive) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getValue() { return value; }
    public boolean isActive() { return isActive; }

    @Override
    public String toString() {
        return name + (isActive ? " [Đang dùng]" : "");
    }
}