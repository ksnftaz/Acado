package com.example.aclass.cls;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.aclass.MaterialAdapter;
import com.example.aclass.MaterialModel;
import com.example.aclass.R;
import com.example.aclass.fragments.AssignmentFragment;
import com.example.aclass.fragments.UploadFragment;
import com.example.aclass.fragments.ViewUploadsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassDetailsActivity extends AppCompatActivity {

    private RecyclerView materialsRecyclerView;
    private FloatingActionButton uploadFileBtn;
    private MaterialButton btnViewUploads, btnUploadFragment;
    private TextView classNameText, classCodeText, createdByText;
    private TabLayout tabLayout;
    private View fragmentContainer;

    private List<MaterialModel> materials;
    private MaterialAdapter materialAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String classId;
    private boolean isTeacher;
    private String currentTab = "assignments"; // Default to assignments
    private final int FILE_PICK_CODE = 1001;
    private boolean isFragmentVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_details);

        initializeViews();
        initializeFirebase();
        setupIntentData();
        setupRecyclerView();
        setupClickListeners();
        setupTabs();
        loadInitialData();
    }

    private void initializeViews() {
        materialsRecyclerView = findViewById(R.id.materialsRecyclerView);
        uploadFileBtn = findViewById(R.id.uploadFileBtn);
        btnViewUploads = findViewById(R.id.btnViewUploads);
        btnUploadFragment = findViewById(R.id.btnUploadFragment);
        classNameText = findViewById(R.id.courseTitleTextView);
        classCodeText = findViewById(R.id.courseCodeTextView);
        createdByText = findViewById(R.id.createdByTextView);
        tabLayout = findViewById(R.id.tabLayout);
        fragmentContainer = findViewById(R.id.fragment_container);

        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        ImageButton backArrowButton = findViewById(R.id.backArrowButton);
        if (backArrowButton != null) {
            backArrowButton.setOnClickListener(v -> onBackPressed());
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupIntentData() {
        Intent intent = getIntent();
        classId = intent != null ? intent.getStringExtra("classId") : null;

        // Initialize UI with placeholders
        classNameText.setText("Loading...");
        classCodeText.setText("Code: Loading...");
        createdByText.setText("Created by: Loading...");
        Log.d("ClassDetails", "classId from Intent: " + classId);
    }

    private void setupRecyclerView() {
        materials = new ArrayList<>();
        materialAdapter = new MaterialAdapter(this, materials);
        materialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        materialsRecyclerView.setAdapter(materialAdapter);
    }

    private void setupClickListeners() {
        uploadFileBtn.setOnClickListener(v -> {
            if (currentTab.equals("materials")) {
                pickFile();
            } else {
                Toast.makeText(this, "Switch to Materials tab to upload files", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewUploads.setOnClickListener(v -> {
            ViewUploadsFragment fragment = new ViewUploadsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("classId", classId);
            bundle.putBoolean("isTeacher", isTeacher);
            fragment.setArguments(bundle);
            showFragment(fragment, "ViewUploads");
        });

        btnUploadFragment.setOnClickListener(v -> {
            if (isTeacher) {
                UploadFragment uploadFragment = new UploadFragment();
                Bundle bundle = new Bundle();
                bundle.putString("classId", classId);
                bundle.putBoolean("isTeacher", isTeacher);
                uploadFragment.setArguments(bundle);
                showFragment(uploadFragment, "Upload");
            } else {
                Toast.makeText(this, "Only teachers can upload files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadInitialData() {
        if (classId != null && !classId.trim().isEmpty()) {
            loadClassInfo();
            tabLayout.selectTab(tabLayout.getTabAt(1)); // Start with Assignments
        } else {
            classNameText.setText("Unknown Class");
            classCodeText.setText("Code: Invalid");
            createdByText.setText("Created by: N/A");
            isTeacher = false;
            updateTeacherViews();
            Log.e("ClassDetails", "Invalid or null classId: " + classId);
        }
    }

    private void loadClassInfo() {
        db.collection("Classes").document(classId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String className = documentSnapshot.getString("className");
                        String classCode = documentSnapshot.getString("classCode");
                        String creatorUid = documentSnapshot.getString("createdBy");

                        classNameText.setText(className != null && !className.trim().isEmpty() ? className : "Unknown Class");
                        classCodeText.setText(classCode != null && !classCode.trim().isEmpty() ? "Code: " + classCode : "Code: N/A");
                        Log.d("ClassDetails", "className: " + className + ", classCode: " + classCode);

                        isTeacher = creatorUid != null && mAuth.getCurrentUser() != null && creatorUid.equals(mAuth.getCurrentUser().getUid());

                        if (creatorUid != null && !creatorUid.trim().isEmpty()) {
                            db.collection("Users").document(creatorUid).get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String displayName = userSnapshot.exists() ? userSnapshot.getString("displayName") : null;
                                        createdByText.setText(displayName != null && !displayName.trim().isEmpty()
                                                ? "Created by: " + displayName
                                                : "Created by: Unknown");
                                        Log.d("ClassDetails", "creatorUid: " + creatorUid + ", displayName: " + displayName);
                                        updateTeacherViews();
                                    })
                                    .addOnFailureListener(e -> {
                                        createdByText.setText("Created by: Error");
                                        Log.e("ClassDetails", "Failed to load user info: " + e.getMessage());
                                        updateTeacherViews();
                                    });
                        } else {
                            createdByText.setText("Created by: Unknown");
                            isTeacher = false;
                            Log.e("ClassDetails", "Invalid or null creatorUid");
                            updateTeacherViews();
                        }
                    } else {
                        classNameText.setText("Unknown Class");
                        classCodeText.setText("Code: N/A");
                        createdByText.setText("Created by: Not Found");
                        isTeacher = false;
                        Log.e("ClassDetails", "Class document not found for classId: " + classId);
                        updateTeacherViews();
                    }
                })
                .addOnFailureListener(e -> {
                    classNameText.setText("Unknown Class");
                    classCodeText.setText("Code: Error");
                    createdByText.setText("Created by: Error");
                    isTeacher = false;
                    Toast.makeText(this, "Failed to load class info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ClassDetails", "Firestore error: " + e.getMessage());
                    updateTeacherViews();
                });
    }

    private void updateTeacherViews() {
        uploadFileBtn.setVisibility(isTeacher && currentTab.equals("materials") ? View.VISIBLE : View.GONE);
        btnUploadFragment.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
        Log.d("ClassDetails", "isTeacher: " + isTeacher + ", FAB visibility: " + (uploadFileBtn.getVisibility() == View.VISIBLE ? "Visible" : "Gone") +
                ", Upload Button visibility: " + (btnUploadFragment.getVisibility() == View.VISIBLE ? "Visible" : "Gone"));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Materials"));
        tabLayout.addTab(tabLayout.newTab().setText("Assignments"));
        tabLayout.addTab(tabLayout.newTab().setText("Announcements"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isFragmentVisible) {
                    hideFragment();
                }
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });
    }

    private void switchTab(int position) {
        switch (position) {
            case 0:
                currentTab = "materials";
                showMainContent();
                loadMaterials();
                break;
            case 1:
                currentTab = "assignments";
                showAssignmentsFragment();
                break;
            case 2:
                currentTab = "announcements";
                loadAnnouncements();
                break;
        }
        updateTeacherViews();
        Log.d("ClassDetails", "Tab selected: " + currentTab);
    }

    private void showAssignmentsFragment() {
        AssignmentFragment fragment = new AssignmentFragment();
        Bundle bundle = new Bundle();
        bundle.putString("classId", classId);
        bundle.putBoolean("isTeacher", isTeacher);
        fragment.setArguments(bundle);
        showFragment(fragment, "Assignments");
    }

    private void showFragment(Fragment fragment, String tag) {
        hideMainContent();
        fragmentContainer.setVisibility(View.VISIBLE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();

        isFragmentVisible = true;
    }

    private void hideFragment() {
        if (!isFragmentVisible) return;

        fragmentContainer.setVisibility(View.GONE);
        getSupportFragmentManager().popBackStack();
        isFragmentVisible = false;
    }

    private void showMainContent() {
        findViewById(R.id.courseHeaderLayout).setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
        findViewById(R.id.materialsCard).setVisibility(View.VISIBLE);
        updateTeacherViews();
    }

    private void hideMainContent() {
        findViewById(R.id.courseHeaderLayout).setVisibility(View.GONE);
        tabLayout.setVisibility(View.VISIBLE);
        findViewById(R.id.materialsCard).setVisibility(View.GONE);
        updateTeacherViews();
    }

    @Override
    public void onBackPressed() {
        if (isFragmentVisible) {
            hideFragment();
            showMainContent();
            switch (currentTab) {
                case "materials":
                    loadMaterials();
                    break;
                case "announcements":
                    loadAnnouncements();
                    break;
                case "assignments":
                    showAssignmentsFragment();
                    break;
            }
        } else {
            super.onBackPressed();
        }
    }

    private void loadMaterials() {
        if (classId == null || classId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid class ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("ClassMaterials")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    materials.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        MaterialModel material = doc.toObject(MaterialModel.class);
                        if (material != null) {
                            material.setDocId(doc.getId());
                            materials.add(material);
                        }
                    }
                    materialAdapter.notifyDataSetChanged();
//                    if (materials.isEmpty()) {
//                        Toast.makeText(this, "No materials found", Toast.LENGTH_SHORT).show();
//                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load materials: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadAnnouncements() {
        if (classId == null || classId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid class ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Announcements")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No announcements found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Found " + queryDocumentSnapshots.size() + " announcements", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load announcements: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/msword", "application/vnd.ms-powerpoint"});
        try {
            startActivityForResult(intent, FILE_PICK_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ClassDetails", "File picker error: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        }
    }
//fab button
    private void uploadFile(Uri fileUri) {
        if (fileUri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = System.currentTimeMillis() + "_" + getFileName(fileUri);
        String uploaderEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "unknown";

        MediaManager.get().upload(fileUri)
                .option("public_id", "ClassMaterials/" + classId + "/" + fileName)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) (100 * bytes / totalBytes);
                        progressDialog.setMessage("Uploading... " + progress + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        progressDialog.dismiss();
                        String fileUrl = (String) resultData.get("secure_url");
                        if (fileUrl == null) {
                            Toast.makeText(ClassDetailsActivity.this, "Upload failed: No URL returned", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        MaterialModel material = new MaterialModel(fileName, fileUrl, classId, uploaderEmail);
                        db.collection("ClassMaterials").add(material)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(ClassDetailsActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                                    if (currentTab.equals("materials")) {
                                        loadMaterials();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(ClassDetailsActivity.this, "Failed to save file info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(ClassDetailsActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null || result.trim().isEmpty()) {
            result = "file_" + System.currentTimeMillis();
        }
        return result;
    }
}