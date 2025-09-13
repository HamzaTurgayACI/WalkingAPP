package com.example.walkingapp.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.walkingapp.model.UserRepository;

public class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final UserRepository repository;

    public AuthViewModelFactory(UserRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(repository);
        }
        throw new IllegalArgumentException("Bilinmeyen ViewModel Sınıfı");
    }
}
