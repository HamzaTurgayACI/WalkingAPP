package com.example.walkingapp.model;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public interface UserCallback {
        void onUserLoaded(User user);
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public void getUserData(String userId, UserCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Log.d("REPO_DEBUG", "Alınan Veri: " + doc.getData());

                            User user = new User();
                            user.setUserId(doc.getString("user_id"));
                            user.setEmail(doc.getString("email"));

                            // Null kontrolü ile veri çekme
                            Double distance = doc.getDouble("total_distance");
                            Long time = doc.getLong("total_time");
                            Long count = doc.getLong("activity_count");

                            user.setTotalDistance(distance != null ? distance : 0.0);
                            user.setTotalTime(time != null ? time : 0L);
                            user.setActivityCount(count != null ? count : 0L);

                            callback.onUserLoaded(user);
                        } else {
                            Log.w("REPO_DEBUG", "Doküman yok");
                            callback.onUserLoaded(null);
                        }
                    } else {
                        Log.e("REPO_DEBUG", "Firestore hatası", task.getException());
                        callback.onUserLoaded(null);
                    }
                });
    }

    public Task<AuthResult> login(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    public void signInWithGoogle(AuthCredential credential, AuthCallback callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Kullanıcıyı Firestore'a kaydet
                            saveUserToFirestore(user);
                            callback.onSuccess();
                        } else {
                            callback.onError("Kullanıcı bilgileri alınamadı");
                        }
                    } else {
                        callback.onError("Google ile giriş başarısız: " +
                                (task.getException() != null ?
                                        task.getException().getMessage() : "Bilinmeyen hata"));
                    }
                });
    }

    // Kullanıcıyı Firestore'a kaydetme
    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        User user = new User();
        user.setUserId(firebaseUser.getUid());
        user.setEmail(firebaseUser.getEmail());
        user.setTotalDistance(0.0);
        user.setTotalTime(0L);
        user.setActivityCount(0L);

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid ->
                        Log.d("UserRepository", "Kullanıcı Firestore'a kaydedildi"))
                .addOnFailureListener(e ->
                        Log.e("UserRepository", "Kullanıcı kaydedilemedi", e));
    }

    // Kullanıcı oturumunu kontrol etme
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
}