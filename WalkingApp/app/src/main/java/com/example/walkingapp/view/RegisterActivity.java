package com.example.walkingapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.walkingapp.databinding.ActivityRegisterBinding;
import com.example.walkingapp.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    private Toast currentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 🔹 Kayıt ol butonu
        binding.btnRegister.setOnClickListener(view -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Lütfen geçerli bir mail adresi giriniz");
                return;
            }

            if (password.length() < 6) {
                showToast("Lütfen en az 6 haneli bir şifre giriniz");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showToast("Şifreler uyuşmuyor!");
                return;
            }

            showToast("Veriler yükleniyor...");
            viewModel.register(email, password);
        });

        // 🔹 Zaten hesabın var mı? -> Login ekranına git
        binding.btnLogin.setOnClickListener(view -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        // 🔹 Başarılı kayıt olursa login ekranına yönlendir
        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                showToast("Kayıt başarılı!");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        // 🔹 Hata mesajları (isteğe göre yorum satırına alınabilir)
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showToast("Hata: " + error);
            }
        });

        // 🔹 Yükleniyor animasyonu
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void showToast(String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
