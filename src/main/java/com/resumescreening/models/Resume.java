package com.resumescreening.models;

import java.time.LocalDateTime;

/**
 * Model representing a candidate resume document and its extracted contents.
 */
public class Resume {
    private int id;
    private int candidateId;
    private String fileName;
    private String filePath;
    private String extractedText;
    private String fileHash;
    private LocalDateTime uploadedAt;

    public Resume() {
    }

    public Resume(int id, int candidateId, String fileName, String filePath,
                  String extractedText, String fileHash, LocalDateTime uploadedAt) {
        this.id = id;
        this.candidateId = candidateId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.extractedText = extractedText;
        this.fileHash = fileHash;
        this.uploadedAt = uploadedAt;
    }

    public Resume(int candidateId, String fileName, String filePath, String extractedText, String fileHash) {
        this.candidateId = candidateId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.extractedText = extractedText;
        this.fileHash = fileHash;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "Resume{" +
                "id=" + id +
                ", candidateId=" + candidateId +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
