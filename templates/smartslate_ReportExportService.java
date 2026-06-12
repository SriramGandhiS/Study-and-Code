package com.smartslate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ReportExportService
 * Provides CSV and JSON export capabilities for report data.
 * Handles proper CSV escaping including commas, quotes, and newlines.
 */
@Service
public class ReportExportService {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/smartslate-reports/";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ReportExportService() {
        // Ensure temp directory exists
        new File(TEMP_DIR).mkdirs();
    }

    /**
     * Exports a list of row maps to CSV string using provided column order.
     */
    public String exportAsCsv(List<Map<String, Object>> data, List<String> columns) {
        if (data == null || data.isEmpty()) return buildCsvHeader(columns) + "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(buildCsvHeader(columns)).append("\n");
        for (Map<String, Object> row : data) {
            sb.append(buildCsvRow(row, columns)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Exports data as a formatted JSON string.
     */
    public String exportAsJson(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < data.size(); i++) {
            sb.append("  {\n");
            Map<String, Object> row = data.get(i);
            List<String> keys = new ArrayList<>(row.keySet());
            for (int k = 0; k < keys.size(); k++) {
                String key = keys.get(k);
                Object val = row.get(key);
                String jsonVal = val == null ? "null"
                        : (val instanceof Number || val instanceof Boolean)
                        ? val.toString()
                        : "\"" + val.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                sb.append("    \"").append(key).append("\": ").append(jsonVal);
                if (k < keys.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }");
            if (i < data.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Generates a timestamped filename for downloads.
     */
    public String generateFilename(String prefix) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "report" : prefix.replaceAll("[^a-zA-Z0-9_-]", "_");
        return safePrefix + "_" + LocalDateTime.now().format(TS_FMT);
    }

    /**
     * Builds a CSV header row from column names.
     */
    public String buildCsvHeader(List<String> columns) {
        if (columns == null || columns.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            sb.append(escapeCsvField(columns.get(i)));
            if (i < columns.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * Builds a single CSV data row. Handles null, commas, quotes, and newlines.
     */
    public String buildCsvRow(Map<String, Object> row, List<String> columns) {
        if (row == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            Object val = row.get(columns.get(i));
            String strVal = val == null ? "" : val.toString();
            sb.append(escapeCsvField(strVal));
            if (i < columns.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV field — wraps in quotes if it contains comma, quote, or newline.
     */
    private String escapeCsvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Writes content string to a temp file and returns the File reference.
     */
    public File writeToTempFile(String content, String filename) throws IOException {
        Path filePath = Paths.get(TEMP_DIR, filename);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, java.nio.charset.StandardCharsets.UTF_8);
        return filePath.toFile();
    }
}
