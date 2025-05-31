package com.example.aclass.cls;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.aclass.MaterialAdapter;
import com.example.aclass.MaterialModel;
import com.example.aclass.R;
import com.example.aclass.fragments.UploadFragment;
import com.example.aclass.fragments.ViewUploadsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassDetailsActivity extends AppCompatActivity {

    private RecyclerView materialsRecyclerView;
    private FloatingActionButton uploadFileBtn;
    private MaterialButton btnViewUploads;
    private MaterialButton btnUploadFragment; // If you want a button to open UploadFragment

    private List<MaterialModel> materials;
    private MaterialAdapter materialAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String classId, className;
    private boolean isTeacher;

    private final int FILE_PICK_CODE = 1001;

    private TextView classNameText, classCodeText, createdByText;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_details);

        // Initialize views
        materialsRecyclerView = findViewById(R.id.materialsRecyclerView);
        uploadFileBtn = findViewById(R.id.uploadFileBtn);
        btnViewUploads = findViewById(R.id.btnViewUploads);
        btnUploadFragment = findViewById(R.id.btnUploadFragment); // Optional, if you have a button to open UploadFragment

        classNameText = findViewById(R.id.classNameText);
        classCodeText = findViewById(R.id.classCodeText);
        createdByText = findViewById(R.id.createdByText);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        classId = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");
        isTeacher = getIntent().getBooleanExtra("isTeacher", false);

        classNameText.setText(className);
        classCodeText.setText("Code: " + classId);

        db.collection("Classes").document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String creatorEmail = documentSnapshot.getString("createdBy");
                        createdByText.setText("Created by: " + creatorEmail);
                    }
                });

        materials = new ArrayList<>();
        materialAdapter = new MaterialAdapter(this, materials);
        materialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        materialsRecyclerView.setAdapter(materialAdapter);

        if (!isTeacher) uploadFileBtn.setVisibility(View.GONE);

        uploadFileBtn.setOnClickListener(v -> pickFile());

        btnViewUploads.setOnClickListener(v -> {
            // Hide views to show fragment container
            materialsRecyclerView.setVisibility(View.GONE);
            uploadFileBtn.setVisibility(View.GONE);
            btnViewUploads.setVisibility(View.GONE);
            btnUploadFragment.setVisibility(View.GONE);
            classNameText.setVisibility(View.GONE);
            classCodeText.setVisibility(View.GONE);
            createdByText.setVisibility(View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

            // Pass classId to ViewUploadsFragment
            ViewUploadsFragment fragment = new ViewUploadsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("classId", classId);
            fragment.setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Optional: if you want a button to open UploadFragment similarly
        btnUploadFragment.setOnClickListener(v -> {
            materialsRecyclerView.setVisibility(View.GONE);
            uploadFileBtn.setVisibility(View.GONE);
            btnViewUploads.setVisibility(View.GONE);
            btnUploadFragment.setVisibility(View.GONE);
            classNameText.setVisibility(View.GONE);
            classCodeText.setVisibility(View.GONE);
            createdByText.setVisibility(View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

            UploadFragment uploadFragment = new UploadFragment();
            Bundle bundle = new Bundle();
            bundle.putString("classId", classId);
            uploadFragment.setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, uploadFragment)
                    .addToBackStack(null)
                    .commit();
        });

        loadMaterials();
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.fragment_container).getVisibility() == View.VISIBLE) {
            findViewById(R.id.fragment_container).setVisibility(View.GONE);

            materialsRecyclerView.setVisibility(View.VISIBLE);
            uploadFileBtn.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
            btnViewUploads.setVisibility(View.VISIBLE);
            btnUploadFragment.setVisibility(View.VISIBLE);
            classNameText.setVisibility(View.VISIBLE);
            classCodeText.setVisibility(View.VISIBLE);
            createdByText.setVisibility(View.VISIBLE);

            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        }
    }

    private void uploadFile(Uri fileUri) {
        if (fileUri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + getFileName(fileUri);

        MediaManager.get().upload(fileUri)
                .option("public_id", "ClassMaterials/" + classId + "/" + fileName)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");
                        MaterialModel material = new MaterialModel(fileName, fileUrl, classId, mAuth.getCurrentUser().getEmail());
                        db.collection("ClassMaterials").add(material)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(ClassDetailsActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                                    loadMaterials();
                                })
                                .addOnFailureListener(e -> Toast.makeText(ClassDetailsActivity.this, "Failed to save file info", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(ClassDetailsActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void loadMaterials() {
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
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
