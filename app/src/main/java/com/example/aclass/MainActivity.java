package com.example.aclass;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aclass.auth.LoginActivity;
import com.example.aclass.cls.CreateClassActivity;
import com.example.aclass.fragments.NotificationsFragment;
import com.example.aclass.fragments.ProfileFragment;
import com.example.aclass.fragments.StudentDashboardFragment;
import com.example.aclass.fragments.TeacherDashboardFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private ImageButton navHomeBtn, navProfileBtn, btnNotifications, createClassTopBtn;
    private TextView topTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean userIsStudent = false;
    private boolean userIsTeacher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }

        // FCM subscribe
        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to topic");
                    } else {
                        Log.w("FCM", "Failed to subscribe", task.getException());
                    }
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM_TOKEN", token);
                        String userId = mAuth.getCurrentUser().getUid();
                        db.collection("Users").document(userId).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        db.collection("Users").document(userId)
                                                .update("fcmToken", token)
                                                .addOnSuccessListener(unused -> Log.d("FCM", "Token saved"))
                                                .addOnFailureListener(e -> Log.e("FCM", "Save failed", e));
                                    } else {
                                        Log.w("FCM", "No user doc");
                                    }
                                });
                    } else {
                        Log.e("FCM", "Token fetch failed", task.getException());
                    }
                });

        setContentView(R.layout.activity_main);
        initViews();
        checkUserRoleAndSetup();
    }

    private void initViews() {
        navHomeBtn = findViewById(R.id.nav_home);
        navProfileBtn = findViewById(R.id.nav_profile);
        btnNotifications = findViewById(R.id.btn_notifications);
        createClassTopBtn = findViewById(R.id.createClassTopBtn);
        topTitle = findViewById(R.id.top_title);

        createClassTopBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateClassActivity.class);
            startActivity(intent);
        });

        navHomeBtn.setOnClickListener(v -> {
            Fragment fragment;
            String title;

            if (userIsStudent) {
                fragment = new StudentDashboardFragment();
                title = "Student Dashboard";
                createClassTopBtn.setVisibility(View.GONE);
            } else if (userIsTeacher) {
                fragment = new TeacherDashboardFragment();
                title = "Teacher Dashboard";
                createClassTopBtn.setVisibility(View.GONE);
            } else {
                fragment = new ProfileFragment();
                title = "Profile";
                createClassTopBtn.setVisibility(View.GONE);
            }

            loadFragment(fragment);
            topTitle.setText(title);
        });

        navProfileBtn.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            topTitle.setText("Profile");
            createClassTopBtn.setVisibility(View.GONE);
        });

        btnNotifications.setOnClickListener(v -> {
            loadFragment(new NotificationsFragment());
            topTitle.setText("Notifications");
            createClassTopBtn.setVisibility(View.GONE);
        });
    }

    private void checkUserRoleAndSetup() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            redirectToLogin();
            return;
        }

        String userId = user.getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");

                    if (role == null) {
                        Toast.makeText(this, "Error: User role not found", Toast.LENGTH_LONG).show();
                        loadFragment(new ProfileFragment());
                        topTitle.setText("Profile");
                        return;
                    }

                    if (ROLE_STUDENT.equalsIgnoreCase(role)) {
                        userIsStudent = true;
                    } else if (ROLE_TEACHER.equalsIgnoreCase(role)) {
                        userIsTeacher = true;
                    } else {
                        Log.w("MainActivity", "Unknown role: " + role);
                    }

                    loadInitialFragment();
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Fetch role failed", e);
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_LONG).show();
                    loadFragment(new ProfileFragment());
                    topTitle.setText("Profile");
                });
    }

    private void loadInitialFragment() {
        Fragment initialFragment;
        String initialTitle;

        if (userIsStudent) {
            initialFragment = new StudentDashboardFragment();
            initialTitle = "Student Dashboard";
            createClassTopBtn.setVisibility(View.GONE);
        } else if (userIsTeacher) {
            initialFragment = new TeacherDashboardFragment();
            initialTitle = "Teacher Dashboard";
            createClassTopBtn.setVisibility(View.VISIBLE);
        } else {
            initialFragment = new ProfileFragment();
            initialTitle = "Profile";
            createClassTopBtn.setVisibility(View.GONE);
        }

        loadFragment(initialFragment);
        topTitle.setText(initialTitle);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void logout() {
        mAuth.signOut();
        redirectToLogin();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
