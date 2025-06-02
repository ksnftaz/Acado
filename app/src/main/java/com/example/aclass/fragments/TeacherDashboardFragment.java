package com.example.aclass.fragments;

import android.content.Intent;  // <-- Import Intent
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.ClassAdapter;
import com.example.aclass.ClassModel;
import com.example.aclass.R;
import com.example.aclass.cls.CreateClassActivity; // <-- Import your CreateClassActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardFragment extends Fragment {

    private RecyclerView myClassesRecyclerView;
    private FloatingActionButton createClassBtn;
    private List<ClassModel> myClasses;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String teacherId;

    public TeacherDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);

        myClassesRecyclerView = view.findViewById(R.id.myClassesRecyclerView);
        createClassBtn = view.findViewById(R.id.createClassBtn);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        teacherId = mAuth.getCurrentUser().getUid();

        myClasses = new ArrayList<>();
        classAdapter = new ClassAdapter(myClasses, getContext());

        myClassesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myClassesRecyclerView.setAdapter(classAdapter);

        loadMyClasses();

        createClassBtn.setOnClickListener(v -> {
            // Launch CreateClassActivity when the FAB is clicked
            Intent intent = new Intent(getContext(), CreateClassActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadMyClasses() {
        db.collection("Classes")
                .whereEqualTo("createdBy", teacherId) // <-- Make sure field name matches your model
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    myClasses.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClassModel model = doc.toObject(ClassModel.class);
                        if (model != null) {
                            myClasses.add(model);
                        }
                    }
                    classAdapter.notifyDataSetChanged();
                });
    }
}