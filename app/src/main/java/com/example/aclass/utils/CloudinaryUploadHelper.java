package com.example.aclass.utils;

import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.UploadRequest;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryUploadHelper {
    private static final String TAG = "CloudinaryUpload";

    public interface UploadListener {
        void onUploadStart(String requestId);
        void onUploadSuccess(String publicId, String secureUrl, Map<String, Object> allData);
        void onUploadFailure(String error);
        void onUploadProgress(long bytesUploaded, long totalBytes);
    }

    public static void uploadFile(Fragment fragment, Uri fileUri, String fileName, String classId, UploadListener listener) {
        String adjustedFileName = fileName.contains(".") ? fileName : fileName + ".dat";
        String resourceType = getResourceType(adjustedFileName);

        Map<String, Object> options = new HashMap<>();
        options.put("public_id", generatePublicId(adjustedFileName));
        options.put("resource_type", resourceType);
        options.put("folder", "aclass_uploads");

        UploadRequest<?> request = MediaManager.get().upload(fileUri)
                .unsigned(CloudinaryConfig.getUploadPreset());

        for (Map.Entry<String, Object> entry : options.entrySet()) {
            request = request.option(entry.getKey(), entry.getValue());
        }

        String finalFileName = adjustedFileName;

        request.callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d(TAG, "Upload started: " + requestId);
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() -> listener.onUploadStart(requestId));
                } else {
                    Log.w(TAG, "Fragment not attached onStart.");
                }
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() -> listener.onUploadProgress(bytes, totalBytes));
                }
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String publicId = (String) resultData.get("public_id");
                String secureUrl = (String) resultData.get("secure_url");
                Log.d(TAG, "Upload successful: " + secureUrl);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseAuth auth = FirebaseAuth.getInstance();

                String uploaderId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "unknown";
                String uploaderEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "unknown@example.com";

                Map<String, Object> fileData = new HashMap<>();
                fileData.put("publicId", publicId);
                fileData.put("secureUrl", secureUrl);
                fileData.put("fileName", finalFileName);
                fileData.put("fileType", resourceType);
                fileData.put("format", resultData.get("format"));
                fileData.put("bytes", resultData.get("bytes"));
                fileData.put("timestamp", FieldValue.serverTimestamp());
                fileData.put("uploaderId", uploaderId);
                fileData.put("uploaderEmail", uploaderEmail);
                fileData.put("classId", classId);

                db.collection("classes")
                        .document(classId)
                        .collection("uploads")
                        .add(fileData)
                        .addOnSuccessListener(documentReference -> {
                            Log.d(TAG, "File info saved to Firestore: " + documentReference.getId());
                            if (fragment.isAdded()) {
                                fragment.requireActivity().runOnUiThread(() ->
                                        listener.onUploadSuccess(publicId, secureUrl, resultData));
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to save to Firestore", e);
                            if (fragment.isAdded()) {
                                fragment.requireActivity().runOnUiThread(() ->
                                        listener.onUploadFailure("Firestore save error: " + e.getMessage()));
                            }
                        });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.e(TAG, "Upload failed: " + error.getDescription());
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() ->
                            listener.onUploadFailure(error.getDescription()));
                }
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() ->
                            listener.onUploadFailure("Upload rescheduled: " + error.getDescription()));
                }
            }
        }).dispatch();
    }

    public static String getResourceType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "webp":
                return "image";
            case "mp4":
            case "avi":
            case "mov":
            case "mkv":
                return "video";
            case "pdf":
            case "doc":
            case "docx":
            case "ppt":
            case "pptx":
            case "txt":
            case "xls":
            case "xlsx":
                return "raw";
            default:
                return "auto";
        }
    }

    private static String generatePublicId(String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nameWithoutExtension = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf("."))
                : fileName;
        return nameWithoutExtension + "_" + timestamp;
    }

    public static void deleteFromCloudinary(String publicId, DeletionCallback callback) {
        Map<String, Object> options = new HashMap<>();
        options.put("invalidate", true);

        try {
            Map result = MediaManager.get().getCloudinary().uploader().destroy(publicId, options);
            if ("ok".equals(result.get("result"))) {
                callback.onSuccess();
            } else {
                callback.onFailure("Cloudinary deletion failed: " + result.get("result"));
            }
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public interface DeletionCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
