package com.example.walkingapp.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.walkingapp.model.ActivityRecord;
import com.example.walkingapp.model.User;
import com.example.walkingapp.model.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardViewModel extends ViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalDistance = new MutableLiveData<>(0.0);
    private final MutableLiveData<Long> totalTime = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> activityCount = new MutableLiveData<>(0L);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration userListener;
    private ListenerRegistration activitiesListener;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public DashboardViewModel() {
        this.userRepository = new UserRepository();
        setupFirestoreListeners();
    }

    public DashboardViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        setupFirestoreListeners();
    }

    private void calculateTotals(String userId) {
        db.collection("users").document(userId).collection("activities")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalDist = 0;
                        long totalTimeSec = 0;
                        int count = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            ActivityRecord record = doc.toObject(ActivityRecord.class);
                            if (record != null) {
                                totalDist += record.getDistance();
                                totalTimeSec += parseTimeToSeconds(record.getElapsedTime());
                                count++;
                            }
                        }

                        Log.d("DashboardVM", "Calculated totals - " +
                                "Distance: " + totalDist + " km, " +
                                "Time: " + totalTimeSec + " sec, " +
                                "Count: " + count);

                        // LiveData'ları güncelle
                        totalDistance.postValue(totalDist);
                        totalTime.postValue(totalTimeSec);
                        activityCount.postValue((long) count);

                        // Firestore'da da güncelle
                        updateUserTotals(userId, totalDist, totalTimeSec, count);
                    } else {
                        Log.e("DashboardVM", "Error calculating totals", task.getException());
                    }
                });
    }


    private void setupFirestoreListeners() {
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            if (userListener != null) {
                userListener.remove();
            }
            if (activitiesListener != null) {
                activitiesListener.remove();
            }
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                Log.d("DashboardVM", "Setting up listeners for user: " + userId);

                userListener = db.collection("users").document(userId)
                        .addSnapshotListener((document, error) -> {
                            if (error != null) {
                                Log.e("DashboardVM", "User listen failed", error);
                                return;
                            }
                            if (document != null && document.exists()) {
                                processUserDocument(document);
                            } else {
                                createDefaultUserDocument(userId);
                            }
                        });
                activitiesListener = db.collection("users").document(userId)
                        .collection("activities")
                        .addSnapshotListener((querySnapshot, error) -> {
                            if (error != null) {
                                Log.e("DashboardVM", "Activities listen failed", error);
                                return;
                            }

                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                calculateTotals(userId);
                            } else {
                                totalDistance.postValue(0.0);
                                totalTime.postValue(0L);
                                activityCount.postValue(0L);
                            }
                        });
            }
        });
    }

    private void processUserDocument(DocumentSnapshot document) {
        try {
            User user = document.toObject(User.class);
            if (user != null) {
                Log.d("DashboardVM", "Updating user data: " + user);
                userLiveData.postValue(user);

                // UI'ı güncellemeden önce toplamları doğrula
                verifyTotals(user.getUserId(),
                        user.getTotalDistance(),
                        user.getTotalTime(),
                        user.getActivityCount());
            }
        } catch (Exception e) {
            Log.e("DashboardVM", "Error processing user document", e);
        }
    }

    private void verifyTotals(String userId, double dbDistance, long dbTime, long dbCount) {
        db.collection("users").document(userId).collection("activities")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double calculatedDistance = 0;
                        long calculatedTime = 0;
                        int calculatedCount = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            ActivityRecord record = doc.toObject(ActivityRecord.class);
                            calculatedDistance += record.getDistance();
                            calculatedTime += parseTimeToSeconds(record.getElapsedTime());
                            calculatedCount++;
                        }

                        // Veritabanındaki değerlerle uyuşmuyorsa güncelle
                        if (Math.abs(dbDistance - calculatedDistance) > 0.1 ||
                                dbTime != calculatedTime ||
                                dbCount != calculatedCount) {

                            Log.w("DashboardVM", "Data mismatch detected, updating totals");
                            updateUserTotals(userId, calculatedDistance, calculatedTime, calculatedCount);
                        } else {
                            // UI'ı güncelle
                            totalDistance.postValue(calculatedDistance);
                            totalTime.postValue(calculatedTime);
                            activityCount.postValue((long)calculatedCount);
                        }
                    }
                });
    }

    private long parseTimeToSeconds(String time) {
        try {
            String[] parts = time.split(":");
            long hours = parts.length > 0 ? Long.parseLong(parts[0]) : 0;
            long minutes = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
            long seconds = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            Log.e("DashboardVM", "Error parsing time: " + time, e);
            return 0;
        }
    }

    private void updateUserTotals(String userId, double distance, long time, long count) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("total_distance", distance);
        updates.put("total_time", time);
        updates.put("activity_count", count);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DashboardVM", "Totals updated successfully");
                    // UI'ı güncelle
                    totalDistance.postValue(distance);
                    totalTime.postValue(time);
                    activityCount.postValue(count);
                })
                .addOnFailureListener(e -> {
                    Log.e("DashboardVM", "Failed to update totals", e);
                    // Sadece UI'ı güncelle (veritabanı yazılamasa bile)
                    totalDistance.postValue(distance);
                    totalTime.postValue(time);
                    activityCount.postValue(count);
                });
    }

    private void createDefaultUserDocument(String userId) {
        Log.d("DashboardVM", "Creating default user document");
        User defaultUser = new User(userId, "", 0.0, 0L, 0L);
        db.collection("users").document(userId)
                .set(defaultUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DashboardVM", "Default user created");
                    userLiveData.postValue(defaultUser);
                })
                .addOnFailureListener(e ->
                        Log.e("DashboardVM", "Error creating default user", e));
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Double> getTotalDistance() {
        return totalDistance;
    }

    public LiveData<Long> getTotalTime() {
        return totalTime;
    }

    public LiveData<Long> getActivityCount() {
        return activityCount;
    }

    public void refreshData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        isLoading.setValue(true); // Yükleniyor

        userRepository.getUserData(currentUser.getUid(), user -> {
            userLiveData.setValue(user);
            isLoading.setValue(false); // Bitti
        });
    }


    public void loadUserData(String userId) {
        Log.d("DashboardVM", "Loading user data for: " + userId);
        userRepository.getUserData(userId, user -> {
            if (user != null) {
                Log.d("DashboardVM", "User data loaded: " + user);
                userLiveData.postValue(user);
                verifyTotals(userId, user.getTotalDistance(), user.getTotalTime(), user.getActivityCount());
            } else {
                Log.d("DashboardVM", "No user data received");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userListener != null) {
            userListener.remove();
        }
        if (activitiesListener != null) {
            activitiesListener.remove();
        }
        Log.d("DashboardVM", "ViewModel cleared");
    }
}