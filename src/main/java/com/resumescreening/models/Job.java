package com.resumescreening.models;

import java.time.LocalDateTime;

/**
 * Model representing a job opening created by a recruiter.
 */
public class Job {
    private int id;
    private int recruiterId;
    private String title;
    private String department;
    private String description;
    private String requiredSkills;
    private String preferredSkills;
    private int minExperienceYears;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Job() {
        this.status = "OPEN";
    }

    public Job(int id, int recruiterId, String title, String department, String description,
               String requiredSkills, int minExperienceYears, String status,
               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.recruiterId = recruiterId;
        this.title = title;
        this.department = department;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.minExperienceYears = minExperienceYears;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(String preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public Job(int recruiterId, String title, String department, String description,
               String requiredSkills, int minExperienceYears) {
        this.recruiterId = recruiterId;
        this.title = title;
        this.department = department;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.minExperienceYears = minExperienceYears;
        this.status = "OPEN";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public int getMinExperienceYears() {
        return minExperienceYears;
    }

    public void setMinExperienceYears(int minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", recruiterId=" + recruiterId +
                ", title='" + title + '\'' +
                ", department='" + department + '\'' +
                ", minExperienceYears=" + minExperienceYears +
                ", status='" + status + '\'' +
                '}';
    }
}
