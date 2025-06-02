package com.example.aclass.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.aclass.ClassAdapter;
import com.example.aclass.ClassModel;
import com.example.aclass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassAdapter classAdapter;
    private List<ClassModel> classList;
    private DatabaseReference classesRef;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewClasses);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        classList = new ArrayList<>();
        classAdapter = new ClassAdapter(classList, getContext());
        recyclerView.setAdapter(classAdapter);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in. Please login first.", Toast.LENGTH_SHORT).show();
            // Optionally navigate back or to login screen here
            return view;
        }

        currentUserId = mAuth.getCurrentUser().getUid();
        classesRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(currentUserId).child("EnrolledClasses");

        loadClasses();

        return view;
    }

    private void loadClasses() {
        classesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ClassModel model = data.getValue(ClassModel.class);
                    classList.add(model);
                }
                classAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading classes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
