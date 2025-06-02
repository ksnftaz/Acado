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

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(ClassModel file);
    }

    private List<ClassModel> classList;
    private Context context;
    private boolean showClickAction = false;
    private OnFileClickListener fileClickListener;

    // Constructor for dashboard-style usage (clickable to open details)
    public ClassAdapter(List<ClassModel> classList, Context context) {
        this.classList = classList;
        this.context = context;
        this.showClickAction = true;
    }

    // Constructor for profile-style usage (non-clickable)
    public ClassAdapter(List<ClassModel> classList, Context context, boolean showClickAction) {
        this.classList = classList;
        this.context = context;
        this.showClickAction = showClickAction;
    }

    // Constructor for file-list usage (custom click listener)
    public ClassAdapter(List<ClassModel> classList, OnFileClickListener listener) {
        this.classList = classList;
        this.fileClickListener = listener;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel model = classList.get(position);

        holder.className.setText(model.getClassName());
        holder.classCode.setText("Code: " + model.getClassCode());
        holder.teacherName.setText("Teacher: " + (model.getCreatedByName() != null ? model.getCreatedByName() : "Unknown"));
        holder.subject.setText("Subject: " + (model.getSubject() != null ? model.getSubject() : "N/A"));

        if (fileClickListener != null) {
            holder.itemView.setOnClickListener(v -> fileClickListener.onFileClick(model));
        } else if (showClickAction && context != null) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ClassDetailsActivity.class);
                intent.putExtra("classId", model.getClassId());
                intent.putExtra("className", model.getClassName());
                context.startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return classList != null ? classList.size() : 0;
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView className, classCode, teacherName, subject;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.classNameText);
            classCode = itemView.findViewById(R.id.classCodeText);
            teacherName = itemView.findViewById(R.id.teacherNameText);
            subject = itemView.findViewById(R.id.subjectText);
        }
    }
}
