package com.example.aclass;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aclass.auth.LoginActivity;
import com.example.aclass.fragments.NotificationsFragment;
import com.example.aclass.fragments.ProfileFragment;
import com.example.aclass.fragments.StudentDashboardFragment;
import com.example.aclass.fragments.TeacherDashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private Toolbar toolbar;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean userIsStudent = false;
    private boolean userIsTeacher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, NOTIFICATION_PERMISSION_CODE);
            }
        }

        // Subscribe to FCM topic
        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to topic");
                    } else {
                        Log.w("FCM", "Failed to subscribe to topic", task.getException());
                    }
                });

        // Get and log FCM token, save to Firestore
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
                                                .addOnFailureListener(e -> Log.e("FCM", "Failed to save token", e));
                                    } else {
                                        Log.w("FCM", "User document missing, cannot save token");
                                    }
                                });
                    } else {
                        Log.e("FCM", "Failed to get token", task.getException());
                    }
                });

        setContentView(R.layout.activity_main);
        initViews();
        setupToolbar();
        checkUserRoleAndSetup();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("AClass");
        }
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
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if (role == null) {
                            Log.w("MainActivity", "Role field is null");
                            Toast.makeText(this, "Error: User role not found", Toast.LENGTH_LONG).show();
                            loadFragment(new ProfileFragment());
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("Profile");
                            }
                            return;
                        }
                        if (ROLE_STUDENT.equalsIgnoreCase(role)) {
                            userIsStudent = true;
                        } else if (ROLE_TEACHER.equalsIgnoreCase(role)) {
                            userIsTeacher = true;
                        } else {
                            Log.w("MainActivity", "Unknown user role: " + role);
                        }
                    } else {
                        Log.w("MainActivity", "User document does not exist!");
                        Toast.makeText(this, "Error: User data not found", Toast.LENGTH_LONG).show();
                        loadFragment(new ProfileFragment());
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Profile");
                        }
                        return;
                    }
                    setupBottomNavigation();
                    loadInitialFragment();
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Failed to fetch user role", e);
                    Toast.makeText(this, "Error loading user data. Please try again.", Toast.LENGTH_LONG).show();
                    loadFragment(new ProfileFragment());
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Profile");
                    }
                });
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                if (userIsStudent) {
                    selectedFragment = new StudentDashboardFragment();
                    title = "Student Dashboard";
                } else if (userIsTeacher) {
                    selectedFragment = new TeacherDashboardFragment();
                    title = "Teacher Dashboard";
                } else {
                    selectedFragment = new ProfileFragment();
                    title = "Unknown Role - Profile";
                }
            }
//            } else if (id == R.id.navigation_notifications) {
//                selectedFragment = new NotificationsFragment();
//                title = "Notifications";
//            }
            else if (id == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
                title = "Profile";
            }

            if (selectedFragment != null) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadInitialFragment() {
        String initialTitle = "";
        Fragment initialFragment;
        if (userIsStudent) {
            initialFragment = new StudentDashboardFragment();
            initialTitle = "Student Dashboard";
        } else if (userIsTeacher) {
            initialFragment = new TeacherDashboardFragment();
            initialTitle = "Teacher Dashboard";
        } else {
            initialFragment = new ProfileFragment();
            initialTitle = "Profile";
        }
        loadFragment(initialFragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(initialTitle);
        }
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

    private void logoutAndRedirect() {
        mAuth.signOut();
        redirectToLogin();
    }

    public void logout() {
        logoutAndRedirect();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy");
    }
}