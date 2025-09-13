package com.example.walkingapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.walkingapp.R;
import com.example.walkingapp.databinding.ActivityNewBinding;
import com.example.walkingapp.viewmodel.NewActivityViewModel;
import com.example.walkingapp.viewmodel.NewActivityViewModelFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.List;

public class NewActivity extends AppCompatActivity {
    private Marker userMarker;
    private ActivityNewBinding binding;
    private NewActivityViewModel viewModel;
    private Polyline routeLine;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private long lastUpdateTime = 0;
    private static final long MIN_UPDATE_INTERVAL = 2000; // Minimum 2 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osm_prefs", MODE_PRIVATE));

        binding = ActivityNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel'i Factory ile oluştur
        viewModel = new ViewModelProvider(this, new NewActivityViewModelFactory(getApplication()))
                .get(NewActivityViewModel.class);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(18.0);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                long currentTime = System.currentTimeMillis();
                // Konum güncellemelerini sınırlama
                if (currentTime - lastUpdateTime > MIN_UPDATE_INTERVAL) {
                    lastUpdateTime = currentTime;
                    for (Location location : locationResult.getLocations()) {
                        viewModel.updateLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        viewModel.getCenterLocation().observe(this, geoPoint -> {
            if (geoPoint != null) {
                binding.mapView.getController().setZoom(18.0);
                binding.mapView.getController().setCenter(geoPoint);
            }
        });

        binding.mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                //adjustMarkerScale();
                return true;
            }
        });

        adjustMarkerScale();

        binding.btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(NewActivity.this, DashboardActivity.class);
            startActivity(intent);
        });
        binding.btnStart.setOnClickListener(v -> startLocationUpdates());
        binding.btnStop.setOnClickListener(v -> {
            stopLocationUpdates(); // Konum güncellemeyi durdur
        });

        viewModel.getRoutePoints().observe(this, this::drawRoute);
        viewModel.getElapsedTime().observe(this, time -> binding.tvTime.setText("Süre: " + time));
        viewModel.getDistance().observe(this, distance -> binding.tvDistance.setText(String.format("%.2f km", distance)));
        viewModel.getCalories().observe(this, calories -> binding.tvCalories.setText("Kalori: " + calories));
        viewModel.getWeatherInfo().observe(this, weather -> binding.tvWeather.setText("Hava Durumu: " + weather));
        viewModel.fetchWeather("b4615278c24cb4106aed6fcf5c1f1f58");
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        viewModel.startActivity();

        viewModel.getCenterLocation().observe(this, geoPoint -> {
            if (geoPoint != null) {
                // Konum güncellenirse, marker'ı güncelle
                updateUserMarker(geoPoint);
            }
        });

    }

    private void stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback);
        viewModel.stopActivity();
    }

    private void drawRoute(List<GeoPoint> points) {
        if (routeLine != null) binding.mapView.getOverlayManager().remove(routeLine);
        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.getOutlinePaint().setStrokeWidth(6f);
        binding.mapView.getOverlayManager().add(routeLine);
        binding.mapView.invalidate();
    }

    private void adjustMarkerScale() {
        if (userMarker != null) {
            float zoomLevel = (float) binding.mapView.getZoomLevelDouble();
            int baseSize = 40;
            int newSize = (int) (baseSize * (zoomLevel / 18.0f));
            int minSize = 40;
            int maxSize = 200;
            newSize = Math.max(minSize, Math.min(maxSize, newSize));

            Drawable icon = ContextCompat.getDrawable(this, R.drawable.location_target_svgrepo_com);
            Bitmap bitmap = getBitmapFromDrawable(icon); // VectorDrawable desteği eklendi

            if (bitmap != null) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newSize, newSize, false);
                Drawable resizedIcon = new BitmapDrawable(getResources(), scaledBitmap);
                userMarker.setIcon(resizedIcon);
                binding.mapView.invalidate();
            } else {
                Log.e("Marker", "Bitmap dönüşümü başarısız oldu!");
            }
        }
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable) {
            VectorDrawable vectorDrawable = (VectorDrawable) drawable;
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
            return bitmap;
        } else {
            Log.e("Marker", "Desteklenmeyen Drawable türü!");
            return null;
        }
    }

    private void updateUserMarker(GeoPoint location) {
        // Marker'ı sadece ilk defa eklemek veya güncellemek
        if (userMarker == null) {
            userMarker = new Marker(binding.mapView);
            userMarker.setPosition(location);  // Konumu ayarlayın
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            userMarker.setTitle("Senin Konumun");

            // Marker simgesini ayarlayın
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.location_target_svgrepo_com);
            Bitmap bitmap = getBitmapFromDrawable(icon); // Bitmap dönüşümünü yapın
            if (bitmap != null) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, false); // Boyutunu ayarlayın
                Drawable resizedIcon = new BitmapDrawable(getResources(), scaledBitmap);
                userMarker.setIcon(resizedIcon);
            }

            // Marker'ı haritaya ekleyin
            binding.mapView.getOverlays().add(userMarker);
        } else {
            userMarker.setPosition(location);  // Marker'ı güncelleyin
        }

        // Haritayı güncelleyin
        binding.mapView.invalidate();
        // Harita merkezini marker'a yönlendirin
        binding.mapView.getController().setCenter(location);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}


