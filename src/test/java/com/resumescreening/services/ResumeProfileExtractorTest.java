package com.resumescreening.services;

import com.resumescreening.models.ResumeProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResumeProfileExtractorTest {

    private final ResumeProfileExtractor extractor = new ResumeProfileExtractor();

    @Test
    @DisplayName("Extracts contact, skill categories, qualifications, and evidence-backed experience")
    void extractsResumeProfile() {
        String text = """
            Jane Doe
            jane.doe@example.test
            +1 415 555 0134
            5 years of professional experience
            Skills
            Java, Spring Boot, MySQL, Docker
            Education
            Bachelor of Science in Computer Science
            Work Experience
            Backend engineer building Spring Boot services
            Internships
            Software engineering intern
            Projects
            Candidate matching project
            Certifications
            Cloud practitioner certification
            """;

        ResumeProfile profile = extractor.extract(text);

        assertEquals("Jane Doe", profile.candidateName());
        assertEquals("jane.doe@example.test", profile.email());
        assertEquals("+1 415 555 0134", profile.phone());
        assertTrue(profile.categorizedSkills().get("Programming Languages").contains("Java"));
        assertTrue(profile.categorizedSkills().get("Frameworks & Technologies").contains("Spring Boot"));
        assertTrue(profile.education().get(0).contains("Bachelor"));
        assertTrue(profile.workExperience().get(0).contains("Backend engineer"));
        assertTrue(profile.internships().get(0).contains("intern"));
        assertTrue(profile.projects().get(0).contains("Candidate matching"));
        assertTrue(profile.certifications().get(0).contains("certification"));
        assertEquals(5, profile.experienceYears());
    }

    @Test
    @DisplayName("Does not infer missing candidate details or treat age as work experience")
    void doesNotInventMissingInformation() {
        ResumeProfile profile = extractor.extract("Age: 42\nGender: female\nA short skills section");

        assertEquals("", profile.candidateName());
        assertEquals("", profile.email());
        assertEquals("", profile.phone());
        assertEquals(0, profile.experienceYears());
    }

    @Test
    @DisplayName("Excludes sensitive fields and their following value lines")
    void excludesSensitiveMultiLineFields() {
        ResumeProfile profile = extractor.extract("Religion\nJava\nSkills\nSpring Boot");

        assertEquals("", profile.candidateName());
        assertFalse(profile.skills().contains("Java"));
        assertTrue(profile.skills().contains("Spring Boot"));
    }

    @Test
    @DisplayName("Extracts common email formats across case and PDF whitespace variations")
    void extractsEmailsAcrossPdfTextVariations() {
        assertEquals("jane.doe+jobs@mail.example.co.uk",
            extractor.extract("Jane Doe\nJANE.DOE+Jobs@MAIL.EXAMPLE.CO.UK").email());
        assertEquals("first.last@example.com",
            extractor.extract("First Last\nEmail: first . last @ example . com").email());
        assertEquals("mixed.user+role@example.org",
            extractor.extract("Mixed User\nEmail: MiXeD.User+\u200Brole @\r\n example .\n org").email());
        assertEquals("jane.doe@example.com",
            extractor.extract("Jane Doe\nEmail: jane.doe [at] example [dot] com").email());
    }

    @Test
    @DisplayName("Categorizes design and user-research skills for UI/UX profiles")
    void extractsDesignAndResearchSkills() {
        ResumeProfile profile = extractor.extract(
            "Skylar Evans\nSkills\nFigma, wireframing, prototyping, usability, user research, accessibility"
        );

        assertTrue(profile.categorizedSkills().get("Design & Research").containsAll(
            java.util.List.of("Figma", "wireframing", "prototyping", "usability", "user research", "accessibility")
        ));
    }
}
