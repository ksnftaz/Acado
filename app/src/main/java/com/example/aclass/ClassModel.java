package com.example.aclass;

public class ClassModel {
    private String classId;
    private String className;
    private String classCode;
    private String createdBy;
    private String createdByName;
    private String subject;

    private String fileUrl;  // <-- add this field for file URL

    public ClassModel() { }

    // include new field in constructor (optional)
    public ClassModel(String classId, String className, String classCode,
                      String createdBy, String createdByName, String subject,
                      String fileUrl) {
        this.classId = classId;
        this.className = className;
        this.classCode = classCode;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.subject = subject;
        this.fileUrl = fileUrl;
    }

    // Getters and setters
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

}
