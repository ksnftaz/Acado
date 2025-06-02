package com.example.aclass;

public class NotificationModel {
    private String message;
    private long timestamp;
    private String classId;

    public NotificationModel() {}

    public NotificationModel(String message, long timestamp, String classId) {
        this.message = message;
        this.timestamp = timestamp;
        this.classId = classId;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getClassId() {
        return classId;
    }

    // Setters
    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
}