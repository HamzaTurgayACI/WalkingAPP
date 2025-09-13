package com.example.walkingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.walkingapp.R;
import com.example.walkingapp.databinding.ActivityDashboardBinding;
import com.example.walkingapp.viewmodel.DashboardViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {
    private ActivityDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel'i başlat
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Kullanıcı oturumunu kontrol et
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startLoginActivity();
            return;
        }

        // Animasyonu başlat
        startLogoAnimation();

        setupObservers();
        setupClickListeners();

        // ProgressBar kontrolü
        dashboardViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                // ProgressBar'ı göstermek için animasyon ekledik
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setIndeterminate(true);
                // Ekranı düzgün şekilde açmak için animasyon ekleyebilirsiniz
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.progressBar.setIndeterminate(false);
            }
        });
    }

    private void startLogoAnimation() {
        ImageView imgAnimatedLogo = binding.imgAnimatedLogo;
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation);
        imgAnimatedLogo.startAnimation(logoAnimation);
    }

    private void setupObservers() {
        dashboardViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                Log.d("Dashboard", "Kullanıcı verisi güncellendi: " + user);
                binding.tvTotalDistance.setText(formatDistance(user.getTotalDistance()));
                binding.tvTotalTime.setText(formatTime(user.getTotalTime()));
                binding.tvActivityCount.setText(formatCount(user.getActivityCount()));
            } else {
                Log.w("Dashboard", "Kullanıcı verisi null");
            }
        });
    }

    private void setupClickListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startLoginActivity();
        });

        binding.btnStartActivity.setOnClickListener(v -> {
            startActivity(new Intent(this, NewActivity.class));
        });

        binding.btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, PastActivitiesActivity.class));
        });

        binding.btnRefresh.setOnClickListener(v -> {
            // Yenileme işlemi başlatılıyor
            dashboardViewModel.refreshData();
            Toast.makeText(this, "Veriler yenileniyor...", Toast.LENGTH_SHORT).show();

            // ProgressBar'ı görünür yap ve veri yenilenmesini bekle
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setIndeterminate(true);
        });
    }

    private String formatDistance(double distance) {
        return String.format("Toplam Mesafe: %.2f km", distance);
    }

    private String formatTime(long minutes) {
        return String.format("Toplam Süre: %d dk", minutes / 3600); // 3600 yerine 60 olmalı
    }

    private String formatCount(long count) {
        return String.format("Toplam Aktivite: %d", count);
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
