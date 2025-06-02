package com.example.aclass.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.aclass.R;
import com.github.barteksc.pdfviewer.PDFView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileViewerFragment extends Fragment {

    private PDFView pdfView;
    private ProgressBar progressBar;
    private String fileUrl, fileName, fileType;
    private File downloadedFile;

    public static FileViewerFragment newInstance(String fileUrl) {
        FileViewerFragment fragment = new FileViewerFragment();
        Bundle args = new Bundle();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String fileType = MimeTypeMap.getFileExtensionFromUrl(fileUrl).toLowerCase();

        args.putString("fileUrl", fileUrl);
        args.putString("fileName", fileName);
        args.putString("fileType", fileType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_viewer, container, false);

        pdfView = view.findViewById(R.id.pdfView);
        progressBar = view.findViewById(R.id.progressBar);

        if (getArguments() != null) {
            fileUrl = getArguments().getString("fileUrl");
            fileName = getArguments().getString("fileName");
            fileType = getArguments().getString("fileType");
        }

        downloadAndOpenFile();

        return view;
    }

    private void downloadAndOpenFile() {
        progressBar.setVisibility(View.VISIBLE);

        File directory = new File(requireContext().getCacheDir(), "downloads");
        if (!directory.exists()) directory.mkdirs();

        downloadedFile = new File(directory, fileName);

        if (downloadedFile.exists()) {
            openFile();
        } else {
            new Thread(() -> {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(downloadedFile);

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }

                    output.flush();
                    output.close();
                    input.close();

                    requireActivity().runOnUiThread(this::openFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to download file.", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }

    private void openFile() {
        progressBar.setVisibility(View.GONE);

        if (fileType.equals("pdf")) {
            pdfView.setVisibility(View.VISIBLE);
            pdfView.fromFile(downloadedFile)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", downloadedFile);

            switch (fileType) {
                case "pptx":
                    intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
                    break;
                case "docx":
                    intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    break;
                case "xlsx":
                    intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    break;
                default:
                    intent.setDataAndType(uri, "*/*");
                    break;
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No app found to open this file type.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
