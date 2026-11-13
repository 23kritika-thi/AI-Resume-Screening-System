package com.resumescreening.services;

import com.resumescreening.ai.WeightedResumeRankingEngine;
import com.resumescreening.models.Job;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ScreeningResult;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Scores all stored resumes for a recruiter-owned job and persists the ranked output.
 */
public class ResumeScreeningService {

    private final DatabaseService databaseService;
    private final WeightedResumeRankingEngine rankingEngine;

    public ResumeScreeningService(DatabaseService databaseService) {
        this(databaseService, new WeightedResumeRankingEngine());
    }

    public ResumeScreeningService(DatabaseService databaseService, WeightedResumeRankingEngine rankingEngine) {
        this.databaseService = databaseService;
        this.rankingEngine = rankingEngine;
    }

    public List<ScreeningEntry> screen(int recruiterId, int jobId) throws SQLException {
        return screen(recruiterId, jobId, databaseService.getResumes());
    }

    public List<ScreeningEntry> screen(int recruiterId, int jobId, List<ResumeEntry> resumes)
        throws SQLException {
        Job job = databaseService.getJobsForRecruiter(recruiterId).stream()
            .filter(candidate -> candidate.getId() == jobId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Select a job owned by the signed-in recruiter."));
        if (resumes == null) {
            throw new IllegalArgumentException("A resume list is required for screening.");
        }
        List<ScoredEntry> scored = new ArrayList<>();
        for (ResumeEntry entry : resumes) {
            ScreeningResult result = rankingEngine.evaluate(job, entry.resume());
            scored.add(new ScoredEntry(entry, result));
        }
        List<ScreeningEntry> ranked = rank(scored.stream()
            .map(entry -> new ScreeningEntry(entry.entry().candidate(), entry.entry().resume(), entry.result()))
            .toList());
        List<ScreeningResult> results = ranked.stream().map(ScreeningEntry::result).toList();
        if (!results.isEmpty()) {
            databaseService.saveScreeningResults(results);
        }
        return List.copyOf(ranked);
    }

    public static List<ScreeningEntry> rank(List<ScreeningEntry> entries) {
        List<ScreeningEntry> ranked = new ArrayList<>(entries);
        ranked.sort(java.util.Comparator
            .comparing((ScreeningEntry entry) -> entry.result().getMatchScore()).reversed()
            .thenComparing(entry -> entry.candidate().getFullName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(entry -> entry.resume().getId()));
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).result().setRanking(i + 1);
        }
        return List.copyOf(ranked);
    }

    private record ScoredEntry(ResumeEntry entry, ScreeningResult result) {
    }
}
