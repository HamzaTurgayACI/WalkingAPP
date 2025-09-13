package com.example.walkingapp.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingapp.databinding.ActivityPastActivityDetailsBinding;
import com.example.walkingapp.model.ActivityRecord;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.util.List;

public class PastActivityDetailsActivity extends AppCompatActivity {
    private ActivityPastActivityDetailsBinding binding;
    private ActivityRecord activity;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPastActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Aktivite verisini al
        activity = (ActivityRecord) getIntent().getSerializableExtra("activityRecord");

        if (activity == null) {
            Log.e("ActivityDetails", "Aktivite verisi bulunamadı!");
            finish();  // Aktiviteyi kapat
            return;
        }

        // UI Güncelleme
        binding.tvTime.setText("Süre: " + activity.getElapsedTime());
        binding.tvDistance.setText(String.format("Mesafe: %.2f km", activity.getDistance()));
        binding.tvCalories.setText("Kalori: " + activity.getCalories());

        binding.btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(PastActivityDetailsActivity.this, PastActivitiesActivity.class);
            startActivity(intent);
        });

        //  Harita Ayarları
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK); // OpenStreetMap kaynak ayarı
        mapView.setZoomRounding(true);  // Yakınlaştırma butonları
        mapView.setMultiTouchControls(true);   // Çoklu dokunmatik desteği
        mapView.getTileProvider().clearTileCache(); // Eski tile'ları temizle

        //  Rota verisini al ve haritaya ekle
        List<GeoPoint> routePoints = activity.getGeoPoints();
        if (routePoints == null || routePoints.isEmpty()) {
            Log.e("MapError", "Rota noktaları bulunamadı!");
        } else {
            drawRoute(routePoints);
            centerMapOnRoute(routePoints);
        }
    }

    //  Rota çizme fonksiyonu
    private void drawRoute(List<GeoPoint> points) {
        Polyline routeLine = new Polyline();
        routeLine.getOutlinePaint().setColor(Color.RED); // Rotanın rengi kırmızı
        routeLine.getOutlinePaint().setStrokeWidth(6f); // Çizgi kalınlığı
        routeLine.setPoints(points);

        mapView.getOverlays().add(routeLine);
        mapView.invalidate(); // Haritayı yenile

        // 🔹 Firestore’dan çekilen rota verilerini logla
        for (GeoPoint point : points) {
            Log.d("RouteData", "Lat: " + point.getLatitude() + ", Lon: " + point.getLongitude());
        }
    }


    private void centerMapOnRoute(List<GeoPoint> routePoints) {
        if (!routePoints.isEmpty()) {
            IMapController mapController = mapView.getController();
            mapController.setZoom(15.0); // Zoom seviyesi
            mapController.setCenter(routePoints.get(0)); // Rotanın ilk noktasına odaklan
        }
    }
}

