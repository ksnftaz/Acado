package com.example.aclass;

public class Material {
    private String fileName;
    private String fileType;
    private String downloadUrl;
    private String uploadedBy;

    // Required empty constructor for Firebase
    public Material() {}

    public Material(String fileName, String fileType, String downloadUrl, String uploadedBy) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.downloadUrl = downloadUrl;
        this.uploadedBy = uploadedBy;
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getUploadedBy() { return uploadedBy; }

    // Setters (required for Firebase)
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
