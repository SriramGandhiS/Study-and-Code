package com.hiresense.util;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * ScoreCalculator
 * Utility for computing weighted ATS (Applicant Tracking System) scores.
 *
 * Score components:
 *   - Keyword match      (40%)
 *   - Experience align   (30%)
 *   - Communication      (20%)
 *   - Technical depth    (10%)
 */
@Component
public class ScoreCalculator {

    private static final double WEIGHT_KEYWORD    = 0.40;
    private static final double WEIGHT_EXPERIENCE = 0.30;
    private static final double WEIGHT_CLARITY    = 0.20;
    private static final double WEIGHT_TECHNICAL  = 0.10;

    public double computeAtsScore(Map<String, Double> componentScores) {
        double keyword    = componentScores.getOrDefault("keyword",    0.0);
        double experience = componentScores.getOrDefault("experience", 0.0);
        double clarity    = componentScores.getOrDefault("clarity",    0.0);
        double technical  = componentScores.getOrDefault("technical",  0.0);

        return (keyword    * WEIGHT_KEYWORD)   +
               (experience * WEIGHT_EXPERIENCE) +
               (clarity    * WEIGHT_CLARITY)   +
               (technical  * WEIGHT_TECHNICAL);
    }

    public String getRating(double score) {
        if (score >= 85) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 55) return "AVERAGE";
        return "POOR";
    }

    public Map<String, Object> buildReport(String candidateId, Map<String, Double> components) {
        double ats = computeAtsScore(components);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("candidateId",    candidateId);
        report.put("atsScore",       Math.round(ats * 100.0) / 100.0);
        report.put("rating",         getRating(ats));
        report.put("components",     components);
        report.put("recommendation", ats >= 70 ? "PROCEED_TO_INTERVIEW" : "REJECT");
        return report;
    }

    public Map<String, Double> normalizeScores(Map<String, Double> raw) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            double clamped = Math.min(100.0, Math.max(0.0, entry.getValue()));
            normalized.put(entry.getKey(), clamped);
        }
        return normalized;
    }

    public List<String> getImprovementTips(Map<String, Double> scores) {
        List<String> tips = new ArrayList<>();
        if (scores.getOrDefault("keyword", 100.0) < 60)
            tips.add("Add more role-specific keywords to your resume");
        if (scores.getOrDefault("experience", 100.0) < 60)
            tips.add("Highlight relevant experience with quantifiable metrics");
        if (scores.getOrDefault("clarity", 100.0) < 60)
            tips.add("Improve resume formatting for better readability");
        if (scores.getOrDefault("technical", 100.0) < 60)
            tips.add("List specific technical tools and certifications");
        return tips;
    }
}
