package com.example.aclass.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.aclass.R;
import com.example.aclass.ClassAdapter;
import com.example.aclass.ClassModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FilesListFragment extends Fragment {

    private static final String TAG = "FilesListFragment";

    private RecyclerView recyclerView;
    private ClassAdapter adapter;
    private List<ClassModel> filesList;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private SwipeRefreshLayout swipeRefresh;

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_files_list, container, false);

        initViews(view);
        initFirebase();
        setupRecyclerView();
        setupSwipeRefresh();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFiles();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        filesList = new ArrayList<>();
    }

    private void initFirebase() {
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupRecyclerView() {
        adapter = new ClassAdapter(filesList, this::openFile);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadFiles);
            swipeRefresh.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }
    }

    private void loadFiles() {
        Log.d(TAG, "Loading files...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            showError("User not authenticated. Please login again.");
            hideLoading();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading files for user: " + userId);

        showLoading();

        firestore.collection("uploads")
                .whereEqualTo("uploadedBy", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Files loaded successfully. Count: " + queryDocumentSnapshots.size());

                    filesList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                ClassModel file = document.toObject(ClassModel.class);
                                file.setClassId(document.getId());

                                if (isValidFile(file)) {
                                    filesList.add(file);
                                } else {
                                    Log.w(TAG, "Invalid file data for document: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document: " + document.getId(), e);
                            }
                        }

                        if (filesList.isEmpty()) {
                            showEmptyState();
                        } else {
                            showFilesList();
                        }
                    }

                    adapter.notifyDataSetChanged();
                    hideLoading();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading files", e);
                    showError("Error loading files: " + e.getMessage());
                    hideLoading();
                });
    }

    private boolean isValidFile(ClassModel file) {
        return file != null &&
                file.getFileUrl() != null &&
                !file.getFileUrl().trim().isEmpty();
    }

    private void openFile(ClassModel file) {
        if (file == null || file.getFileUrl() == null || file.getFileUrl().trim().isEmpty()) {
            Toast.makeText(getContext(), "Invalid file URL", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(TAG, "Opening file: " + file.getFileUrl());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = Uri.parse(file.getFileUrl());
            intent.setDataAndType(fileUri, getMimeType(file.getFileUrl()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, fileUri);
                startActivity(browserIntent);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening file", e);
            Toast.makeText(getContext(), "Unable to open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return "*/*";
    }

    private void showLoading() {
        boolean isRefreshing = swipeRefresh != null && swipeRefresh.isRefreshing();

        if (!isRefreshing && progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    private void showFilesList() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("No files uploaded yet");
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Optional: Uncomment this if you want to always reload
        // loadFiles();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        adapter = null;
        progressBar = null;
        emptyStateText = null;
        swipeRefresh = null;
    }
}
