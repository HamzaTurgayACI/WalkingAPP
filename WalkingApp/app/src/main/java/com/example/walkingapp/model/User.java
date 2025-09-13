package com.example.walkingapp.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

@IgnoreExtraProperties
public class User implements Serializable {
    private String userId;
    private String email;
    private double totalDistance;
    private long totalTime;
    private long activityCount;

    public User() {}

    // Constructor with all fields
    public User(String userId, String email, double totalDistance,
                long totalTime, long activityCount) {
        this.userId = userId;
        this.email = email;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.activityCount = activityCount;
    }

    // Firestore field mapping for "user_id"
    @PropertyName("user_id")
    public String getUserId() {
        return userId;
    }

    @PropertyName("user_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Firestore field mapping for "email"
    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    // Firestore field mapping for "total_distance"
    @PropertyName("total_distance")
    public double getTotalDistance() {
        return totalDistance;
    }

    @PropertyName("total_distance")
    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    // Firestore field mapping for "total_time"
    @PropertyName("total_time")
    public long getTotalTime() {
        return totalTime;
    }

    @PropertyName("total_time")
    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    // Firestore field mapping for "activity_count"
    @PropertyName("activity_count")
    public long getActivityCount() {
        return activityCount;
    }

    @PropertyName("activity_count")
    public void setActivityCount(long activityCount) {
        this.activityCount = activityCount;
    }

    // Optional: Add toString method for better readability in logs
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", totalDistance=" + totalDistance +
                ", totalTime=" + totalTime +
                ", activityCount=" + activityCount +
                '}';
    }
}
