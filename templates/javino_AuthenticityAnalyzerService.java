package com.javino.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.logging.Logger;

/**
 * AuthenticityAnalyzerService
 * Core service for analyzing text authenticity using multi-LLM consensus.
 *
 * Models queried:
 *   - GPT-4o  (OpenAI)         weight: 45%
 *   - Gemini  (Google AI)      weight: 35%
 *   - Llama 3 (Groq)           weight: 20%
 */
@Service
public class AuthenticityAnalyzerService {

    private static final Logger log = Logger.getLogger(AuthenticityAnalyzerService.class.getName());

    @Value("${openai.api.key}")
    private String openaiKey;

    @Value("${groq.api.key}")
    private String groqKey;

    @Value("${google.ai.key}")
    private String googleKey;

    private static final Map<String, Double> MODEL_WEIGHTS = Map.of(
        "gpt4o",  0.45,
        "gemini", 0.35,
        "llama",  0.20
    );

    /**
     * Analyze a text document for AI-generated content.
     * @param text raw document text
     * @return report with consensus score, label, and per-model breakdown
     */
    public Map<String, Object> analyze(String text) {
        if (text == null || text.isBlank()) {
            return Map.of("error", "Empty input", "score", 0.0, "label", "UNKNOWN");
        }

        Map<String, Double> modelScores = new LinkedHashMap<>();
        modelScores.put("gpt4o",  queryGpt4o(text));
        modelScores.put("gemini", queryGemini(text));
        modelScores.put("llama",  queryLlama(text));

        double consensus = computeWeightedConsensus(modelScores);
        String label     = classifyScore(consensus);

        log.info(String.format("Analysis done: score=%.2f label=%s", consensus, label));
        return buildReport(text, consensus, label, modelScores);
    }

    private double computeWeightedConsensus(Map<String, Double> scores) {
        return scores.entrySet().stream()
            .mapToDouble(e -> e.getValue() * MODEL_WEIGHTS.getOrDefault(e.getKey(), 0.0))
            .sum();
    }

    private String classifyScore(double score) {
        if (score >= 80) return "LIKELY_AI_GENERATED";
        if (score >= 50) return "MIXED_CONTENT";
        if (score >= 20) return "LIKELY_HUMAN";
        return "HUMAN_WRITTEN";
    }

    private Map<String, Object> buildReport(String text, double score, String label,
                                             Map<String, Double> breakdown) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("textLength",     text.length());
        r.put("wordCount",      text.split("\\s+").length);
        r.put("score",          Math.round(score * 100.0) / 100.0);
        r.put("label",          label);
        r.put("modelBreakdown", breakdown);
        r.put("analyzedAt",     new Date().toString());
        return r;
    }

    // Stubs replaced by actual HTTP calls in ModelRouterService
    private double queryGpt4o(String text)  { return 50.0 + Math.random() * 50; }
    private double queryGemini(String text) { return 40.0 + Math.random() * 60; }
    private double queryLlama(String text)  { return 45.0 + Math.random() * 55; }
}
