package com.example.walkingapp.view;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.walkingapp.R;
import com.example.walkingapp.databinding.ActivityLoginBinding;
import com.example.walkingapp.model.UserRepository;
import com.example.walkingapp.viewmodel.AuthViewModel;
import com.example.walkingapp.viewmodel.AuthViewModelFactory;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;

    private Toast currentToast;
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleGoogleSignInResult(task);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Google Sign-In ayarlarını yapılandır
        configureGoogleSignIn();

        // ViewModel'i başlat
        UserRepository repository = new UserRepository();
        AuthViewModelFactory factory = new AuthViewModelFactory(repository);
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        setupObservers();
        setupClickListeners();

        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

    }



    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);


        googleSignInClient.silentSignIn()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        signOutFromGoogle();
                    }
                });
    }

    private void signOutFromGoogle() {
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    Log.d("GoogleSignIn", "Kullanıcı çıkış yaptı");
                });
    }

    private void setupObservers() {
        authViewModel.getLoginSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                showToast("Giriş Başarılı!");
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }
        });

        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                if (error.contains("ERROR_WRONG_PASSWORD")) {
                    showToast("Kullanıcı maili ile şifre uyuşmuyor");
                } else if (error.contains("ERROR_USER_NOT_FOUND")) {
                    showToast("Kullanıcı bulunamadı");
                } else {
                    showToast("Giriş başarısız!");
                }
            }
        });
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen e-posta ve şifre girin!", Toast.LENGTH_SHORT).show();
            } else {
                authViewModel.login(email, password);
            }
        });

        binding.btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> launchGoogleSignIn());


        binding.forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

    }

    private void launchGoogleSignIn() {
        // Her seferinde hesap seçim ekranını göster
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Log.d("GoogleSignIn", "ID Token: " + account.getIdToken());
                authViewModel.signInWithGoogle(completedTask);
            }
        } catch (ApiException e) {
            String errorMessage = "Google ile giriş başarısız: " + e.getStatusCode();
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
        }
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