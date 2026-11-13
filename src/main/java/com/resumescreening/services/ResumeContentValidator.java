package com.resumescreening.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Requires resume-like identity and career/qualification evidence, not merely readable PDF text.
 */
public class ResumeContentValidator {

    private static final Pattern EMAIL = Pattern.compile(
        "(?i)\\b[A-Z0-9._%+-]+\\s*@\\s*[A-Z0-9.-]+\\s*\\.\\s*[A-Z]{2,}\\b"
    );
    private static final Pattern PHONE = Pattern.compile(
        "(?<!\\w)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\w)"
    );
    private static final Pattern RESUME_TITLE = Pattern.compile(
        "(?im)^\\s*(?:resume|curriculum vitae|cv|professional profile)\\s*$"
    );
    private static final Pattern CAREER_EVIDENCE = Pattern.compile(
        "(?i)\\b(?:work experience|professional experience|employment history|career history|"
            + "internship|internships|worked as|years? of experience|experience working|"
            + "professional summary|work history)\\b"
    );
    private static final Pattern EDUCATION_EVIDENCE = Pattern.compile(
        "(?i)\\b(?:bachelor(?:'s)?|master(?:'s)?|ph\\.?d\\.?|associate degree|university|"
            + "college|diploma|education|b\\.?s\\.?|b\\.?e\\.?|m\\.?s\\.?|m\\.?b\\.?a\\.?)\\b"
    );
    private static final Set<String> SECTION_HEADINGS = Set.of(
        "education", "work experience", "professional experience", "experience", "employment history",
        "career history", "work history", "skills", "technical skills", "certifications",
        "internship", "internships", "projects", "professional summary", "summary", "profile"
    );
    private static final Pattern SKILLS_HEADING = Pattern.compile(
        "(?im)^\\s*(?:technical\\s+)?skills\\s*:?\\s*$"
    );

    private final ResumeProfileExtractor profileExtractor;

    public ResumeContentValidator() {
        this(new ResumeProfileExtractor());
    }

    public ResumeContentValidator(ResumeProfileExtractor profileExtractor) {
        this.profileExtractor = profileExtractor;
    }

    public boolean isResume(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String eligibleText = ResumeProfileExtractor.excludeSensitiveAttributeLines(text);
        String normalized = normalizeEmailText(eligibleText);
        boolean hasContact = EMAIL.matcher(normalized).find() || PHONE.matcher(normalized).find();
        boolean hasCareer = CAREER_EVIDENCE.matcher(normalized).find();
        boolean hasEducation = EDUCATION_EVIDENCE.matcher(normalized).find();
        boolean hasSkills = SKILLS_HEADING.matcher(normalized).find()
            || !profileExtractor.extract(normalized).skills().isEmpty();
        int sectionCount = countSections(normalized);
        boolean hasResumeTitle = RESUME_TITLE.matcher(normalized).find();

        if (hasResumeTitle && (hasContact || hasCareer || hasEducation || hasSkills)) {
            return true;
        }
        if (hasContact && (hasCareer || hasEducation || hasSkills)) {
            return true;
        }
        return sectionCount >= 2 && (hasCareer || hasEducation || hasSkills);
    }

    static String normalizeEmailText(String text) {
        return text
            .replaceAll("(?i)\\s*(?:\\[at\\]|\\(at\\)|\\bat\\b)\\s*", "@")
            .replaceAll("(?i)\\s*(?:\\[dot\\]|\\(dot\\)|\\bdot\\b)\\s*", ".")
            .replaceAll("\\s*@\\s*", "@")
            .replaceAll("\\s*\\.\\s*", ".");
    }

    private int countSections(String text) {
        List<String> found = new ArrayList<>();
        for (String line : text.lines().toList()) {
            String heading = line.strip().replaceAll("[:\\s]+$", "").toLowerCase(Locale.ROOT);
            if (SECTION_HEADINGS.contains(heading) && !found.contains(heading)) {
                found.add(heading);
            }
        }
        return found.size();
    }
}
