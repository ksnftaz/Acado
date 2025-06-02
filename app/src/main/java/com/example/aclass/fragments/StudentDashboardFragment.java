package com.example.aclass.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;

import com.example.aclass.ClassAdapter;
import com.example.aclass.ClassModel;
import com.example.aclass.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardFragment extends Fragment {

    private RecyclerView joinedClassesRecyclerView;
    private FloatingActionButton joinClassBtn;
    private List<ClassModel> joinedClasses;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String studentId;

    public StudentDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        joinedClassesRecyclerView = view.findViewById(R.id.joinedClassesRecyclerView);
        joinClassBtn = view.findViewById(R.id.joinClassBtn);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        studentId = mAuth.getCurrentUser().getUid();

        joinedClasses = new ArrayList<>();
        classAdapter = new ClassAdapter(joinedClasses, getContext());

        joinedClassesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        joinedClassesRecyclerView.setAdapter(classAdapter);

        // Add sparkle animation to FAB
        addSparkleToFAB();

        loadJoinedClasses();

        joinClassBtn.setOnClickListener(v -> {
            // Add click animation
            animateButtonClick(v);
            showEnhancedJoinClassDialog();
        });

        return view;
    }

    private void addSparkleToFAB() {
        // Subtle pulse animation for the FAB
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(joinClassBtn, "scaleX", 1.0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(joinClassBtn, "scaleY", 1.0f, 1.1f, 1.0f);

        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    private void animateButtonClick(View view) {
        // Quick scale animation on click
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.9f);
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1.0f);

        scaleDown.setDuration(100);
        scaleUp.setDuration(100);

        scaleDown.start();
        scaleUp.setStartDelay(100);
        scaleUp.start();
    }

    private void loadJoinedClasses() {
        db.collection("JoinedClasses")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    joinedClasses.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String classId = doc.getString("classId");

                        db.collection("Classes").document(classId)
                                .get().addOnSuccessListener(classDoc -> {
                                    ClassModel model = classDoc.toObject(ClassModel.class);
                                    if (model != null) {
                                        joinedClasses.add(model);
                                        classAdapter.notifyDataSetChanged();

                                        // Add subtle entrance animation for new items
                                        animateRecyclerViewItem();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showStyledToast("Error loading classes", false);
                });
    }

    private void animateRecyclerViewItem() {
        // Fade in animation for recycler view
        joinedClassesRecyclerView.setAlpha(0.7f);
        joinedClassesRecyclerView.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
    }

    private void showEnhancedJoinClassDialog() {
        // Create custom dialog view
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_class, null);

        // Find views
        TextInputLayout inputLayout = dialogView.findViewById(R.id.classCodeInputLayout);
        TextInputEditText classCodeInput = dialogView.findViewById(R.id.classCodeInput);
        MaterialButton joinButton = dialogView.findViewById(R.id.joinButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Create dialog with rounded corners
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Add entrance animation to dialog
        dialog.setOnShowListener(dialogInterface -> {
            View decorView = dialog.getWindow().getDecorView();
            decorView.setScaleX(0.8f);
            decorView.setScaleY(0.8f);
            decorView.setAlpha(0.5f);

            decorView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        });

        // Enhanced join button click
        joinButton.setOnClickListener(v -> {
            String code = classCodeInput.getText().toString().trim().toUpperCase();

            if (code.isEmpty()) {
                inputLayout.setError("Please enter a class code");
                shakeView(inputLayout);
                return;
            }

            if (code.length() < 4) {
                inputLayout.setError("Class code must be at least 4 characters");
                shakeView(inputLayout);
                return;
            }

            // Clear any previous errors
            inputLayout.setError(null);

            // Show loading state
            joinButton.setText("Joining...");
            joinButton.setEnabled(false);

            // Add success animation and close dialog
            findClassByCodeAndJoinEnhanced(code, dialog, joinButton);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void shakeView(View view) {
        // Shake animation for errors
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        shake.setDuration(500);
        shake.start();
    }

    private void findClassByCodeAndJoinEnhanced(String code, AlertDialog dialog, MaterialButton joinButton) {
        db.collection("Classes")
                .whereEqualTo("classCode", code)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot classDoc = query.getDocuments().get(0);
                        String classId = classDoc.getId();

                        // Check if already joined
                        checkIfAlreadyJoined(classId, code, dialog, joinButton);

                    } else {
                        // Reset button state
                        joinButton.setText("Join Class");
                        joinButton.setEnabled(true);
                        showStyledToast("❌ Invalid class code", false);
                    }
                })
                .addOnFailureListener(e -> {
                    joinButton.setText("Join Class");
                    joinButton.setEnabled(true);
                    showStyledToast("❌ Error finding class", false);
                });
    }

    private void checkIfAlreadyJoined(String classId, String code, AlertDialog dialog, MaterialButton joinButton) {
        db.collection("JoinedClasses")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Already joined
                        joinButton.setText("Join Class");
                        joinButton.setEnabled(true);
                        showStyledToast("ℹ️ You're already in this class", false);
                    } else {
                        // Join the class
                        joinClass(classId, dialog, joinButton);
                    }
                });
    }

    private void joinClass(String classId, AlertDialog dialog, MaterialButton joinButton) {
        Map<String, Object> joinInfo = new HashMap<>();
        joinInfo.put("studentId", studentId);
        joinInfo.put("classId", classId);

        db.collection("JoinedClasses")
                .add(joinInfo)
                .addOnSuccessListener(documentReference -> {
                    // Success animation
                    joinButton.setText("✓ Joined!");
                    joinButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));

                    // Delayed actions
                    joinButton.postDelayed(() -> {
                        dialog.dismiss();
                        showStyledToast("🎉 Successfully joined class!", true);
                        loadJoinedClasses();

                        // Celebrate with FAB animation
                        celebrateFAB();
                    }, 800);
                })
                .addOnFailureListener(e -> {
                    joinButton.setText("Join Class");
                    joinButton.setEnabled(true);
                    showStyledToast("❌ Failed to join class", false);
                });
    }

    private void celebrateFAB() {
        // Celebration animation for FAB
        ObjectAnimator rotation = ObjectAnimator.ofFloat(joinClassBtn, "rotation", 0f, 360f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(joinClassBtn, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(joinClassBtn, "scaleY", 1f, 1.3f, 1f);

        rotation.setDuration(600);
        scaleX.setDuration(600);
        scaleY.setDuration(600);

        rotation.start();
        scaleX.start();
        scaleY.start();
    }

    private void showStyledToast(String message, boolean isSuccess) {
        // Create custom toast with styling
        Toast toast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);

        // You can customize toast appearance here if needed
        toast.show();
    }

    // Legacy method for fallback (keeping your original simple dialog)
    private void showJoinClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Join Class");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter Class Code");
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                findClassByCodeAndJoin(code);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Legacy method (keeping your original for fallback)
    private void findClassByCodeAndJoin(String code) {
        db.collection("Classes")
                .whereEqualTo("classCode", code)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot classDoc = query.getDocuments().get(0);
                        String classId = classDoc.getId();

                        Map<String, Object> joinInfo = new HashMap<>();
                        joinInfo.put("studentId", studentId);
                        joinInfo.put("classId", classId);

                        db.collection("JoinedClasses")
                                .add(joinInfo)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Class joined!", Toast.LENGTH_SHORT).show();
                                    loadJoinedClasses();
                                });
                    } else {
                        Toast.makeText(requireContext(), "Invalid class code", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}