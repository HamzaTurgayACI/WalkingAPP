package com.example.walkingapp.viewmodel;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.walkingapp.model.ActivityRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.List;

public class PastActivitiesViewModel extends ViewModel {

    private final MutableLiveData<List<ActivityRecord>> activityRecords = new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public PastActivitiesViewModel() {
        fetchPastActivities();
    }

    public LiveData<List<ActivityRecord>> getActivityRecords() {
        return activityRecords;
    }

    public void fetchPastActivities() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("Firestore", "Kullanıcı giriş yapmamış!");
            return;
        }
        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // kullanıcının aktivitelerini Firestore’dan çek
        db.collection("users")
                .document(userId)
                .collection("activities")
                .orderBy("timestamp", Query.Direction.DESCENDING) //  En son eklenen aktiviteleri önce göster
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ActivityRecord> records = new ArrayList<>();
                        QuerySnapshot result = task.getResult();

                        if (result != null) {
                            for (QueryDocumentSnapshot document : result) {
                                ActivityRecord record = document.toObject(ActivityRecord.class);
                                if (record.getRoutePoints() == null) {
                                    record.setRoutePoints(new ArrayList<>());
                                }
                                records.add(record);
                            }
                        }
                        activityRecords.setValue(records);
                    } else {
                        Log.w("FirestoreError", "Veri çekme hatası", task.getException());
                    }
                });
    }
}