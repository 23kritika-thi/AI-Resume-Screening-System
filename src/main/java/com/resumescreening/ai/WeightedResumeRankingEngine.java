package com.resumescreening.ai;

import com.resumescreening.models.Job;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ResumeProfile;
import com.resumescreening.models.ScreeningResult;
import com.resumescreening.services.ResumeProfileExtractor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Explainable, deterministic scoring. Results are decision-support signals only.
 */
public class WeightedResumeRankingEngine implements ResumeRankingEngine {

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "into", "is", "of",
        "on", "or", "that", "the", "this", "to", "with", "will", "years", "year",
        "age", "gender", "sex", "religion", "caste", "ethnicity", "ethnic", "race",
        "male", "female", "nonbinary", "nationality", "photograph", "photo"
    );
    private static final Pattern SKILL_SEPARATOR = Pattern.compile("[,;\\n\\r]+");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}+#.]+");
    private static final Pattern CONTACT = Pattern.compile(
        "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b|(?<!\\w)\\+?\\d[\\d ()-]{7,}\\d(?!\\w)"
    );
    private final ResumeProfileExtractor profileExtractor;

    public WeightedResumeRankingEngine() {
        this(new ResumeProfileExtractor());
    }

    public WeightedResumeRankingEngine(ResumeProfileExtractor profileExtractor) {
        this.profileExtractor = profileExtractor;
    }

    @Override
    public ScreeningResult evaluate(Job job, Resume resume) {
        if (job == null || resume == null) {
            throw new IllegalArgumentException("A job and resume are required to evaluate a match.");
        }
        String eligibleText = scoringText(resume.getExtractedText());
        ResumeProfile profile = profileExtractor.extract(eligibleText);
        Set<String> resumeSkills = new LinkedHashSet<>(profile.skills());
        List<String> required = parseSkills(job.getRequiredSkills());
        List<String> preferred = parseSkills(job.getPreferredSkills());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        int requiredMatched = countMatches(required, resumeSkills, eligibleText, matched, missing);
        int preferredMatched = countMatches(preferred, resumeSkills, eligibleText, null, null);
        double requiredPoints = required.isEmpty() ? 0 : 50.0 * requiredMatched / required.size();
        double preferredPoints = preferred.isEmpty() ? 0 : 20.0 * preferredMatched / preferred.size();
        double experiencePoints = job.getMinExperienceYears() <= 0
            ? 15.0
            : 15.0 * Math.min(1.0, (double) profile.experienceYears() / job.getMinExperienceYears());
        double similarityPoints = 15.0 * cosineSimilarity(
            job.getDescription(), eligibleText
        );
        BigDecimal requiredScore = score(requiredPoints);
        BigDecimal preferredScore = score(preferredPoints);
        BigDecimal experienceScore = score(experiencePoints);
        BigDecimal similarityScore = score(similarityPoints);
        BigDecimal total = requiredScore.add(preferredScore).add(experienceScore).add(similarityScore);

        ScreeningResult result = new ScreeningResult(
            job.getId(), resume.getCandidateId(), resume.getId(), total,
            String.join(", ", matched), String.join(", ", missing), null, "Recruiter review"
        );
        result.setRequiredSkillsScore(requiredScore);
        result.setPreferredSkillsScore(preferredScore);
        result.setExperienceScore(experienceScore);
        result.setTextSimilarityScore(similarityScore);
        return result;
    }

    public static double cosineSimilarity(String first, String second) {
        List<String> left = tokens(first);
        List<String> right = tokens(second);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> vocabulary = new LinkedHashSet<>(left);
        vocabulary.addAll(right);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (String term : vocabulary) {
            double leftWeight = tfidf(term, left, right);
            double rightWeight = tfidf(term, right, left);
            dot += leftWeight * rightWeight;
            leftNorm += leftWeight * leftWeight;
            rightNorm += rightWeight * rightWeight;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm))));
    }

    private static double tfidf(String term, List<String> document, List<String> other) {
        long count = document.stream().filter(term::equals).count();
        double tf = (double) count / document.size();
        int documentFrequency = (document.contains(term) ? 1 : 0) + (other.contains(term) ? 1 : 0);
        double idf = Math.log(1.0 + 2.0 / (1.0 + documentFrequency));
        return tf * idf;
    }

    private int countMatches(List<String> requested, Set<String> knownSkills, String resumeText,
                             List<String> matched, List<String> missing) {
        int count = 0;
        String lowerText = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
        for (String skill : requested) {
            String escaped = Pattern.quote(skill.toLowerCase(Locale.ROOT));
            boolean present = knownSkills.stream().anyMatch(found -> found.equalsIgnoreCase(skill))
                || Pattern.compile("(?<![\\p{L}\\p{N}])" + escaped + "(?![\\p{L}\\p{N}])")
                    .matcher(lowerText).find();
            if (present) {
                count++;
                if (matched != null) {
                    matched.add(skill);
                }
            } else if (missing != null) {
                missing.add(skill);
            }
        }
        return count;
    }

    private static List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(SKILL_SEPARATOR.split(skills))
            .map(String::strip)
            .filter(value -> !value.isBlank() && !isSensitiveAttribute(value))
            .distinct()
            .toList();
    }

    private static String scoringText(String text) {
        if (text == null) {
            return "";
        }
        return ResumeProfileExtractor.excludeSensitiveAttributeLines(text).lines()
            .map(line -> CONTACT.matcher(line).replaceAll(" "))
            .reduce("", (first, second) -> first + "\n" + second);
    }

    private static boolean isSensitiveAttribute(String value) {
        return Pattern.compile(
            "(?i)\\b(?:gender|sex|religion|caste|ethnicity|race|age|date of birth|dob|"
                + "marital status|nationality|photograph|photo)\\b"
        ).matcher(value).find();
    }

    private static List<String> tokens(String text) {
        if (text == null) {
            return List.of();
        }
        var matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static BigDecimal score(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
