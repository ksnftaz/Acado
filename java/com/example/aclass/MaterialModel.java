package com.example.aclass;

public class MaterialModel {
    private String fileName;
    private String fileUrl;
    private String classId;
    private String uploadedBy;
    private String docId;   // Optional Firestore doc ID, if used

    public MaterialModel() { }

    public MaterialModel(String fileName, String fileUrl, String classId, String uploadedBy) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.classId = classId;
        this.uploadedBy = uploadedBy;
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public String getClassId() { return classId; }
    public String getUploadedBy() { return uploadedBy; }
    public String getDocId() { return docId; }

    // Setters
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setClassId(String classId) { this.classId = classId; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setDocId(String docId) { this.docId = docId; }
}
