package com.example.aclass.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aclass.R;
import com.example.aclass.utils.CloudinaryUploadHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadFragment extends Fragment {

    private static final String TAG = "UploadFragment";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB limit

    private Button btnSelectFile, btnUpload;
    private TextView tvSelectedFile, tvUploadStatus;
    private ProgressBar progressBar;

    private Uri selectedFileUri;
    private String selectedFileName;
    private long selectedFileSize;
    private String selectedMimeType;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private String classId;
    private String className;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    private boolean isUploading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        try {
            if (getArguments() != null) {
                classId = getArguments().getString("classId");
                className = getArguments().getString("className");
                Log.d(TAG, "Fragment created for classId: " + classId);
            }

            initViews(view);
            initFirebase();
            setupActivityResultLaunchers();
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            showError("Error initializing upload fragment: " + e.getMessage());
        }

        return view;
    }

    private void initViews(View view) {
        btnSelectFile = view.findViewById(R.id.btnSelectFile);
        btnUpload = view.findViewById(R.id.btnUpload);
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        tvUploadStatus = view.findViewById(R.id.tvUploadStatus);
        progressBar = view.findViewById(R.id.progressBar);

        resetUploadUI();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Verify user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated");
            showError("Please login to upload files");
        }
    }

    private void setupActivityResultLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == requireActivity().RESULT_OK &&
                                result.getData() != null &&
                                result.getData().getData() != null) {

                            selectedFileUri = result.getData().getData();
                            Log.d(TAG, "File selected: " + selectedFileUri.toString());
                            handleSelectedFile(selectedFileUri);
                        } else {
                            Log.d(TAG, "File selection cancelled or failed");
                            tvUploadStatus.setText("File selection cancelled");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling file selection result", e);
                        showError("Error processing selected file: " + e.getMessage());
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Storage permission granted");
                        openFilePicker();
                    } else {
                        Log.w(TAG, "Storage permission denied");
                        showError("Storage permission is required to select files");
                    }
                });
    }

    private void setupClickListeners() {
        btnSelectFile.setOnClickListener(v -> {
            if (!isUploading) {
                if (checkPermissions()) {
                    openFilePicker();
                }
            }
        });

        btnUpload.setOnClickListener(v -> {
            if (!isUploading && validateFileSelection()) {
                uploadFile();
            }
        });
    }

    private boolean checkPermissions() {
        // For Android 13+ (API 33), we don't need READ_EXTERNAL_STORAGE for file picker
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting storage permission");
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");

            // Fixed MIME types (removed invalid ones)
            String[] mimeTypes = {
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel", // Fixed: was "application/xxls"
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint", // Fixed: was "application/pptx"
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "image/jpeg",
                    "image/png",
                    "video/mp4",
                    "text/plain"
            };

            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            Intent chooser = Intent.createChooser(intent, "Select File to Upload");

            // Verify there's an app that can handle this intent
            if (chooser.resolveActivity(requireContext().getPackageManager()) != null) {
                Log.d(TAG, "Opening file picker");
                filePickerLauncher.launch(chooser);
            } else {
                showError("No file manager app available");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            showError("Error opening file picker: " + e.getMessage());
        }
    }

    public long getFolderSize(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            Log.e("getFolderSize", "Invalid folder: " + (folder != null ? folder.getAbsolutePath() : "null"));
            return 0;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            Log.e("getFolderSize", "listFiles() returned null for folder: " + folder.getAbsolutePath());
            return 0;
        }

        long size = 0;
        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            } else if (file.isDirectory()) {
                size += getFolderSize(file);
            }
        }
        return size;
    }


    private void handleSelectedFile(Uri fileUri) {
        try {
            // Get file information safely
            FileInfo fileInfo = getFileInfo(fileUri);

            if (fileInfo == null) {
                showError("Unable to read file information");
                return;
            }

            selectedFileName = fileInfo.name;
            selectedFileSize = fileInfo.size;
            selectedMimeType = fileInfo.mimeType;

            Log.d(TAG, String.format("File info - Name: %s, Size: %d bytes, Type: %s",
                    selectedFileName, selectedFileSize, selectedMimeType));

            // Validate file size
            if (selectedFileSize > MAX_FILE_SIZE) {
                showError("File is too large. Maximum size is " + (MAX_FILE_SIZE / (1024 * 1024)) + " MB");
                resetFileSelection();
                return;
            }

            // Update UI
            String sizeText = formatFileSize(selectedFileSize);
            tvSelectedFile.setText(String.format("Selected: %s (%s)", selectedFileName, sizeText));
            tvUploadStatus.setText("Ready to upload");
            btnUpload.setVisibility(View.VISIBLE);
            btnUpload.setEnabled(true);

        } catch (Exception e) {
            Log.e(TAG, "Error handling selected file", e);
            showError("Error processing selected file: " + e.getMessage());
            resetFileSelection();
        }
    }

    private FileInfo getFileInfo(Uri uri) {
        FileInfo fileInfo = new FileInfo();
        Cursor cursor = null;

        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Get file name
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileInfo.name = cursor.getString(nameIndex);
                }

                // Get file size
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    fileInfo.size = cursor.getLong(sizeIndex);
                }
            }

            // Fallback for file name
            if (fileInfo.name == null || fileInfo.name.trim().isEmpty()) {
                String path = uri.getLastPathSegment();
                fileInfo.name = (path != null) ? path : "file_" + System.currentTimeMillis();
            }

            // Get MIME type
            fileInfo.mimeType = requireContext().getContentResolver().getType(uri);
            if (fileInfo.mimeType == null) {
                fileInfo.mimeType = "*/*";
            }

            return fileInfo;

        } catch (Exception e) {
            Log.e(TAG, "Error getting file info", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean validateFileSelection() {
        if (selectedFileUri == null) {
            showError("Please select a file first");
            return false;
        }

        if (selectedFileName == null || selectedFileName.trim().isEmpty()) {
            showError("Invalid file name");
            return false;
        }

        if (classId == null || classId.trim().isEmpty()) {
            showError("Invalid class ID");
            return false;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Please login to upload files");
            return false;
        }

        return true;
    }

    private void uploadFile() {
        if (isUploading) {
            Log.w(TAG, "Upload already in progress");
            return;
        }

        Log.d(TAG, "Starting file upload");
        setUploadingState(true);

        tvUploadStatus.setText("Preparing upload...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        CloudinaryUploadHelper.uploadFile(this, selectedFileUri, selectedFileName, classId,
                new CloudinaryUploadHelper.UploadListener() {
                    @Override
                    public void onUploadStart(String requestId) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvUploadStatus.setText("Upload started...");
                                Log.d(TAG, "Upload started with request ID: " + requestId);
                            });
                        }
                    }

                    @Override
                    public void onUploadSuccess(String publicId, String secureUrl, Map<String, Object> allData) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.d(TAG, "Upload successful: " + publicId);
                                progressBar.setVisibility(View.GONE);
                                tvUploadStatus.setText("Upload successful!");
                                saveFileToFirestore(publicId, secureUrl, allData);
                                showSuccess("File uploaded successfully!");
                            });
                        }
                    }

                    @Override
                    public void onUploadFailure(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.e(TAG, "Upload failed: " + error);
                                progressBar.setVisibility(View.GONE);
                                tvUploadStatus.setText("Upload failed");
                                showError("Upload failed: " + error);
                                setUploadingState(false);
                            });
                        }
                    }

                    @Override
                    public void onUploadProgress(long bytesUploaded, long totalBytes) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                int progress = (int) ((bytesUploaded * 100) / totalBytes);
                                progressBar.setProgress(progress);
                                tvUploadStatus.setText("Uploading... " + progress + "%");
                            });
                        }
                    }
                });
    }

    private void saveFileToFirestore(String publicId, String secureUrl, Map<String, Object> cloudinaryData) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                showError("User authentication lost");
                setUploadingState(false);
                return;
            }

            Map<String, Object> fileData = new HashMap<>();
            fileData.put("publicId", publicId);
            fileData.put("secureUrl", secureUrl);
            fileData.put("fileName", selectedFileName);
            fileData.put("fileSize", selectedFileSize);
            fileData.put("mimeType", selectedMimeType);
            fileData.put("fileType", CloudinaryUploadHelper.getResourceType(selectedFileName));
            fileData.put("format", cloudinaryData.get("format"));
            fileData.put("bytes", cloudinaryData.get("bytes"));
            fileData.put("uploadedAt", System.currentTimeMillis());
            fileData.put("uploadedBy", currentUser.getUid());
            fileData.put("uploaderEmail", currentUser.getEmail());
            fileData.put("classId", classId);

            if (className != null && !className.trim().isEmpty()) {
                fileData.put("className", className);
            }

            firestore.collection("classes")
                    .document(classId)
                    .collection("uploads")
                    .add(fileData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "File data saved to Firestore: " + documentReference.getId());
                        tvUploadStatus.setText("File saved successfully!");
                        sendNotification();
                        resetUploadUI();
                        setUploadingState(false);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving file data to Firestore", e);
                        tvUploadStatus.setText("Error saving file info");
                        showError("Error saving upload info: " + e.getMessage());
                        setUploadingState(false);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in saveFileToFirestore", e);
            showError("Error saving file: " + e.getMessage());
            setUploadingState(false);
        }
    }

    private void sendNotification() {
        try {
            DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                    .getReference("class_notifications")
                    .child(classId)
                    .push(); // creates a new unique notification

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "User is not authenticated for sending notification");
                return;
            }

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("title", "New Material Uploaded");
            notificationData.put("message", selectedFileName + " has been uploaded.");
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("senderId", currentUser.getUid());
            notificationData.put("senderEmail", currentUser.getEmail());
            notificationData.put("classId", classId);
            notificationData.put("className", className);
            notificationData.put("type", "material");

            notificationRef.setValue(notificationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification sent successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send notification", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in sendNotification", e);
        }
    }

    private void setUploadingState(boolean uploading) {
        isUploading = uploading;
        btnSelectFile.setEnabled(!uploading);
        btnUpload.setEnabled(!uploading);
    }

    private void resetUploadUI() {
        resetFileSelection();
        tvUploadStatus.setText("");
        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
    }

    private void resetFileSelection() {
        selectedFileUri = null;
        selectedFileName = null;
        selectedFileSize = 0;
        selectedMimeType = null;
        tvSelectedFile.setText("No file selected");
        btnUpload.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, message);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // Helper class to store file information
    private static class FileInfo {
        String name;
        long size;
        String mimeType;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        selectedFileUri = null;
        btnSelectFile = null;
        btnUpload = null;
        tvSelectedFile = null;
        tvUploadStatus = null;
        progressBar = null;
    }
}

