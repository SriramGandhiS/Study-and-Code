package com.hiresense.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FeedbackEngineService
 *
 * Evaluates interview answers and generates structured feedback covering
 * clarity, technical accuracy, communication style, and confidence.
 *
 * Add:    feat: add feedback engine service for post-answer evaluation
 * Delete: refactor: merge feedback engine into EvaluationService pipeline
 */
@Service
public class FeedbackEngineService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackEngineService.class);

    private static final List<String> POSITIVE_MARKERS = Arrays.asList(
            "therefore", "because", "for example", "specifically", "in conclusion",
            "as a result", "to summarize", "first", "second", "finally", "moreover",
            "however", "on the other hand", "consequently", "implementation"
    );

    private static final List<String> TECHNICAL_KEYWORDS = Arrays.asList(
            "algorithm", "complexity", "cache", "database", "api", "microservice",
            "thread", "async", "latency", "throughput", "index", "query", "pattern",
            "refactor", "scalable", "redundancy", "failover", "transaction", "schema"
    );

    private static final List<String> FILLER_WORDS = Arrays.asList(
            "um", "uh", "like", "you know", "sort of", "kind of", "basically", "literally"
    );

    /**
     * Analyzes a single interview answer and returns scored dimensions.
     *
     * @param question the interview question asked
     * @param answer   candidate's answer text
     * @return map with clarity, technicalAccuracy, communication, confidence scores
     */
    public Map<String, Object> analyzeSingleAnswer(String question, String answer) {
        log.info("Analyzing answer for question: '{}'", question.substring(0, Math.min(60, question.length())));
        if (answer == null || answer.isBlank()) {
            log.warn("Empty answer provided, returning zero scores");
            return buildZeroScores();
        }
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("question", question);
        analysis.put("answerLength", answer.split("\\s+").length);
        analysis.put("clarityScore", computeClarityScore(answer));
        analysis.put("technicalAccuracyScore", computeTechnicalScore(answer));
        analysis.put("communicationScore", computeCommunicationScore(answer));
        analysis.put("confidenceScore", computeConfidenceScore(answer));
        return analysis;
    }

    /**
     * Generates detailed structured feedback from a single answer analysis.
     *
     * @param question the question asked
     * @param answer   candidate's answer
     * @return feedback map with scores, strengths, improvements, overallGrade
     */
    public Map<String, Object> generateFeedback(String question, String answer) {
        Map<String, Object> analysis = analyzeSingleAnswer(question, answer);
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("analysis", analysis);

        double avg = computeAverageScore(analysis);
        feedback.put("overallScore", Math.round(avg * 10.0) / 10.0);
        feedback.put("overallGrade", mapScoreToGrade(avg));
        feedback.put("strengths", identifyStrengths(analysis));
        feedback.put("improvements", identifyImprovements(analysis));
        feedback.put("tip", generateTip(answer));
        log.info("Feedback generated — overall score: {}", avg);
        return feedback;
    }

    /**
     * Computes a confidence score (0–10) based on sentence structure and filler word density.
     *
     * @param answer raw answer text
     * @return confidence score
     */
    public double computeConfidenceScore(String answer) {
        if (answer == null || answer.isBlank()) return 0.0;
        String lower = answer.toLowerCase();
        long fillerCount = FILLER_WORDS.stream().filter(lower::contains).count();
        int wordCount = answer.split("\\s+").length;
        double fillerRatio = wordCount == 0 ? 0 : (double) fillerCount / wordCount;
        double base = 10.0 - (fillerRatio * 50);
        // Length bonus: answers between 80–200 words score best
        if (wordCount >= 80 && wordCount <= 200) base = Math.min(base + 1.5, 10.0);
        return Math.max(0.0, Math.min(10.0, Math.round(base * 10.0) / 10.0));
    }

    /**
     * Summarizes a full interview session from a list of per-answer feedback maps.
     *
     * @param sessionFeedbacks list of feedback maps (one per question)
     * @return session summary with averages, topScore, bottomScore, recommendation
     */
    public Map<String, Object> summarizeSession(List<Map<String, Object>> sessionFeedbacks) {
        log.info("Summarizing interview session with {} answers", sessionFeedbacks.size());
        Map<String, Object> summary = new LinkedHashMap<>();
        if (sessionFeedbacks == null || sessionFeedbacks.isEmpty()) {
            summary.put("error", "No feedback data available");
            return summary;
        }
        DoubleSummaryStatistics stats = sessionFeedbacks.stream()
                .mapToDouble(f -> f.containsKey("overallScore") ? (double) f.get("overallScore") : 0.0)
                .summaryStatistics();
        summary.put("totalQuestions", sessionFeedbacks.size());
        summary.put("averageScore", Math.round(stats.getAverage() * 10.0) / 10.0);
        summary.put("highestScore", stats.getMax());
        summary.put("lowestScore", stats.getMin());
        summary.put("sessionGrade", mapScoreToGrade(stats.getAverage()));
        summary.put("recommendation", buildRecommendation(stats.getAverage()));
        return summary;
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private double computeClarityScore(String answer) {
        String lower = answer.toLowerCase();
        long markers = POSITIVE_MARKERS.stream().filter(lower::contains).count();
        int sentences = answer.split("[.!?]+").length;
        double score = Math.min(10.0, (markers * 1.2) + (sentences * 0.4));
        return Math.round(score * 10.0) / 10.0;
    }

    private double computeTechnicalScore(String answer) {
        String lower = answer.toLowerCase();
        long techHits = TECHNICAL_KEYWORDS.stream().filter(lower::contains).count();
        double score = Math.min(10.0, techHits * 0.8);
        return Math.round(score * 10.0) / 10.0;
    }

    private double computeCommunicationScore(String answer) {
        int wordCount = answer.split("\\s+").length;
        double lengthScore = wordCount < 30 ? 3.0 : wordCount < 80 ? 6.0 : wordCount < 250 ? 9.0 : 7.0;
        boolean hasStructure = answer.contains(",") && answer.contains(".");
        return Math.min(10.0, hasStructure ? lengthScore + 1.0 : lengthScore);
    }

    private double computeAverageScore(Map<String, Object> analysis) {
        double clarity = toDouble(analysis.get("clarityScore"));
        double tech = toDouble(analysis.get("technicalAccuracyScore"));
        double comm = toDouble(analysis.get("communicationScore"));
        double conf = toDouble(analysis.get("confidenceScore"));
        return (clarity + tech + comm + conf) / 4.0;
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    private String mapScoreToGrade(double score) {
        if (score >= 8.5) return "A";
        if (score >= 7.0) return "B";
        if (score >= 5.5) return "C";
        if (score >= 4.0) return "D";
        return "F";
    }

    private List<String> identifyStrengths(Map<String, Object> analysis) {
        List<String> strengths = new ArrayList<>();
        if (toDouble(analysis.get("clarityScore")) >= 7.0) strengths.add("Clear and structured response");
        if (toDouble(analysis.get("technicalAccuracyScore")) >= 7.0) strengths.add("Strong technical vocabulary");
        if (toDouble(analysis.get("communicationScore")) >= 7.0) strengths.add("Good answer length and articulation");
        if (toDouble(analysis.get("confidenceScore")) >= 7.0) strengths.add("Confident delivery with minimal filler words");
        return strengths;
    }

    private List<String> identifyImprovements(Map<String, Object> analysis) {
        List<String> improvements = new ArrayList<>();
        if (toDouble(analysis.get("clarityScore")) < 5.0) improvements.add("Add more structure: use 'First...', 'Then...', 'Finally...'");
        if (toDouble(analysis.get("technicalAccuracyScore")) < 5.0) improvements.add("Include specific technical terms relevant to the question");
        if (toDouble(analysis.get("communicationScore")) < 5.0) improvements.add("Expand your answer — aim for 80–150 words per response");
        if (toDouble(analysis.get("confidenceScore")) < 5.0) improvements.add("Reduce filler words like 'um', 'like', 'you know'");
        return improvements;
    }

    private String generateTip(String answer) {
        if (answer.split("\\s+").length < 40) return "Try to elaborate more with concrete examples from past experience.";
        if (!answer.contains("example") && !answer.contains("instance")) return "Support your answer with a real-world example for added impact.";
        return "Good length — ensure every sentence adds value and avoids repetition.";
    }

    private String buildRecommendation(double avg) {
        if (avg >= 8.0) return "Strong candidate — recommend advancing to technical round.";
        if (avg >= 6.0) return "Moderate performance — consider a follow-up round to verify depth.";
        if (avg >= 4.0) return "Below expectations — recommend additional screening before proceeding.";
        return "Does not meet minimum threshold — recommend declining at this stage.";
    }

    private Map<String, Object> buildZeroScores() {
        Map<String, Object> z = new LinkedHashMap<>();
        z.put("clarityScore", 0.0);
        z.put("technicalAccuracyScore", 0.0);
        z.put("communicationScore", 0.0);
        z.put("confidenceScore", 0.0);
        return z;
    }
}
