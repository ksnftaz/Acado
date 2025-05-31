package com.example.aclass.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.NotificationAdapter;
import com.example.aclass.NotificationModel;
import com.example.aclass.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StudentNotificationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StudentNotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> allNotifications = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference userRef, notifRef;

    public StudentNotificationsFragment() {}

    public static StudentNotificationsFragment newInstance() {
        return new StudentNotificationsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_notifications, container, false);

        recyclerView = view.findViewById(R.id.recyclerStudentNotifs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(allNotifications);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("JoinedClasses");
            notifRef = FirebaseDatabase.getInstance().getReference("Notifications");

            loadNotifications();
        } else {
            Toast.makeText(getContext(), "Please log in to view notifications", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    // REMOVED the sendNotification() method - it doesn't belong in a student fragment
    // Students should only VIEW notifications, not SEND them

    private void loadNotifications() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot classSnap : snapshot.getChildren()) {
                    String classId = classSnap.getKey();

                    notifRef.child(classId).addListenerForSingleValueEvent(new ValueEventListener() {
                        public void onDataChange(@NonNull DataSnapshot notiSnap) {
                            for (DataSnapshot n : notiSnap.getChildren()) {
                                NotificationModel model = n.getValue(NotificationModel.class);
                                allNotifications.add(model);
                            }
                            Collections.sort(allNotifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                            adapter.notifyDataSetChanged();
                        }

                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up any listeners if needed
    }
}