package com.example.aclass.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.R;
import com.example.aclass.UploadAdapter;
import com.example.aclass.UploadModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ViewUploadsFragment extends Fragment {
    private RecyclerView recyclerView;
    private UploadAdapter uploadAdapter;
    private List<UploadModel> uploadList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String classId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_uploads, container, false);

        if (getArguments() != null) {
            classId = getArguments().getString("classId");
        }

        // Get classId from arguments
        classId = getArguments() != null ? getArguments().getString("classId") : null;

        if (classId == null) {
            Toast.makeText(getContext(), "Class ID is missing", Toast.LENGTH_SHORT).show();
            return view;
        }



        recyclerView = view.findViewById(R.id.recyclerViewUploads);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadUploads();

        return view;
    }

    private void loadUploads() {
        firestore.collection("classes")
                .document(classId)
                .collection("uploads")
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    uploadList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UploadModel model = doc.toObject(UploadModel.class);
                        if (model != null) {
                            model.setDocId(doc.getId());
                            uploadList.add(model);
                        }
                    }
                    uploadAdapter = new UploadAdapter(uploadList, getContext(), firestore, classId);
                    recyclerView.setAdapter(uploadAdapter);
                });
    }
}
