package com.example.walkingapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class NewActivityViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public NewActivityViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NewActivityViewModel.class)) {
            return (T) new NewActivityViewModel(application);
        }
        throw new IllegalArgumentException("Bilinmeyen ViewModel sınıfı: " + modelClass.getName());
    }
}
