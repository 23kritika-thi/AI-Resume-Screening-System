package com.resumescreening.models;

import java.time.LocalDateTime;

/**
 * Model representing a job candidate.
 */
public class Candidate {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private LocalDateTime createdAt;

    public Candidate() {
    }

    public Candidate(int id, String fullName, String email, String phone, String linkedinUrl, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.linkedinUrl = linkedinUrl;
        this.createdAt = createdAt;
    }

    public Candidate(String fullName, String email, String phone, String linkedinUrl) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.linkedinUrl = linkedinUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
