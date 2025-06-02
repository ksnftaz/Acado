package com.example.aclass;

public class SubmissionModel {
    private String docId;
    private String assignmentId;
    private String studentId;
    private String fileUrl;
    private String publicId;
    private String fileName;
    private long submittedAt;
    private String grade; // Optional

    public SubmissionModel() {}

    public SubmissionModel(String assignmentId, String studentId, String fileUrl, String publicId,
                           String fileName, long submittedAt, String grade) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.fileUrl = fileUrl;
        this.publicId = publicId;
        this.fileName = fileName;
        this.submittedAt = submittedAt;
        this.grade = grade;
    }

    // Getters and Setters
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}