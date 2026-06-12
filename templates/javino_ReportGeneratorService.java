package com.javino.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * ReportGeneratorService
 *
 * Generates detailed, structured authenticity analysis reports in a
 * LinkedHashMap format with predictable section ordering.
 *
 * Add:    feat: add report generator service for structured analysis output
 * Delete: refactor: inline report generation into AnalysisController response builder
 */
@Service
public class ReportGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ReportGeneratorService.class);

    private static final Map<String, String> SCORE_LABELS = new LinkedHashMap<>();

    static {
        SCORE_LABELS.put("VERY_HIGH",   "Very High Probability of AI Generation");
        SCORE_LABELS.put("HIGH",        "High Probability of AI Generation");
        SCORE_LABELS.put("MODERATE",    "Moderate AI Influence Detected");
        SCORE_LABELS.put("LOW",         "Low AI Influence — Likely Human-Written");
        SCORE_LABELS.put("VERY_LOW",    "Very Low AI Influence — Human-Written");
    }

    /**
     * Builds a complete authenticity analysis report.
     *
     * @param jobId         unique identifier for this analysis job
     * @param originalText  the original input text
     * @param modelResults  map of modelName → {"score": double, "label": String, "sentences": List}
     * @param sentenceData  list of per-sentence analysis objects
     * @return LinkedHashMap with sections: overview, model_breakdown, sentence_analysis, recommendation
     */
    public Map<String, Object> buildFullReport(String jobId,
                                               String originalText,
                                               Map<String, Map<String, Object>> modelResults,
                                               List<Map<String, Object>> sentenceData) {
        log.info("Building full report for jobId={}", jobId);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("jobId",       jobId);
        report.put("generatedAt", Instant.now().toString());
        report.put("textLength",  originalText == null ? 0 : originalText.length());

        report.put("overview",          generateSummary(originalText, modelResults));
        report.put("model_breakdown",   buildModelBreakdown(modelResults));
        report.put("sentence_analysis", sentenceData != null ? sentenceData : Collections.emptyList());
        report.put("recommendation",    generateRecommendations(modelResults));

        log.info("Report built successfully for jobId={}", jobId);
        return report;
    }

    /**
     * Generates a high-level summary section with aggregate scoring.
     *
     * @param originalText raw text
     * @param modelResults per-model results
     * @return summary map with averageScore, verdict, wordCount, modelCount
     */
    public Map<String, Object> generateSummary(String originalText,
                                               Map<String, Map<String, Object>> modelResults) {
        Map<String, Object> summary = new LinkedHashMap<>();
        int wordCount = originalText == null ? 0 : originalText.split("\\s+").length;
        summary.put("wordCount",   wordCount);
        summary.put("modelCount",  modelResults == null ? 0 : modelResults.size());

        double avgScore = computeAverageScore(modelResults);
        summary.put("averageAiScore", formatScore(avgScore));
        summary.put("verdict",        deriveVerdict(avgScore));
        summary.put("verdictLabel",   SCORE_LABELS.getOrDefault(deriveVerdict(avgScore), "Unknown"));
        return summary;
    }

    /**
     * Adds a single model's result section to the report.
     *
     * @param modelName    name of the AI model (e.g., "GPT-4o")
     * @param score        AI detection score (0.0–1.0)
     * @param rawResponse  the model's raw JSON response string
     * @return model section map
     */
    public Map<String, Object> addModelSection(String modelName, double score, String rawResponse) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("model",       modelName);
        section.put("score",       formatScore(score));
        section.put("confidence",  score >= 0.8 ? "HIGH" : score >= 0.5 ? "MEDIUM" : "LOW");
        section.put("rawResponse", rawResponse != null ? rawResponse.substring(0, Math.min(500, rawResponse.length())) : "");
        return section;
    }

    /**
     * Formats a 0–1 score as a percentage string (e.g., "73.5%").
     *
     * @param score raw score between 0.0 and 1.0
     * @return formatted string like "73.5%"
     */
    public String formatScore(double score) {
        double clamped = Math.max(0.0, Math.min(1.0, score));
        return String.format("%.1f%%", clamped * 100.0);
    }

    /**
     * Generates recommendations based on overall AI detection score.
     *
     * @param modelResults per-model result data
     * @return recommendation map with action, detail, confidence
     */
    public Map<String, Object> generateRecommendations(Map<String, Map<String, Object>> modelResults) {
        double avg = computeAverageScore(modelResults);
        Map<String, Object> rec = new LinkedHashMap<>();

        if (avg >= 0.85) {
            rec.put("action",     "REJECT");
            rec.put("detail",     "Text is very likely AI-generated. Recommend manual review and rejection in academic or professional contexts.");
            rec.put("confidence", "HIGH");
        } else if (avg >= 0.65) {
            rec.put("action",     "FLAG");
            rec.put("detail",     "Significant AI influence detected. Human review is strongly advised before acceptance.");
            rec.put("confidence", "MEDIUM");
        } else if (avg >= 0.40) {
            rec.put("action",     "REVIEW");
            rec.put("detail",     "Moderate AI patterns found. Consider requesting the author to verify originality.");
            rec.put("confidence", "MEDIUM");
        } else {
            rec.put("action",     "ACCEPT");
            rec.put("detail",     "Text appears predominantly human-written. Low AI influence detected.");
            rec.put("confidence", "HIGH");
        }
        rec.put("averageScore", formatScore(avg));
        return rec;
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private Map<String, Object> buildModelBreakdown(Map<String, Map<String, Object>> modelResults) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        if (modelResults == null || modelResults.isEmpty()) return breakdown;
        for (Map.Entry<String, Map<String, Object>> entry : modelResults.entrySet()) {
            String model  = entry.getKey();
            double score  = toDouble(entry.getValue().get("score"));
            String raw    = String.valueOf(entry.getValue().getOrDefault("rawResponse", ""));
            breakdown.put(model, addModelSection(model, score, raw));
        }
        return breakdown;
    }

    private double computeAverageScore(Map<String, Map<String, Object>> modelResults) {
        if (modelResults == null || modelResults.isEmpty()) return 0.0;
        return modelResults.values().stream()
                .mapToDouble(m -> toDouble(m.get("score")))
                .average()
                .orElse(0.0);
    }

    private String deriveVerdict(double avg) {
        if (avg >= 0.85) return "VERY_HIGH";
        if (avg >= 0.65) return "HIGH";
        if (avg >= 0.40) return "MODERATE";
        if (avg >= 0.20) return "LOW";
        return "VERY_LOW";
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); }
        catch (Exception e) { return 0.0; }
    }
}
