package com.roi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * DocumentStorageService
 * Manages legal document storage on the local file system.
 * Files are stored under STORAGE_ROOT with UUID-based names.
 * Maintains a metadata index in memory (could be persisted to DB in production).
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private static final String STORAGE_ROOT = System.getProperty("user.home") + "/roi-documents/";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".docx", ".txt", ".jpg", ".png"
    );

    /** docId -> { caseId, originalName, storedName, path, size, uploadedAt } */
    private final Map<String, Map<String, Object>> metadataIndex = new LinkedHashMap<>();

    public DocumentStorageService() {
        new File(STORAGE_ROOT).mkdirs();
        log.info("[DocumentStorage] Storage root: {}", STORAGE_ROOT);
    }

    /**
     * Stores an uploaded document and returns metadata.
     */
    public Map<String, Object> storeDocument(String caseId, MultipartFile file) throws IOException {
        if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId required");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File must not be empty");

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        if (!validateFileType(originalName)) {
            throw new IllegalArgumentException("File type not allowed: " + originalName);
        }

        String ext      = getExtension(originalName);
        String docId    = UUID.randomUUID().toString();
        String stored   = docId + ext;
        Path   dir      = Paths.get(STORAGE_ROOT, caseId);
        Files.createDirectories(dir);
        Path   dest     = dir.resolve(stored);

        file.transferTo(dest.toFile());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("docId",        docId);
        meta.put("caseId",       caseId);
        meta.put("originalName", originalName);
        meta.put("storedName",   stored);
        meta.put("path",         dest.toString());
        meta.put("sizeByes",     file.getSize());
        meta.put("uploadedAt",   java.time.LocalDateTime.now().toString());

        metadataIndex.put(docId, meta);
        log.info("[DocumentStorage] Stored {} as {} for case {}", originalName, stored, caseId);
        return meta;
    }

    /**
     * Retrieves the File object for a given docId.
     */
    public File retrieveDocument(String docId) {
        Map<String, Object> meta = metadataIndex.get(docId);
        if (meta == null) throw new NoSuchElementException("Document not found: " + docId);
        File f = new File((String) meta.get("path"));
        if (!f.exists()) throw new NoSuchElementException("File missing on disk for docId: " + docId);
        return f;
    }

    /**
     * Deletes a document from disk and index. Returns true if successful.
     */
    public boolean deleteDocument(String docId) {
        Map<String, Object> meta = metadataIndex.remove(docId);
        if (meta == null) return false;
        File f = new File((String) meta.get("path"));
        boolean deleted = f.delete();
        log.info("[DocumentStorage] Deleted {} — success={}", docId, deleted);
        return deleted;
    }

    /**
     * Lists all documents for a given caseId.
     */
    public List<Map<String, Object>> listDocuments(String caseId) {
        List<Map<String, Object>> result = new ArrayList<>();
        metadataIndex.forEach((id, meta) -> {
            if (caseId.equals(meta.get("caseId"))) result.add(meta);
        });
        return result;
    }

    /**
     * Validates that the filename has an allowed extension.
     */
    public boolean validateFileType(String filename) {
        if (filename == null) return false;
        String ext = getExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    /**
     * Returns aggregate storage stats.
     */
    public Map<String, Object> getStorageStats() {
        long totalFiles   = metadataIndex.size();
        long totalSizeBytes = metadataIndex.values().stream()
                .mapToLong(m -> {
                    Object s = m.get("sizeByes");
                    return s instanceof Number ? ((Number) s).longValue() : 0L;
                }).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFiles",   totalFiles);
        stats.put("totalSizeKb",  totalSizeBytes / 1024);
        stats.put("storageRoot",  STORAGE_ROOT);
        stats.put("snapshotAt",   java.time.LocalDateTime.now().toString());
        return stats;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}
