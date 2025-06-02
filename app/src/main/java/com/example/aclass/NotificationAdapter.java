package com.example.aclass;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotiViewHolder> {

    private List<NotificationModel> notificationList;

    public NotificationAdapter(List<NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    public static class NotiViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;

        public NotiViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notificationMessage);
            time = itemView.findViewById(R.id.notificationTime);
        }
    }

    @NonNull
    @Override
    public NotiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotiViewHolder holder, int position) {
        NotificationModel model = notificationList.get(position);
        holder.message.setText(model.getMessage());

        String formattedTime = DateFormat.format("dd MMM yyyy, hh:mm a", model.getTimestamp()).toString();
        holder.time.setText(formattedTime);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }
}