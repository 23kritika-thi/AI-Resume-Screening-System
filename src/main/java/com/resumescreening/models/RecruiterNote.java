package com.resumescreening.models;

import java.time.LocalDateTime;

/**
 * Model representing recruiter qualitative notes on a screening result.
 */
public class RecruiterNote {
    private int id;
    private int screeningId;
    private int recruiterId;
    private String noteText;
    private LocalDateTime createdAt;

    public RecruiterNote() {
    }

    public RecruiterNote(int id, int screeningId, int recruiterId, String noteText, LocalDateTime createdAt) {
        this.id = id;
        this.screeningId = screeningId;
        this.recruiterId = recruiterId;
        this.noteText = noteText;
        this.createdAt = createdAt;
    }

    public RecruiterNote(int screeningId, int recruiterId, String noteText) {
        this.screeningId = screeningId;
        this.recruiterId = recruiterId;
        this.noteText = noteText;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScreeningId() {
        return screeningId;
    }

    public void setScreeningId(int screeningId) {
        this.screeningId = screeningId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RecruiterNote{" +
                "id=" + id +
                ", screeningId=" + screeningId +
                ", recruiterId=" + recruiterId +
                '}';
    }
}
