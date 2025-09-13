package com.example.walkingapp.viewmodel;

import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.walkingapp.model.ActivityRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NewActivityViewModel extends AndroidViewModel {

    private final MutableLiveData<String> elapsedTime = new MutableLiveData<>("00:00");
    private final MutableLiveData<Double> distance = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> calories = new MutableLiveData<>(0);
    private final MutableLiveData<List<GeoPoint>> routePoints = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> weatherInfo = new MutableLiveData<>("Yükleniyor...");
    private final MutableLiveData<GeoPoint> centerLocation = new MutableLiveData<>();

    private Timer timer;
    private long seconds = 0;
    private double totalDistance = 0.0;
    private Location lastLocation = null;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public LiveData<String> getElapsedTime() { return elapsedTime; }
    public LiveData<Double> getDistance() { return distance; }
    public LiveData<Integer> getCalories() { return calories; }
    public LiveData<List<GeoPoint>> getRoutePoints() { return routePoints; }
    public LiveData<String> getWeatherInfo() { return weatherInfo; }
    public LiveData<GeoPoint> getCenterLocation() { return centerLocation; }

    public NewActivityViewModel(@NonNull Application application) {
        super(application);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application.getApplicationContext());
    }

    @SuppressLint("MissingPermission")
    public void startActivity() {
        startTimer();
        startLocationUpdates();
        getCurrentLocation(); // Başlangıçta konumu ortala
    }

    public void stopActivity() {
        stopTimer();
        stopLocationUpdates();
    }

    private void startTimer() {
        if (timer != null) return;
        seconds = 0;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seconds++;
                int minutes = (int) (seconds / 60);
                int sec = (int) (seconds % 60);
                elapsedTime.postValue(String.format("%02d:%02d", minutes, sec));
                calories.postValue((int) (seconds / 5));
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        updateLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }


    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        saveActivityToFirestore(); // ViewModel içinde çağırırken doğrudan çağır
    }


    public void updateLocation(double latitude, double longitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        updateRoute(location);

    }


    private void updateRoute(Location location) {
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        List<GeoPoint> currentRoute = new ArrayList<>(routePoints.getValue());
        currentRoute.add(point);
        routePoints.postValue(currentRoute);

        if (lastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results
            );
            if (results[0] > 0.5) {
                totalDistance += results[0];
                distance.postValue(totalDistance / 1000);
            }
        }
        lastLocation = location;
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        centerLocation.postValue(currentPoint); // Harita konumunu ortala
                    } else {
                        Log.w("Location", "Konum alınamadı.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Location", "Konum hatası: " + e.getMessage()));
    }
    public void fetchWeather(String apiKey) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=38.355&lon=38.297&units=metric&appid=" + apiKey;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                weatherInfo.postValue("Hava durumu alınamadı.");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
                        double temp = json.getJSONObject("main").getDouble("temp");
                        weatherInfo.postValue(String.format("%s, %.1f°C", description, temp));
                    } catch (Exception e) {
                        weatherInfo.postValue("Veri çözümleme hatası.");
                    }
                } else {
                    weatherInfo.postValue("Hava durumu alınamadı.");
                }
            }
        });
    }

    private boolean isActivitySaved = false; // Daha önce kaydedildi mi?

    public void saveActivityToFirestore() {
        if (isActivitySaved) {
            Log.d("Firestore", "Bu aktivite zaten kaydedildi!");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.e("Firestore", "Kullanıcı giriş yapmamış!");
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        long timestamp = System.currentTimeMillis(); // Aktivitenin kaydedildiği zaman

        // LiveData'dan verileri al ve null kontrolü yap
        String elapsedTimeValue = elapsedTime.getValue() != null ? elapsedTime.getValue() : "00:00:00";
        double distanceValue = distance.getValue() != null ? distance.getValue() : 0.0;
        int caloriesValue = calories.getValue() != null ? calories.getValue() : 0;
        List<GeoPoint> routePointsValue = routePoints.getValue();

        // Eğer rota noktaları null ise hata vermemesi için boş liste ata
        if (routePointsValue == null) {
            routePointsValue = new ArrayList<>();
        }

        // GeoPoint listesini Firestore’a uygun `List<Map<String, Double>>` formatına çevir
        List<Map<String, Double>> formattedRoutePoints = new ArrayList<>();
        for (GeoPoint point : routePointsValue) {
            Map<String, Double> pointMap = new HashMap<>();
            pointMap.put("latitude", point.getLatitude());
            pointMap.put("longitude", point.getLongitude());
            formattedRoutePoints.add(pointMap);
        }

        // Firestore'a eklenmek üzere `ActivityRecord` nesnesini oluştur
        ActivityRecord record = new ActivityRecord(elapsedTimeValue, distanceValue, caloriesValue, formattedRoutePoints, timestamp, userId);

        // Kullanıcıya özel aktiviteleri Firestore’da kaydet
        db.collection("users")  // Kullanıcı koleksiyonu
                .document(userId)   // Kullanıcı ID'si
                .collection("activities") // Kullanıcı aktiviteleri
                .add(record)  // Yeni aktivite ekle
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Aktivite başarıyla eklendi!");
                    isActivitySaved = true; // Aktiviteyi kaydedilmiş olarak işaretle
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Aktivite eklenirken hata oluştu", e));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopActivity();
    }
}
