package com.example.aclass.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aclass.R;
import com.example.aclass.utils.CloudinaryConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

public class NotificationsFragment extends Fragment {

    private Spinner classSpinner;
    private EditText messageInput;
    private Button sendButton;
    private ImageButton attachImageButton;

    private List<String> classIds = new ArrayList<>();
    private List<String> classNames = new ArrayList<>();
    private String selectedClassId;
    private File tempImageFile; // Local file for upload

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isTeacher = false;
    private static final String TAG = "NotificationsFragment";

    // Activity result launchers
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            tempImageFile = createTempImageFile(imageUri);
                            Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to select image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    public NotificationsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Initialize views
        classSpinner = view.findViewById(R.id.spinnerClasses);
        messageInput = view.findViewById(R.id.inputNotification);
        sendButton = view.findViewById(R.id.sendNotificationBtn);


        // Check for null views
        if (classSpinner == null || messageInput == null || sendButton == null || attachImageButton == null) {
            Toast.makeText(getContext(), "Error: UI components not found", Toast.LENGTH_LONG).show();
            return view;
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_LONG).show();
            disableUI();
            return view;
        }
        String currentUserId = currentUser.getUid();

        // Initialize Cloudinary
        try {
            CloudinaryConfig.initCloudinary(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cloudinary initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Check user role
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("teacher".equalsIgnoreCase(role)) {
                            isTeacher = true;
                            loadCreatedClasses(currentUserId);
                        } else {
                            isTeacher = false;
                            disableUI();
                            Toast.makeText(requireContext(), "Only teachers can send notifications", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error: User data not found", Toast.LENGTH_LONG).show();
                        disableUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to check role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    disableUI();
                });

        // Set up spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, classNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSpinner.setAdapter(adapter);

        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!classIds.isEmpty()) {
                    selectedClassId = classIds.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedClassId = null;
            }
        });

        // Image attachment
        attachImageButton.setOnClickListener(v -> {
            if (!isTeacher) {
                Toast.makeText(getContext(), "Only teachers can attach images", Toast.LENGTH_SHORT).show();
                return;
            }
            requestStoragePermission();
        });

        // Send notification
        sendButton.setOnClickListener(v -> {
            if (!isTeacher) {
                Toast.makeText(getContext(), "Only teachers can send notifications", Toast.LENGTH_SHORT).show();
                return;
            }
            String msg = messageInput.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(getContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            } else if (selectedClassId == null) {
                Toast.makeText(getContext(), "Select a class", Toast.LENGTH_SHORT).show();
            } else {
                sendNotification(msg);
            }
        });

        return view;
    }

    private void requestStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private File createTempImageFile(Uri imageUri) throws Exception {
        // Create a temporary file in the app's cache directory
        File cacheDir = requireContext().getCacheDir();
        File tempFile = File.createTempFile("image_", ".jpg", cacheDir);

        // Copy the content from the URI to the temporary file
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) {
                throw new Exception("Unable to open image stream");
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private void loadCreatedClasses(String userId) {
        db.collection("Users").document(userId).collection("CreatedClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    classIds.clear();
                    classNames.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("className");

                        if (id != null && name != null) {
                            classIds.add(id);
                            classNames.add(name);
                        }
                    }

                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) classSpinner.getAdapter();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    if (!classIds.isEmpty()) {
                        selectedClassId = classIds.get(0);
                        classSpinner.setSelection(0);
                    } else {
                        Toast.makeText(requireContext(), "No classes found", Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(false);
                        attachImageButton.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(false);
                    attachImageButton.setEnabled(false);
                });
    }

    private void sendNotification(String message) {
        long time = System.currentTimeMillis();
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("timestamp", time);
        notification.put("classId", selectedClassId);

        if (tempImageFile != null && tempImageFile.exists()) {
            try {
                // Upload image to Cloudinary using file path
                MediaManager.get().upload(tempImageFile.getAbsolutePath())
                        .option("upload_preset", CloudinaryConfig.getUploadPreset())
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {}

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                String imageUrl = resultData.get("url").toString();
                                notification.put("imageUrl", imageUrl);
                                saveNotification(notification);
                                cleanupTempFile();
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                Toast.makeText(requireContext(), "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                cleanupTempFile();
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {}
                        })
                        .dispatch();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to start image upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                cleanupTempFile();
            }
        } else {
            saveNotification(notification);
        }
    }

    private void saveNotification(Map<String, Object> notification) {
        db.collection("Notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Notification sent", Toast.LENGTH_SHORT).show();
                    messageInput.setText("");
                    tempImageFile = null;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cleanupTempFile() {
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
            tempImageFile = null;
        }
    }

    private void disableUI() {
        classSpinner.setEnabled(false);
        messageInput.setEnabled(false);
        sendButton.setEnabled(false);
        attachImageButton.setEnabled(false);
    }
}