package com.example.aclass;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aclass.fragments.FileViewerFragment;
import com.example.aclass.utils.CloudinaryUploadHelper;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UploadAdapter extends RecyclerView.Adapter<UploadAdapter.UploadViewHolder> {
    private List<UploadModel> fileList;
    private Context context;
    private FirebaseFirestore firestore;
    private String classId;

    public UploadAdapter(List<UploadModel> fileList, Context context, FirebaseFirestore firestore, String classId) {
        this.fileList = fileList;
        this.context = context;
        this.firestore = firestore;
        this.classId = classId;
    }

    @NonNull
    @Override
    public UploadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_upload, parent, false);
        return new UploadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UploadViewHolder holder, int position) {
        UploadModel model = fileList.get(position);

        holder.tvFileName.setText(model.getFileName());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.tvUploadedAt.setText("Uploaded at: " + sdf.format(new Date(model.getUploadedAt())));

        // View file
        holder.btnView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                openFile(model.getSecureUrl(), model.getFileName());
            }
        });

        // Delete file
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete File")
                        .setMessage("Are you sure you want to delete this file?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteFileFromFirestoreAndCloud(model, pos))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // Open in fragment if PDF
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                if (model.getSecureUrl().toLowerCase().endsWith(".pdf")) {
                    Fragment fragment = new FileViewerFragment();
                    Bundle args = new Bundle();
                    args.putString("fileUrl", model.getSecureUrl());
                    args.putString("fileName", model.getFileName());
                    fragment.setArguments(args);

                    if (context instanceof FragmentActivity) {
                        ((FragmentActivity) context).getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    }
                } else {
                    openFile(model.getSecureUrl(), model.getFileName());
                }
            }
        });
    }

    private void openFile(String url, String fileName) {
        try {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimeType != null ? mimeType : "*/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // fallback to Google Docs viewer for unsupported formats
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://docs.google.com/viewer?url=" + url));
                webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFileFromFirestoreAndCloud(UploadModel model, int position) {
        firestore.collection("classes")
                .document(classId)
                .collection("uploads")
                .document(model.getDocId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    fileList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "File deleted from Firestore", Toast.LENGTH_SHORT).show();

                    CloudinaryUploadHelper.deleteFromCloudinary(model.getPublicId(), new CloudinaryUploadHelper.DeletionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Deleted from Cloudinary", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(context, "Cloudinary deletion failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete from Firestore", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class UploadViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvUploadedAt;
        Button btnView, btnDelete;

        public UploadViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvUploadedAt = itemView.findViewById(R.id.tvUploadedAt);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
