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
import com.example.aclass.SubmissionModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignmentDetailsFragment extends Fragment {

    private TextView titleTextView, dueDateTextView, pointsTextView, fileTextView;
    private Button downloadFileButton, submitButton;
    private RecyclerView submissionsRecyclerView;
    private SubmissionAdapter submissionAdapter;
    private List<SubmissionModel> submissions;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String classId, assignmentId;
    private boolean isTeacher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedSubmissionUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assignment_details, container, false);

        titleTextView = view.findViewById(R.id.assignmentTitleTextView);
        dueDateTextView = view.findViewById(R.id.dueDateTextView);
        pointsTextView = view.findViewById(R.id.pointsTextView);
        fileTextView = view.findViewById(R.id.fileTextView);
        downloadFileButton = view.findViewById(R.id.downloadFileButton);
        submitButton = view.findViewById(R.id.submitButton);
        submissionsRecyclerView = view.findViewById(R.id.submissionsRecyclerView);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        submissions = new ArrayList<>();
        submissionAdapter = new SubmissionAdapter(getContext(), submissions, this::onDownloadSubmissionClick);
        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        submissionsRecyclerView.setAdapter(submissionAdapter);

        if (getArguments() != null) {
            classId = getArguments().getString("classId");
            assignmentId = getArguments().getString("assignmentId");
            isTeacher = getArguments().getBoolean("isTeacher");
        }

        submitButton.setVisibility(isTeacher ? View.GONE : View.VISIBLE);
        submissionsRecyclerView.setVisibility(isTeacher ? View.VISIBLE : View.GONE);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                selectedSubmissionUri = result.getData().getData();
                showSubmissionDialog();
            }
        });

        submitButton.setOnClickListener(v -> showSubmissionDialog());

        loadAssignmentDetails();

        return view;
    }

    private void loadAssignmentDetails() {
        if (assignmentId == null || classId == null) {
            Toast.makeText(getContext(), "Invalid assignment or class ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Assignments").document(assignmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        AssignmentModel assignment = doc.toObject(AssignmentModel.class);
                        if (assignment != null) {
                            titleTextView.setText(assignment.getTitle());
                            dueDateTextView.setText("Due: " + assignment.getDueDate());
                            pointsTextView.setText(assignment.getPoints() + " points");
                            fileTextView.setText(assignment.getFileName() != null ? "File: " + assignment.getFileName() : "No file attached");
                            downloadFileButton.setVisibility(assignment.getFileUrl() != null ? View.VISIBLE : View.GONE);
                            downloadFileButton.setOnClickListener(v -> {
                                if (assignment.getFileUrl() != null) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(assignment.getFileUrl()));
                                    startActivity(browserIntent);
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load assignment: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        if (isTeacher) {
            loadSubmissions();
        } else {
            checkStudentSubmission();
        }
    }

    private void loadSubmissions() {
        db.collection("AssignmentSubmissions")
                .whereEqualTo("assignmentId", assignmentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    submissions.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        SubmissionModel submission = doc.toObject(SubmissionModel.class);
                        submission.setDocId(doc.getId());
                        submissions.add(submission);
                    }
                    submissionAdapter.notifyDataSetChanged();
                    if (submissions.isEmpty()) {
                        Toast.makeText(getContext(), "No submissions yet", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load submissions: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkStudentSubmission() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("AssignmentSubmissions")
                .whereEqualTo("assignmentId", assignmentId)
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        submitButton.setEnabled(false);
                        submitButton.setText("Submitted");
                    }
                });
    }

    private void showSubmissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_submission_input, null);
        builder.setView(dialogView);

        Button attachFileButton = dialogView.findViewById(R.id.attachSubmissionButton);
        TextView fileNameText = dialogView.findViewById(R.id.submissionFileNameText);

        attachFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/msword", "application/vnd.ms-powerpoint"});
            filePickerLauncher.launch(intent);
        });

        if (selectedSubmissionUri != null) {
            fileNameText.setText(getFileName(selectedSubmissionUri));
        }

        builder.setTitle("Submit Assignment")
                .setPositiveButton("Submit", (dialog, which) -> {
                    if (selectedSubmissionUri == null) {
                        Toast.makeText(getContext(), "Please attach a file", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    uploadSubmission(selectedSubmissionUri);
                    selectedSubmissionUri = null;
                })
                .setNegativeButton("Cancel", (dialog, which) -> selectedSubmissionUri = null);

        builder.show();
    }

    private void uploadSubmission(Uri fileUri) {
        if (fileUri == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Invalid file or user", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + getFileName(fileUri);
        String studentId = mAuth.getCurrentUser().getUid();

        MediaManager.get().upload(fileUri)
                .option("public_id", "Submissions/" + classId + "/" + assignmentId + "/" + fileName)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(getContext(), "Uploading submission...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");
                        String publicId = (String) resultData.get("public_id");
                        SubmissionModel submission = new SubmissionModel(assignmentId, studentId, fileUrl, publicId,
                                fileName, System.currentTimeMillis(), null);
                        db.collection("AssignmentSubmissions").add(submission)
                                .addOnSuccessListener(doc -> {
                                    Toast.makeText(getContext(), "Submission uploaded successfully", Toast.LENGTH_SHORT).show();
                                    submitButton.setEnabled(false);
                                    submitButton.setText("Submitted");
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save submission: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void onDownloadSubmissionClick(SubmissionModel submission) {
        if (submission.getFileUrl() != null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getFileUrl()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(getContext(), "No file available", Toast.LENGTH_SHORT).show();
        }
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

class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder> {
    private final Context context;
    private final List<SubmissionModel> submissions;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SubmissionModel submission);
    }

    public SubmissionAdapter(Context context, List<SubmissionModel> submissions, OnItemClickListener listener) {
        this.context = context;
        this.submissions = submissions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_submission, parent, false);
        return new SubmissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubmissionViewHolder holder, int position) {
        SubmissionModel submission = submissions.get(position);
        holder.fileNameTextView.setText(submission.getFileName());
        holder.studentIdTextView.setText("Student ID: " + submission.getStudentId());
        holder.downloadButton.setOnClickListener(v -> listener.onItemClick(submission));
    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    static class SubmissionViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView, studentIdTextView;
        Button downloadButton;

        public SubmissionViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            studentIdTextView = itemView.findViewById(R.id.studentIdTextView);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }
    }
}