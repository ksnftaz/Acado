package com.example.aclass.cls;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aclass.ClassModel;
import com.example.aclass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class CreateClassActivity extends AppCompatActivity {

    private EditText classNameInput, subjectInput;
    private Button createClassBtn;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        classNameInput = findViewById(R.id.classNameInput);
        subjectInput = findViewById(R.id.subjectInput);
        createClassBtn = findViewById(R.id.createClassBtn);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        teacherId = mAuth.getCurrentUser().getUid();

        createClassBtn.setOnClickListener(v -> {
            String className = classNameInput.getText().toString().trim();
            String subjectInputValue = subjectInput.getText().toString().trim();

            if (TextUtils.isEmpty(className)) {
                classNameInput.setError("Class name required");
                return;
            }

            createClassBtn.setEnabled(false); // disable button to avoid multiple clicks

            String classCode = generateClassCode();
            String classId = UUID.randomUUID().toString();

            db.collection("Users").document(teacherId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String teacherName = documentSnapshot.getString("name");

                            String subject = !TextUtils.isEmpty(subjectInputValue) ? subjectInputValue.trim()
                                    : (documentSnapshot.getString("subject") != null ? documentSnapshot.getString("subject") : "Not Assigned");

                            ClassModel classModel = new ClassModel(
                                    classId,
                                    className,
                                    classCode,
                                    teacherId,
                                    teacherName != null ? teacherName : "Unknown",
                                    subject != null ? subject : "Not Assigned",
                                    null  // or "" if you prefer empty string for fileUrl
                            );


                            db.collection("Classes").document(classId).set(classModel)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Class created with code: " + classCode, Toast.LENGTH_LONG).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        createClassBtn.setEnabled(true);
                                        Toast.makeText(this, "Failed to create class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            createClassBtn.setEnabled(true);
                            Toast.makeText(this, "Teacher profile not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        createClassBtn.setEnabled(true);
                        Toast.makeText(this, "Error fetching teacher info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private String generateClassCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
