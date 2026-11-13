package com.resumescreening.models;

import java.util.List;
import java.util.Map;

/**
 * Information that could be extracted from resume text. Missing fields remain empty.
 */
public record ResumeProfile(
    String candidateName,
    String email,
    String phone,
    List<String> skills,
    Map<String, List<String>> categorizedSkills,
    List<String> education,
    List<String> workExperience,
    List<String> internships,
    List<String> projects,
    List<String> certifications,
    int experienceYears
) {
    public ResumeProfile {
        skills = List.copyOf(skills);
        categorizedSkills = Map.copyOf(categorizedSkills);
        education = List.copyOf(education);
        workExperience = List.copyOf(workExperience);
        internships = List.copyOf(internships);
        projects = List.copyOf(projects);
        certifications = List.copyOf(certifications);
    }
}
