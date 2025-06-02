package com.example.aclass;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RosterAdapter extends RecyclerView.Adapter<RosterAdapter.ViewHolder> {

    private List<UserModel> students;
    private Context context;
    private String classId;

    public RosterAdapter(List<UserModel> students, Context context, String classId) {
        this.students = students;
        this.context = context;
        this.classId = classId;
    }

    @NonNull
    @Override
    public RosterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RosterAdapter.ViewHolder holder, int position) {
        UserModel student = students.get(position);
        holder.nameText.setText(student.getName());
        holder.emailText.setText(student.getEmail());

        holder.removeBtn.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("Classes").document(classId)
                    .collection("EnrolledUsers").document(student.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Student removed successfully", Toast.LENGTH_SHORT).show();
                        students.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, students.size());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to remove student", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText;
        Button removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            emailText = itemView.findViewById(R.id.emailText);
            removeBtn = itemView.findViewById(R.id.removeBtn);
        }
    }
}