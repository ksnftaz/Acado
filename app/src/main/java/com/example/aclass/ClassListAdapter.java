package com.example.aclass;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.cls.ClassDetailsActivity;

import java.util.List;

public class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ClassViewHolder> {

    private Context context;
    private List<ClassModel> classList;
    private boolean isTeacher;

    public ClassListAdapter(Context context, List<ClassModel> classList, boolean isTeacher) {
        this.context = context;
        this.classList = classList;
        this.isTeacher = isTeacher;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_class_card, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel classroom = classList.get(position);

        holder.className.setText(classroom.getClassName());
        holder.classCode.setText("Code: " + classroom.getClassCode());

        String createdByName = classroom.getCreatedByName();
        if (createdByName == null || createdByName.isEmpty()) {
            createdByName = "Unknown";
        }
        holder.createdBy.setText("Created by: " + createdByName);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ClassDetailsActivity.class);
            intent.putExtra("classId", classroom.getClassId());
            intent.putExtra("classCode", classroom.getClassCode());
            intent.putExtra("className", classroom.getClassName());
            intent.putExtra("isTeacher", isTeacher);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView className, classCode, createdBy;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.tvClassName);
            classCode = itemView.findViewById(R.id.tvClassCode);
            createdBy = itemView.findViewById(R.id.tvCreatedBy);
        }
    }

    // Method to update the adapter’s data
    public void setData(List<ClassModel> newList) {
        classList.clear();
        classList.addAll(newList);
        notifyDataSetChanged();
    }
}
