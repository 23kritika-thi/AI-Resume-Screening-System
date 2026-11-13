package com.resumescreening.ai;

import com.resumescreening.models.Job;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ScreeningResult;

/**
 * Interface defining the contract for AI resume screening, skill matching, and ranking engines.
 * Full implementation will be added in subsequent phases.
 */
public interface ResumeRankingEngine {

    /**
     * Scores and evaluates a candidate resume against a specific job opening.
     *
     * @param job Target job specification with requirements and descriptions.
     * @param resume Candidate resume with extracted contents.
     * @return ScreeningResult containing match score, matched/missing skills, and recommendation.
     */
    ScreeningResult evaluate(Job job, Resume resume);
}
