package com.example.walkingapp.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.walkingapp.databinding.ItemPastActivityBinding;
import com.example.walkingapp.model.ActivityRecord;
import java.util.ArrayList;
import java.util.List;

public class PastActivitiesAdapter extends RecyclerView.Adapter<PastActivitiesAdapter.ViewHolder> {
    private List<ActivityRecord> activities = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ActivityRecord activity);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setActivityList(List<ActivityRecord> activities) {
        if (activities != null) {
            this.activities.clear();
            this.activities.addAll(activities);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPastActivityBinding binding = ItemPastActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityRecord activity = activities.get(position);

        holder.binding.tvElapsedTime.setText("Süre: " + activity.getElapsedTime());
        holder.binding.tvDistance.setText("Mesafe: " + String.format("%.2f km", activity.getDistance()));
        holder.binding.tvCalories.setText("Kalori: " + activity.getCalories());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPastActivityBinding binding;

        public ViewHolder(ItemPastActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
