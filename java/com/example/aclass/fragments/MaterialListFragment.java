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

import com.example.aclass.MaterialAdapter;
import com.example.aclass.MaterialModel;
import com.example.aclass.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MaterialListFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaterialAdapter adapter;
    private List<MaterialModel> materialList;
    private DatabaseReference materialsRef;
    private String classCode = "class123"; // TODO: Replace with actual dynamic class code

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_material_list, container, false);

        recyclerView = view.findViewById(R.id.materialRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        materialList = new ArrayList<>();
        adapter = new MaterialAdapter(getContext(), materialList);
        recyclerView.setAdapter(adapter);

        materialsRef = FirebaseDatabase.getInstance()
                .getReference("Classes")
                .child(classCode)
                .child("materials");

        loadMaterials();

        return view;
    }

    private void loadMaterials() {
        materialsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                materialList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    MaterialModel material = data.getValue(MaterialModel.class);
                    if (material != null) {
                        materialList.add(material);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load materials", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
