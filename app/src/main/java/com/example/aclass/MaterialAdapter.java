package com.example.aclass;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.fragments.FileViewerFragment;

import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    private Context context;
    private List<MaterialModel> materialList;

    public MaterialAdapter(Context context, List<MaterialModel> materials) {
        this.context = context;
        this.materialList = materials;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_material, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        MaterialModel material = materialList.get(position);
        holder.fileNameText.setText(material.getFileName());
        holder.uploadedByText.setText("Uploaded by: " + material.getUploadedBy());

        // Download button click
        holder.downloadBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(material.getFileUrl()));
            context.startActivity(intent);
        });

        // View in fragment on entire item click
        holder.itemView.setOnClickListener(v -> {
            String fileUrl = material.getFileUrl(); // Use material (not model)

            FileViewerFragment viewerFragment = FileViewerFragment.newInstance(fileUrl);

            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, viewerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }


    @Override
    public int getItemCount() {
        return materialList.size();
    }

    public static class MaterialViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, uploadedByText;
        Button downloadBtn;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            uploadedByText = itemView.findViewById(R.id.uploadedByText);
            downloadBtn = itemView.findViewById(R.id.downloadBtn);
        }
    }
}
