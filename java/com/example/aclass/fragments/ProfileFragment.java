package com.example.aclass.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.aclass.R;
import com.example.aclass.auth.LoginActivity;
import com.example.aclass.utils.CloudinaryConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView profileName, profileEmail, profileRole;
    private Button logoutBtn, changePicBtn, deletePicBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int IMAGE_PERMISSION_CODE = 2001;
    private Uri imageUri;

    private ProgressDialog progressDialog;
    private String currentImagePublicId;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Cloudinary once here
        CloudinaryConfig.initCloudinary(requireContext());

        profileImage = view.findViewById(R.id.profileImage);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileRole = view.findViewById(R.id.profileRole);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        changePicBtn = view.findViewById(R.id.changePicBtn);
        deletePicBtn = view.findViewById(R.id.deletePicBtn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return view;
        }

        String uid = mAuth.getCurrentUser().getUid();
        userRef = db.collection("Users").document(uid);

        loadProfile();

        changePicBtn.setOnClickListener(v -> checkAndRequestPermission());
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        deletePicBtn.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    private void loadProfile() {
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String name = snapshot.getString("name");
                String email = snapshot.getString("email");
                String role = snapshot.getString("role");
                String profilePicUrl = snapshot.getString("profilePicUrl");
                currentImagePublicId = snapshot.getString("profilePicPublicId");

                profileName.setText(name != null ? name : "N/A");
                profileEmail.setText(email != null ? email : "N/A");
                profileRole.setText(role != null ? role : "N/A");

                if (profilePicUrl != null) {
                    Glide.with(requireContext())
                            .load(profilePicUrl)
                            .signature(new ObjectKey(profilePicUrl))
                            .into(profileImage);
                }
            } else {
                Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, IMAGE_PERMISSION_CODE);
                return;
            }
        }
        openImagePicker();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == IMAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "Permission denied to access media", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (imageUri == null) return;

        changePicBtn.setEnabled(false);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String uid = mAuth.getCurrentUser().getUid();
        String publicId = "ProfilePics/" + uid;

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        currentImagePublicId = publicId;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profilePicUrl", imageUrl);
                        updates.put("profilePicPublicId", publicId);

                        userRef.update(updates)
                                .addOnSuccessListener(unused -> {
                                    Glide.with(requireContext())
                                            .load(imageUrl)
                                            .signature(new ObjectKey(imageUrl))
                                            .into(profileImage);
                                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    changePicBtn.setEnabled(true);
                                    if (progressDialog.isShowing()) progressDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to update profile info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    changePicBtn.setEnabled(true);
                                    if (progressDialog.isShowing()) progressDialog.dismiss();
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        changePicBtn.setEnabled(true);
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile Picture")
                .setMessage("Are you sure you want to delete your profile picture?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfilePicture())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfilePicture() {
        if (currentImagePublicId == null) {
            Toast.makeText(getContext(), "No profile picture to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Removing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePicUrl", null);
        updates.put("profilePicPublicId", null);

        userRef.update(updates)
                .addOnSuccessListener(unused -> {
                    profileImage.setImageResource(R.drawable.ic_profile); // placeholder image
                    Toast.makeText(getContext(), "Profile picture removed.", Toast.LENGTH_SHORT).show();
                    currentImagePublicId = null;
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
