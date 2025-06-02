package com.example.aclass;

public class AssignmentModel {
    private String docId;
    private String title;
    private String dueDate;
    private String points;
    private String status;
    private String classId;
    private String fileUrl; // Teacher-uploaded file
    private String publicId; // Cloudinary public ID
    private String fileName; // File name
    private String uploadedBy; // Teacher email or ID

    // Default constructor for Firestore
    public AssignmentModel() {}

    public AssignmentModel(String title, String dueDate, String points, String status, String classId,
                           String fileUrl, String publicId, String fileName, String uploadedBy) {
        this.title = title;
        this.dueDate = dueDate;
        this.points = points;
        this.status = status;
        this.classId = classId;
        this.fileUrl = fileUrl;
        this.publicId = publicId;
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getPoints() { return points; }
    public void setPoints(String points) { this.points = points; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}