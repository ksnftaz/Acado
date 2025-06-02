package com.example.aclass;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameText, emailText;
    private Button logoutBtn;
    private RecyclerView classListRecycler;
    private List<ClassModel> classList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private ClassListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupFirebase();
        setupRecyclerView();
        setupListeners();

        loadUserInfo();
        loadClasses();
    }

    private void initViews() {
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        logoutBtn = findViewById(R.id.logoutBtn);
        classListRecycler = findViewById(R.id.classListRecycler);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        } else {
            // User not logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupRecyclerView() {
        classListRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassListAdapter(this, classList, false); // 👈 assuming this screen is for students// Fixed constructor order: Context first
        classListRecycler.setAdapter(adapter);
    }

    private void setupListeners() {
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        db.collection("Users").document(currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("name");
                        String email = snapshot.getString("email");

                        nameText.setText(name != null ? name : "No name");
                        emailText.setText(email != null ? email : "No email");
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void loadClasses() {
        db.collection("Users").document(currentUid)
                .collection("EnrolledClasses")
                .get()
                .addOnSuccessListener(query -> {
                    classList.clear();
                    for (DocumentSnapshot doc : query) {
                        ClassModel classModel = doc.toObject(ClassModel.class);
                        if (classModel != null) {
                            classList.add(classModel);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }
}
