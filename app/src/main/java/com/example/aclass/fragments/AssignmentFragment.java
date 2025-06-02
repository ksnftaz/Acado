package com.example.aclass.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.aclass.AssignmentModel;
import com.example.aclass.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignmentFragment extends Fragment {

    private RecyclerView assignmentsRecyclerView;
    private AssignmentAdapter assignmentAdapter;
    private List<AssignmentModel> assignments;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String classId;
    private boolean isTeacher;
    private FloatingActionButton uploadAssignmentBtn;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedFileUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assignment, container, false);

        assignmentsRecyclerView = view.findViewById(R.id.contentRecyclerView);
        uploadAssignmentBtn = view.findViewById(R.id.uploadAssignmentBtn);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        assignments = new ArrayList<>();
        assignmentAdapter = new AssignmentAdapter(getContext(), assignments, this::onViewDetailsClick);

        assignmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        assignmentsRecyclerView.setAdapter(assignmentAdapter);

        if (getArguments() != null) {
            classId = getArguments().getString("classId");
            isTeacher = getArguments().getBoolean("isTeacher", false);
        }

        uploadAssignmentBtn.setVisibility(isTeacher ? View.VISIBLE : View.GONE);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                selectedFileUri = result.getData().getData();
                showAssignmentInputDialog();
            }
        });

        uploadAssignmentBtn.setOnClickListener(v -> showAssignmentInputDialog());

        loadAssignments();

        return view;
    }

    private void loadAssignments() {
        if (classId == null) {
            Toast.makeText(getContext(), "Invalid class ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Assignments")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    assignments.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AssignmentModel assignment = doc.toObject(AssignmentModel.class);
                        assignment.setDocId(doc.getId());
                        assignments.add(assignment);
                    }
                    assignmentAdapter.notifyDataSetChanged();
                    if (assignments.isEmpty()) {
                        Toast.makeText(getContext(), "No assignments found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load assignments: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onViewDetailsClick(AssignmentModel assignment) {
        AssignmentDetailsFragment fragment = new AssignmentDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("assignmentId", assignment.getDocId());
        bundle.putString("classId", classId);
        bundle.putBoolean("isTeacher", isTeacher);
        fragment.setArguments(bundle);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showAssignmentInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_assignment_input, null);
        builder.setView(dialogView);

        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText dueDateInput = dialogView.findViewById(R.id.dueDateInput);
        EditText pointsInput = dialogView.findViewById(R.id.pointsInput);
        Button attachFileButton = dialogView.findViewById(R.id.attachFileButton);
        TextView fileNameText = dialogView.findViewById(R.id.fileNameText);

        attachFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/msword", "application/vnd.ms-powerpoint"});
            filePickerLauncher.launch(intent);
        });

        if (selectedFileUri != null) {
            fileNameText.setText(getFileName(selectedFileUri));
        }

        builder.setTitle("Create Assignment")
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String dueDate = dueDateInput.getText().toString().trim();
                    String points = pointsInput.getText().toString().trim();

                    if (title.isEmpty() || dueDate.isEmpty()) {
                        Toast.makeText(getContext(), "Title and due date are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    uploadAssignment(title, dueDate, points.isEmpty() ? "0" : points, selectedFileUri);
                    selectedFileUri = null;
                })
                .setNegativeButton("Cancel", (dialog, which) -> selectedFileUri = null);

        builder.show();
    }

    private void uploadAssignment(String title, String dueDate, String points, Uri fileUri) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uploaderEmail = mAuth.getCurrentUser().getEmail();
        AssignmentModel assignment = new AssignmentModel(title, dueDate, points, "Pending", classId,
                null, null, null, uploaderEmail);

        if (fileUri == null) {
            // Save assignment without file
            db.collection("Assignments").add(assignment)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Assignment created successfully", Toast.LENGTH_SHORT).show();
                        loadAssignments();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to create assignment: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + getFileName(fileUri);
        MediaManager.get().upload(fileUri)
                .option("public_id", "Assignments/" + classId + "/" + fileName)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(getContext(), "Uploading file...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");
                        String publicId = (String) resultData.get("public_id");
                        assignment.setFileUrl(fileUrl);
                        assignment.setPublicId(publicId);
                        assignment.setFileName(fileName);

                        db.collection("Assignments").add(assignment)
                                .addOnSuccessListener(doc -> {
                                    Toast.makeText(getContext(), "Assignment created successfully", Toast.LENGTH_SHORT).show();
                                    loadAssignments();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save assignment: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        return result != null && !result.trim().isEmpty() ? result : "file_" + System.currentTimeMillis();
    }
}

class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {
    private final Context context;
    private final List<AssignmentModel> assignments;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AssignmentModel assignment);
    }

    public AssignmentAdapter(Context context, List<AssignmentModel> assignments, OnItemClickListener listener) {
        this.context = context;
        this.assignments = assignments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        AssignmentModel assignment = assignments.get(position);
        holder.titleTextView.setText(assignment.getTitle());
        holder.dueDateTextView.setText(assignment.getDueDate() + " • " + assignment.getPoints() + " points");
        holder.statusTextView.setText(assignment.getStatus());
        holder.viewDetailsButton.setOnClickListener(v -> listener.onItemClick(assignment));
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, dueDateTextView, statusTextView;
        Button viewDetailsButton;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.assignmentTitleTextView);
            dueDateTextView = itemView.findViewById(R.id.assignmentDueDateTextView);
            statusTextView = itemView.findViewById(R.id.assignmentStatusTextView);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}