package com.example.aclass;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ClassRosterActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RosterAdapter adapter;
    private List<UserModel> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_roster);

        initViews();
        setupRecyclerView();

        db = FirebaseFirestore.getInstance();
        classId = getIntent().getStringExtra("classId");

        if (classId != null) {
            loadStudents();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rosterRecyclerView);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RosterAdapter(studentList, this, classId);
        recyclerView.setAdapter(adapter);
    }

    private void loadStudents() {
        db.collection("Classes").document(classId)
                .collection("EnrolledUsers")
                .get()
                .addOnSuccessListener(query -> {
                    studentList.clear();
                    for (DocumentSnapshot doc : query) {
                        UserModel student = doc.toObject(UserModel.class);
                        if (student != null) {
                            studentList.add(student);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Handle error - maybe show a toast or log
                    e.printStackTrace();
                });
    }
}