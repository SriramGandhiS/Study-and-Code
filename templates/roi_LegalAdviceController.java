package com.roi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * LegalAdviceController
 * REST endpoints for the ROI legal advice module.
 * Provides AI-assisted legal question answering, category listing,
 * disclaimer retrieval, and similar case matching.
 */
@RestController
@RequestMapping("/api/legal")
public class LegalAdviceController {

    private static final String DISCLAIMER =
            "IMPORTANT LEGAL DISCLAIMER: The information provided by this platform is for general " +
            "informational and educational purposes only. It does not constitute legal advice, and no " +
            "attorney-client relationship is formed by using this service. Laws vary by jurisdiction; " +
            "always consult a qualified legal professional for advice specific to your situation. " +
            "ROI Legal Platform expressly disclaims all liability for actions taken based on content provided.";

    private static final List<String> LEGAL_CATEGORIES = Arrays.asList(
            "Family Law",
            "Criminal Law",
            "Property & Real Estate",
            "Employment & Labour",
            "Consumer Protection",
            "Corporate & Business",
            "Intellectual Property",
            "Civil Litigation",
            "Constitutional & Human Rights"
    );

    /**
     * POST /api/legal/ask
     * Accepts a legal question, jurisdiction, and category.
     * Returns a structured response with advice and confidence.
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askLegalQuestion(@RequestBody Map<String, String> request) {
        String question    = request.getOrDefault("question", "").trim();
        String jurisdiction = request.getOrDefault("jurisdiction", "INDIA").trim().toUpperCase();
        String category    = request.getOrDefault("category", "General").trim();

        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Question field is required",
                    "code",  "MISSING_QUESTION"
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question",     question);
        response.put("jurisdiction", jurisdiction);
        response.put("category",     category);
        response.put("advice",       generateAdvicePlaceholder(question, jurisdiction, category));
        response.put("disclaimer",   DISCLAIMER);
        response.put("confidence",   "MEDIUM");
        response.put("suggestProfessional", true);
        response.put("answeredAt",   java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/legal/categories
     * Returns the list of supported legal categories.
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getLegalCategories() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", LEGAL_CATEGORIES);
        result.put("count",      LEGAL_CATEGORIES.size());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/legal/disclaimer
     * Returns the full legal disclaimer text.
     */
    @GetMapping("/disclaimer")
    public ResponseEntity<Map<String, Object>> getDisclaimer() {
        return ResponseEntity.ok(Map.of(
                "disclaimer", DISCLAIMER,
                "version",    "1.0",
                "effective",  "2024-01-01"
        ));
    }

    /**
     * POST /api/legal/similar-cases
     * Accepts a case description and limit, returns mock similar cases.
     */
    @PostMapping("/similar-cases")
    public ResponseEntity<Map<String, Object>> findSimilarCases(@RequestBody Map<String, Object> request) {
        String description = String.valueOf(request.getOrDefault("description", "")).trim();
        int limit = Integer.parseInt(String.valueOf(request.getOrDefault("limit", "5")));
        if (limit < 1 || limit > 20) limit = 5;

        if (description.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "description is required"));
        }

        List<Map<String, Object>> cases = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("caseId",    "CASE-2024-" + String.format("%04d", i));
            c.put("title",     "Similar Case #" + i);
            c.put("court",     i % 2 == 0 ? "High Court" : "District Court");
            c.put("year",      2020 + (i % 5));
            c.put("relevance", Math.round((1.0 - i * 0.08) * 100) + "%");
            cases.add(c);
        }

        return ResponseEntity.ok(Map.of(
                "similarCases", cases,
                "total",        cases.size(),
                "disclaimer",   DISCLAIMER
        ));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String generateAdvicePlaceholder(String question, String jurisdiction, String category) {
        return String.format(
                "Based on %s law under the category of '%s': Your question has been received. " +
                "A licensed attorney will review and provide detailed guidance. " +
                "General note: '%s' — please consult a qualified professional for binding advice.",
                jurisdiction, category, question.length() > 80 ? question.substring(0, 80) + "..." : question
        );
    }
}
