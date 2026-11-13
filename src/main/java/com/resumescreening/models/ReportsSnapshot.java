package com.resumescreening.models;

import java.util.List;

/** Immutable database snapshot used by the Reports and Analytics screen and exports. */
public record ReportsSnapshot(DashboardMetrics metrics, List<JobAnalytics> jobs) {

    public ReportsSnapshot {
        jobs = List.copyOf(jobs);
    }

    public List<ScreeningEntry> screenings() {
        return jobs.stream()
            .flatMap(job -> job.screenings().stream())
            .toList();
    }

    public int screenedCandidateCount() {
        return (int) screenings().stream()
            .map(entry -> entry.candidate().getId())
            .distinct()
            .count();
    }
}
