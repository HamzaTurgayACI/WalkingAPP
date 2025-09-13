package com.example.walkingapp.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingapp.databinding.ActivityPastActivitiesBinding;
import com.example.walkingapp.model.ActivityRecord;
import com.example.walkingapp.viewmodel.PastActivitiesViewModel;

import java.util.List;

public class PastActivitiesActivity extends AppCompatActivity {
    private ActivityPastActivitiesBinding binding;
    private PastActivitiesViewModel viewModel;
    private PastActivitiesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPastActivitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PastActivitiesViewModel.class);

        adapter = new PastActivitiesAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getActivityRecords().observe(this, activities -> adapter.setActivityList(activities));

        binding.btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(PastActivitiesActivity.this, DashboardActivity.class);
            startActivity(intent);
        });
        adapter.setOnItemClickListener(activity -> {
            Intent intent = new Intent(this, PastActivityDetailsActivity.class);
            intent.putExtra("activityRecord", activity);
            startActivity(intent);
        });
    }
}
