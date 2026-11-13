package com.resumescreening.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model representing the AI screening and ranking outcome for a candidate against a job.
 */
public class ScreeningResult {
    private int id;
    private int jobId;
    private int candidateId;
    private int resumeId;
    private BigDecimal matchScore;
    private BigDecimal requiredSkillsScore;
    private BigDecimal preferredSkillsScore;
    private BigDecimal experienceScore;
    private BigDecimal textSimilarityScore;
    private String matchedSkills;
    private String missingSkills;
    private Integer ranking;
    private String recommendation;
    private LocalDateTime screenedAt;

    public ScreeningResult() {
    }

    public ScreeningResult(int id, int jobId, int candidateId, int resumeId, BigDecimal matchScore,
                           String matchedSkills, String missingSkills, Integer ranking,
                           String recommendation, LocalDateTime screenedAt) {
        this.id = id;
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.resumeId = resumeId;
        this.matchScore = matchScore;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.ranking = ranking;
        this.recommendation = recommendation;
        this.screenedAt = screenedAt;
    }

    public ScreeningResult(int jobId, int candidateId, int resumeId, BigDecimal matchScore,
                           String matchedSkills, String missingSkills, Integer ranking,
                           String recommendation) {
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.resumeId = resumeId;
        this.matchScore = matchScore;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.ranking = ranking;
        this.recommendation = recommendation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getResumeId() {
        return resumeId;
    }

    public void setResumeId(int resumeId) {
        this.resumeId = resumeId;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public BigDecimal getRequiredSkillsScore() {
        return requiredSkillsScore;
    }

    public void setRequiredSkillsScore(BigDecimal requiredSkillsScore) {
        this.requiredSkillsScore = requiredSkillsScore;
    }

    public BigDecimal getPreferredSkillsScore() {
        return preferredSkillsScore;
    }

    public void setPreferredSkillsScore(BigDecimal preferredSkillsScore) {
        this.preferredSkillsScore = preferredSkillsScore;
    }

    public BigDecimal getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(BigDecimal experienceScore) {
        this.experienceScore = experienceScore;
    }

    public BigDecimal getTextSimilarityScore() {
        return textSimilarityScore;
    }

    public void setTextSimilarityScore(BigDecimal textSimilarityScore) {
        this.textSimilarityScore = textSimilarityScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getScreenedAt() {
        return screenedAt;
    }

    public void setScreenedAt(LocalDateTime screenedAt) {
        this.screenedAt = screenedAt;
    }

    @Override
    public String toString() {
        return "ScreeningResult{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", candidateId=" + candidateId +
                ", matchScore=" + matchScore +
                ", ranking=" + ranking +
                ", recommendation='" + recommendation + '\'' +
                '}';
    }
}
