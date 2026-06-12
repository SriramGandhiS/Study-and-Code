package com.javino.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * DocumentController
 *
 * REST controller for document upload, text-based analysis, job status retrieval,
 * and batch cleanup for AI authenticity analysis.
 *
 * Add:    feat: add document controller for batch authenticity analysis API
 * Delete: refactor: merge DocumentController into AnalysisController
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    // In-memory job store — replace with Redis/DB in production
    private final Map<String, Map<String, Object>> jobStore = new LinkedHashMap<>();

    /**
     * POST /api/documents/analyze
     * Accepts a multipart file upload (PDF, DOCX, TXT) and queues an analysis job.
     *
     * @param file       the uploaded document
     * @param language   optional language hint (default: "en")
     * @param mode       analysis mode: "standard" | "deep" (default: "standard")
     * @return job metadata with jobId for polling
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeDocument(
            @RequestPart("file")                           MultipartFile file,
            @RequestParam(value = "language", defaultValue = "en")   String language,
            @RequestParam(value = "mode",     defaultValue = "standard") String mode) {

        log.info("Document upload: name={}, size={} bytes, lang={}, mode={}",
                file.getOriginalFilename(), file.getSize(), language, mode);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("File is empty"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!Set.of("pdf", "docx", "txt").contains(ext)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(errorBody("Unsupported file type: " + ext + ". Allowed: pdf, docx, txt"));
        }

        String jobId = UUID.randomUUID().toString();
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobId",       jobId);
        job.put("filename",    file.getOriginalFilename());
        job.put("fileSize",    file.getSize());
        job.put("language",    language);
        job.put("mode",        mode);
        job.put("status",      "QUEUED");
        job.put("queuedAt",    System.currentTimeMillis());
        job.put("result",      null);

        jobStore.put(jobId, job);
        log.info("Job {} queued for file '{}'", jobId, file.getOriginalFilename());

        // In a real system, submit to async ExecutorService / message queue here
        simulateProcessing(jobId);

        return ResponseEntity.accepted().body(job);
    }

    /**
     * POST /api/documents/analyze/text
     * Accepts raw text for immediate authenticity analysis.
     *
     * @param body request body: {"text": "...", "title": "...", "mode": "standard|deep"}
     * @return analysis result map
     */
    @PostMapping("/analyze/text")
    public ResponseEntity<Map<String, Object>> analyzeText(@RequestBody Map<String, String> body) {
        String text  = body.getOrDefault("text", "");
        String title = body.getOrDefault("title", "Untitled");
        String mode  = body.getOrDefault("mode", "standard");

        log.info("Text analysis request: title='{}', textLength={}, mode={}", title, text.length(), mode);

        if (text.isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("Text field must not be empty"));
        }
        if (text.length() < 50) {
            return ResponseEntity.badRequest().body(errorBody("Text too short — minimum 50 characters required"));
        }

        String jobId = UUID.randomUUID().toString();
        Map<String, Object> result = buildTextAnalysisResult(jobId, title, text, mode);
        jobStore.put(jobId, result);
        log.info("Text analysis complete for jobId={}", jobId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/documents/report/{jobId}
     * Returns the analysis report for a given job ID.
     *
     * @param jobId the job identifier returned during upload
     * @return job status and result if complete
     */
    @GetMapping("/report/{jobId}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable String jobId) {
        log.info("Report requested for jobId={}", jobId);
        if (!jobStore.containsKey(jobId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("No job found with id: " + jobId));
        }
        return ResponseEntity.ok(jobStore.get(jobId));
    }

    /**
     * DELETE /api/documents/{jobId}
     * Removes the job record and any associated stored data.
     *
     * @param jobId the job identifier to delete
     * @return confirmation message
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable String jobId) {
        log.info("Delete requested for jobId={}", jobId);
        if (!jobStore.containsKey(jobId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("No job found with id: " + jobId));
        }
        jobStore.remove(jobId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Job " + jobId + " deleted successfully");
        resp.put("jobId",   jobId);
        return ResponseEntity.ok(resp);
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private void simulateProcessing(String jobId) {
        // Simulate async processing by immediately marking as COMPLETED
        // Replace with @Async + actual model invocation in production
        Map<String, Object> job = jobStore.get(jobId);
        if (job == null) return;
        job.put("status",      "COMPLETED");
        job.put("completedAt", System.currentTimeMillis());
        job.put("result", Map.of(
                "aiScore",      "72.4%",
                "verdict",      "HIGH",
                "verdictLabel", "High Probability of AI Generation",
                "models",       List.of("GPT-4o", "Gemini 1.5 Flash")
        ));
    }

    private Map<String, Object> buildTextAnalysisResult(String jobId, String title, String text, String mode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("jobId",       jobId);
        r.put("title",       title);
        r.put("textLength",  text.length());
        r.put("wordCount",   text.split("\\s+").length);
        r.put("mode",        mode);
        r.put("status",      "COMPLETED");
        r.put("analyzedAt",  System.currentTimeMillis());
        // Placeholder scores — wire to ModelRouterService in production
        r.put("aiScore",     "68.0%");
        r.put("verdict",     "MODERATE");
        r.put("verdictLabel","Moderate AI Influence Detected");
        return r;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error",     true);
        err.put("message",   message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
