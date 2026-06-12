package com.hiresense.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * ResumeParserService
 *
 * Parses uploaded resumes (PDF extracted text), extracts structured
 * candidate information including name, email, skills, and experience.
 *
 * Add:    feat: add resume parser service for candidate profile extraction
 * Delete: refactor: move resume parsing to ResumeProcessingPipeline, remove standalone service
 */
@Service
public class ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^([A-Z][a-z]+(\\s[A-Z][a-z]+){1,3})");

    private static final Pattern EXPERIENCE_PATTERN =
            Pattern.compile("(\\d+)[+]?\\s*(years?|yrs?)(\\s+of)?\\s+(experience|exp)", Pattern.CASE_INSENSITIVE);

    private static final List<String> DEFAULT_SKILL_KEYWORDS = Arrays.asList(
            "Java", "Spring Boot", "Python", "JavaScript", "TypeScript", "React",
            "Node.js", "SQL", "MySQL", "PostgreSQL", "MongoDB", "Docker", "Kubernetes",
            "AWS", "GCP", "Azure", "REST", "GraphQL", "Microservices", "Kafka",
            "Redis", "Git", "CI/CD", "Linux", "Maven", "Gradle", "JUnit", "Mockito"
    );

    /**
     * Extracts the candidate's full name from the resume text.
     * Assumes the name appears on the first 3 lines.
     *
     * @param text raw resume text
     * @return extracted name or "Unknown"
     */
    public String extractName(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Empty resume text provided for name extraction");
            return "Unknown";
        }
        String[] lines = text.strip().split("\\r?\\n");
        for (int i = 0; i < Math.min(3, lines.length); i++) {
            String line = lines[i].strip();
            Matcher m = NAME_PATTERN.matcher(line);
            if (m.find()) {
                log.debug("Extracted name: {}", m.group(1));
                return m.group(1);
            }
        }
        log.warn("Could not extract name from resume text");
        return "Unknown";
    }

    /**
     * Extracts the first email address found in the resume text.
     *
     * @param text raw resume text
     * @return email string or empty string
     */
    public String extractEmail(String text) {
        if (text == null || text.isBlank()) return "";
        Matcher m = EMAIL_PATTERN.matcher(text);
        if (m.find()) {
            String email = m.group();
            log.debug("Extracted email: {}", email);
            return email;
        }
        log.warn("No email found in resume text");
        return "";
    }

    /**
     * Matches known skills against the resume text (case-insensitive).
     *
     * @param text        raw resume text
     * @param knownSkills list of skills to look for
     * @return list of matched skill strings
     */
    public List<String> extractSkills(String text, List<String> knownSkills) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<String> pool = (knownSkills == null || knownSkills.isEmpty())
                ? DEFAULT_SKILL_KEYWORDS : knownSkills;

        String lowerText = text.toLowerCase();
        List<String> found = pool.stream()
                .filter(skill -> lowerText.contains(skill.toLowerCase()))
                .collect(Collectors.toList());
        log.info("Extracted {} skills from resume", found.size());
        return found;
    }

    /**
     * Extracts the total years of experience from the resume text.
     *
     * @param text raw resume text
     * @return years of experience (0 if not found)
     */
    public int extractExperienceYears(String text) {
        if (text == null || text.isBlank()) return 0;
        Matcher m = EXPERIENCE_PATTERN.matcher(text);
        int maxYears = 0;
        while (m.find()) {
            try {
                int years = Integer.parseInt(m.group(1));
                if (years > maxYears) maxYears = years;
            } catch (NumberFormatException ignored) {
            }
        }
        log.debug("Extracted experience years: {}", maxYears);
        return maxYears;
    }

    /**
     * Orchestrates full parsing and returns a structured candidate profile map.
     *
     * @param resumeText raw text extracted from PDF
     * @return map containing name, email, skills, experienceYears, parsedAt
     */
    public Map<String, Object> buildCandidateProfile(String resumeText) {
        log.info("Building candidate profile from resume text ({} chars)", resumeText == null ? 0 : resumeText.length());
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", extractName(resumeText));
        profile.put("email", extractEmail(resumeText));
        profile.put("skills", extractSkills(resumeText, null));
        profile.put("experienceYears", extractExperienceYears(resumeText));
        profile.put("parsedAt", System.currentTimeMillis());
        log.info("Candidate profile built successfully for: {}", profile.get("name"));
        return profile;
    }
}
