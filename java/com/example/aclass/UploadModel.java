package com.example.aclass;

public class UploadModel {
    private String fileName;
    private String fileUrl;
    private String classId;
    private long uploadedAt;
    private String secureUrl;
    private String publicId;
    private String docId;

    public UploadModel() {}

    public UploadModel(String fileName, String fileUrl, String classId, long uploadedAt,
                       String secureUrl, String publicId, String docId) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.classId = classId;
        this.uploadedAt = uploadedAt;
        this.secureUrl = secureUrl;
        this.publicId = publicId;
        this.docId = docId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getClassId() {
        return classId;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getDocId() {
        return docId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setSecureUrl(String secureUrl) {
        this.secureUrl = secureUrl;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
