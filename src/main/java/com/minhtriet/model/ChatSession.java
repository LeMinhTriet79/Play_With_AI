package com.minhtriet.model;

public class ChatSession {
    private int id;
    private String title;

    public ChatSession(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public String toString() {
        return title; // Hiển thị tên lên JList
    }
}