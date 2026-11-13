package com.resumescreening.ai;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.Job;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.models.ScreeningResult;
import com.resumescreening.services.ResumeScreeningService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeightedResumeRankingEngineTest {

    private final WeightedResumeRankingEngine engine = new WeightedResumeRankingEngine();

    @Test
    @DisplayName("Applies weighted skill, experience, and text similarity scores with an explanation")
    void calculatesTransparentWeightedScore() {
        Job job = new Job(7, "Backend Engineer", "Engineering",
            "Build Java and Spring Boot backend services with SQL.", "Java, Spring Boot, C++", 4);
        job.setPreferredSkills("Docker");
        Resume resume = new Resume(12, 4, "jane.pdf", "storage/jane.pdf",
            "Jane Doe\nJava Spring Boot Docker SQL backend services and 5 years of professional experience",
            "hash", null);

        ScreeningResult result = engine.evaluate(job, resume);

        assertEquals(new BigDecimal("33.33"), result.getRequiredSkillsScore());
        assertEquals(new BigDecimal("20.00"), result.getPreferredSkillsScore());
        assertEquals(new BigDecimal("15.00"), result.getExperienceScore());
        assertTrue(result.getTextSimilarityScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getMatchScore().compareTo(new BigDecimal("53.33")) > 0);
        assertEquals(result.getRequiredSkillsScore().add(result.getPreferredSkillsScore())
            .add(result.getExperienceScore()).add(result.getTextSimilarityScore()), result.getMatchScore());
        assertTrue(result.getMissingSkills().contains("C++"));
        assertTrue(result.getMatchedSkills().contains("Java"));
        assertEquals("Recruiter review", result.getRecommendation());
    }

    @Test
    @DisplayName("Does not include sensitive attribute labels or numeric age in scoring")
    void excludesSensitiveAttributesFromScore() {
        Job job = new Job(7, "Engineer", "Engineering", "Build Java services.", "Java", 0);
        Resume first = new Resume(1, 1, "one.pdf", "", "Jane Doe\nJava engineer\nAge: 27\nGender: female", "", null);
        Resume second = new Resume(2, 2, "two.pdf", "", "Jane Doe\nJava engineer\nAge: 63\nGender: male", "", null);

        assertEquals(engine.evaluate(job, first).getMatchScore(), engine.evaluate(job, second).getMatchScore());
        assertEquals(0, new com.resumescreening.services.ResumeProfileExtractor()
            .extract("Age: 63 years").experienceYears());
    }

    @Test
    @DisplayName("Ranks higher-scoring candidates first with deterministic tie breaking")
    void ranksCandidates() {
        ScreeningEntry lower = entry("Zara", 1, "62.00");
        ScreeningEntry higher = entry("Alex", 2, "92.00");
        ScreeningEntry tie = entry("Bea", 3, "92.00");

        List<ScreeningEntry> ranked = ResumeScreeningService.rank(List.of(lower, higher, tie));

        assertEquals("Alex", ranked.get(0).candidate().getFullName());
        assertEquals("Bea", ranked.get(1).candidate().getFullName());
        assertEquals("Zara", ranked.get(2).candidate().getFullName());
        assertEquals(1, ranked.get(0).result().getRanking());
        assertEquals(3, ranked.get(2).result().getRanking());
    }

    private ScreeningEntry entry(String name, int id, String score) {
        Candidate candidate = new Candidate(name, null, null, null);
        candidate.setId(id);
        Resume resume = new Resume(id, id, name + ".pdf", "", "", "", null);
        ScreeningResult result = new ScreeningResult(1, id, id, new BigDecimal(score),
            "", "", null, "Recruiter review");
        return new ScreeningEntry(candidate, resume, result);
    }
}
