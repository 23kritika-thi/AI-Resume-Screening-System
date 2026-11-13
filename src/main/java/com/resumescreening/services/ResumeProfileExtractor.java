package com.resumescreening.services;

import com.resumescreening.models.ResumeProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts conservative, evidence-backed contact and qualification details from resume text.
 */
public class ResumeProfileExtractor {

    private static final String EMAIL_LOCAL_ATOM = "[\\p{L}\\p{N}!#$%&'*+/=?^_`{|}~-]+";
    private static final String EMAIL_LOCAL_PART = EMAIL_LOCAL_ATOM + "(?:\\." + EMAIL_LOCAL_ATOM + ")*";
    private static final String EMAIL_DOMAIN_LABEL =
        "[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?";
    private static final Pattern EMAIL = Pattern.compile(
        "(?iu)(?<![\\p{L}\\p{N}._%+-])" + EMAIL_LOCAL_PART + "@"
            + EMAIL_DOMAIN_LABEL + "(?:\\." + EMAIL_DOMAIN_LABEL + ")+(?![\\p{L}\\p{N}-])"
    );
    private static final Pattern PHONE = Pattern.compile(
        "(?<!\\w)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\w)"
    );
    private static final Pattern YEARS = Pattern.compile(
        "(?i)\\b(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)\\s+(?:of\\s+)?(?:professional\\s+|relevant\\s+|work\\s+)?experience\\b|"
            + "\\bexperience\\s+(?:of\\s+)?(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)\\b"
    );
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "gender", "sex", "religion", "caste", "ethnicity", "ethnic group", "race", "age",
        "date of birth", "dob", "marital status", "nationality", "photograph", "photo"
    );
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
        "(?i)\\b(?:gender|sex|religion|caste|ethnicity|ethnic group|race|age|date of birth|dob|"
            + "marital status|nationality|photograph|photo)\\b\\s*(?::|=|-|\\s)\\s*.+"
    );
    private static final Map<String, List<String>> SKILL_CATEGORIES = buildSkillCategories();
    private static final Set<String> SECTION_HEADINGS = Set.of(
        "education", "work experience", "experience", "professional experience", "internships",
        "projects", "certifications", "skills", "technical skills", "summary", "profile",
        "objective", "work history", "contact"
    );

    public ResumeProfile extract(String text) {
        String safeText = excludeSensitiveAttributeLines(text);
        String name = inferName(safeText);
        String email = extractEmail(safeText);
        String phone = firstMatch(PHONE, safeText);
        Map<String, List<String>> categories = new LinkedHashMap<>();
        Set<String> allSkills = new LinkedHashSet<>();
        String lowerText = safeText.toLowerCase(Locale.ROOT);
        SKILL_CATEGORIES.forEach((category, skills) -> {
            List<String> found = skills.stream()
                .filter(skill -> containsSkill(lowerText, skill))
                .toList();
            if (!found.isEmpty()) {
                categories.put(category, found);
                allSkills.addAll(found);
            }
        });
        List<String> lines = safeText.lines().map(String::strip).filter(line -> !line.isBlank()).toList();
        return new ResumeProfile(
            name, email, phone, new ArrayList<>(allSkills), categories,
            sectionLines(lines, "education", "work experience", "experience", "professional experience", "internships", "projects", "certifications", "skills", "technical skills"),
            sectionLines(lines, "work experience", "experience", "professional experience", "internships", "projects", "certifications", "education", "skills", "technical skills"),
            sectionLines(lines, "internships", "projects", "certifications", "education", "work experience", "experience", "professional experience", "skills", "technical skills"),
            sectionLines(lines, "projects", "certifications", "education", "work experience", "experience", "professional experience", "internships", "skills", "technical skills"),
            sectionLines(lines, "certifications", "education", "work experience", "experience", "professional experience", "internships", "projects", "skills", "technical skills"),
            estimateExperience(safeText)
        );
    }

    public static String excludeSensitiveAttributeLines(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        List<String> lines = text.lines().toList();
        List<String> filtered = new ArrayList<>();
        boolean skipNextValue = false;
        for (String line : lines) {
            String normalized = line.strip().replaceAll("[:=\\-]+$", "").toLowerCase(Locale.ROOT);
            if (skipNextValue) {
                if (line.isBlank()) {
                    filtered.add(line);
                    continue;
                }
                if (!SENSITIVE_FIELDS.contains(normalized)) {
                    skipNextValue = false;
                    continue;
                }
            }
            if (SENSITIVE_FIELDS.contains(normalized)) {
                skipNextValue = true;
                continue;
            }
            if (SENSITIVE_VALUE.matcher(line).find()) {
                continue;
            }
            filtered.add(line);
        }
        return String.join("\n", filtered);
    }

    private String inferName(String text) {
        for (String line : text.lines().limit(5).toList()) {
            String candidate = line.strip();
            if (candidate.isBlank()) {
                continue;
            }
            if (EMAIL.matcher(candidate).find() || PHONE.matcher(candidate).find() || candidate.contains("@")) {
                continue;
            }
            if (candidate.length() > 80
                || SECTION_HEADINGS.contains(candidate.toLowerCase(Locale.ROOT))
                || candidate.matches(".*\\d.*")) {
                return "";
            }
            String[] words = candidate.split("\\s+");
            boolean nameCase = java.util.Arrays.stream(words).allMatch(word ->
                !word.isEmpty() && Character.isUpperCase(word.codePointAt(0))
            );
            if (words.length >= 2 && words.length <= 5 && nameCase
                && candidate.matches("[\\p{L} .'-]+")) {
                return candidate;
            }
            return "";
        }
        return "";
    }

    private List<String> sectionLines(List<String> lines, String heading, String... otherHeadings) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).strip().replaceAll("[:\\s]+$", "").equalsIgnoreCase(heading)) {
                start = i + 1;
                break;
            }
        }
        if (start < 0) {
            return List.of();
        }
        Set<String> stops = new LinkedHashSet<>(List.of(otherHeadings));
        List<String> section = new ArrayList<>();
        for (int i = start; i < lines.size() && section.size() < 30; i++) {
            String line = lines.get(i);
            String normalized = line.strip().replaceAll("[:\\s]+$", "").toLowerCase(Locale.ROOT);
            if (stops.contains(normalized)) {
                break;
            }
            if (!line.isBlank()) {
                section.add(line);
            }
        }
        return section;
    }

    private int estimateExperience(String text) {
        int maximum = 0;
        for (String line : text.lines().toList()) {
            Matcher matcher = YEARS.matcher(line);
            while (matcher.find()) {
                String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                maximum = Math.max(maximum, Integer.parseInt(value));
            }
        }
        return maximum;
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().strip() : "";
    }

    private String extractEmail(String text) {
        String normalized = text.replace('\u00A0', ' ')
            .replace("\u200B", "")
            .replace("\uFEFF", "")
            .replace("\u00AD", "");
        Matcher email = EMAIL.matcher(ResumeContentValidator.normalizeEmailText(normalized));
        if (!email.find()) {
            return "";
        }
        return email.group().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static boolean containsSkill(String text, String skill) {
        String escaped = Pattern.quote(skill.toLowerCase(Locale.ROOT));
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + escaped + "(?![\\p{L}\\p{N}])")
            .matcher(text).find();
    }

    private static Map<String, List<String>> buildSkillCategories() {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("Programming Languages", List.of(
            "Java", "Python", "C++", "C#", "JavaScript", "TypeScript", "Kotlin", "Go", "Rust", "SQL", "PHP", "Ruby", "Swift"
        ));
        categories.put("Frameworks & Technologies", List.of(
            "Spring", "Spring Boot", "React", "Angular", "Vue", "Node.js", "Express", "Django", "Flask",
            "TensorFlow", "PyTorch", "Pandas", "NumPy", "Hibernate", "Docker", "Kubernetes", "AWS", "Azure", "GCP",
            "MySQL", "PostgreSQL", "MongoDB", "Git", "Linux", "Kafka", "Redis"
        ));
        categories.put("Design & Research", List.of(
            "Figma", "wireframing", "prototyping", "usability", "user research", "Adobe XD", "accessibility"
        ));
        return Map.copyOf(categories);
    }
}
