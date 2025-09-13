package com.example.walkingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.walkingapp.model.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthViewModel extends ViewModel {
    private final UserRepository repository;
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        this.repository = new UserRepository();
    }

    public AuthViewModel(UserRepository repository) {
        this.repository = repository;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void login(String email, String password) {
        isLoading.setValue(true); // Başlarken true

        repository.login(email, password).addOnCompleteListener(task -> {
            isLoading.setValue(false); // Bittiğinde false

            if (task.isSuccessful()) {
                loginSuccess.setValue(true);
            } else {
                errorMessage.setValue("Giriş başarısız: ");
            }
        });
    }


    public void register(String email, String password) {
        isLoading.setValue(true); // Yükleme başlasın

        repository.register(email, password).addOnCompleteListener(task -> {
            isLoading.setValue(false); // Yükleme dursun

            if (task.isSuccessful()) {
                loginSuccess.setValue(true);
            } else {
                errorMessage.setValue("Kayıt başarısız: ");
            }
        });
    }


    public void signInWithGoogle(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult();
            if (account != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                repository.signInWithGoogle(credential, new UserRepository.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        loginSuccess.postValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        errorMessage.postValue(message);
                    }
                });
            }
        } catch (Exception e) {
            errorMessage.postValue("Google ile giriş sırasında hata: " + e.getMessage());
        }
    }
}