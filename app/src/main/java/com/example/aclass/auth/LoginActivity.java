package com.example.aclass.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aclass.MainActivity;
import com.example.aclass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, registerRedirect;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerRedirect = findViewById(R.id.registerRedirect);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(v -> loginUser());

        registerRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(LoginActivity.this, "Login failed: user not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = mAuth.getCurrentUser().getUid();

                // Fetch user role from Firestore
                db.collection("Users").document(uid).get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        if (role == null) {
                            Toast.makeText(LoginActivity.this, "User role not defined", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Redirect based on user role
                        if (role.equalsIgnoreCase("teacher")) {
                            startDashboardActivity("teacher");
                        } else if (role.equalsIgnoreCase("student")) {
                            startDashboardActivity("student");
                        } else {
                            Toast.makeText(LoginActivity.this, "Unknown user role", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

            } else {
                Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startDashboardActivity(String role) {
        Intent intent;

        // If you have separate dashboards, set them here, else use MainActivity
        if (role.equalsIgnoreCase("teacher")) {
            // Example: intent = new Intent(this, TeacherDashboardActivity.class);
            intent = new Intent(this, MainActivity.class); // if MainActivity handles role-based UI
        } else {
            // Example: intent = new Intent(this, StudentDashboardActivity.class);
            intent = new Intent(this, MainActivity.class); // if MainActivity handles role-based UI
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
