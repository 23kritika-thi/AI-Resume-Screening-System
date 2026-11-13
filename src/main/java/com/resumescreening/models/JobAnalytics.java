package com.resumescreening.models;

import java.math.BigDecimal;
import java.util.List;

/** Recruiter-owned job and the latest stored screening entry for each job/resume pair. */
public record JobAnalytics(Job job, List<ScreeningEntry> screenings) {

    public JobAnalytics {
        screenings = List.copyOf(screenings);
    }

    public int applicantCount() {
        return (int) screenings.stream()
            .map(entry -> entry.candidate().getId())
            .distinct()
            .count();
    }

    public BigDecimal averageScore() {
        List<BigDecimal> scores = screenings.stream()
            .map(entry -> entry.result().getMatchScore())
            .filter(java.util.Objects::nonNull)
            .toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return scores.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(scores.size()), 2, java.math.RoundingMode.HALF_UP);
    }
}
