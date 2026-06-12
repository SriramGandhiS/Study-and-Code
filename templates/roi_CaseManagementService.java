package com.roi.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * CaseManagementService
 * Manages the full lifecycle of legal cases in the ROI Legal platform.
 *
 * States: FILED -> UNDER_REVIEW -> HEARING_SCHEDULED -> DECIDED -> CLOSED
 */
@Service
public class CaseManagementService {

    private static final Logger log = Logger.getLogger(CaseManagementService.class.getName());

    private static final List<String> CASE_TYPES = List.of(
        "CIVIL", "CRIMINAL", "FAMILY", "CORPORATE",
        "INTELLECTUAL_PROPERTY", "LABOUR", "CONSUMER", "CONSTITUTIONAL"
    );

    private static final Map<String, String> STATUS_TRANSITIONS = Map.of(
        "FILED",             "UNDER_REVIEW",
        "UNDER_REVIEW",      "HEARING_SCHEDULED",
        "HEARING_SCHEDULED", "DECIDED",
        "DECIDED",           "CLOSED"
    );

    /**
     * Create a new legal case record.
     */
    public Map<String, Object> createCase(String clientId, String caseType, String description) {
        String caseId = "ROI-" + System.currentTimeMillis();

        if (!CASE_TYPES.contains(caseType.toUpperCase())) {
            log.warning("Unknown case type: " + caseType);
        }

        Map<String, Object> legalCase = new LinkedHashMap<>();
        legalCase.put("caseId",        caseId);
        legalCase.put("clientId",      clientId);
        legalCase.put("caseType",      caseType.toUpperCase());
        legalCase.put("description",   description);
        legalCase.put("status",        "FILED");
        legalCase.put("filedDate",     LocalDate.now().toString());
        legalCase.put("nextHearing",   LocalDate.now().plusDays(30).toString());
        legalCase.put("assignedJudge", null);
        legalCase.put("documents",     new ArrayList<>());

        log.info("Case created: " + caseId + " type=" + caseType);
        return legalCase;
    }

    /**
     * Advance the case to the next status.
     */
    public String advanceCaseStatus(String currentStatus) {
        return STATUS_TRANSITIONS.getOrDefault(currentStatus.toUpperCase(), currentStatus);
    }

    /**
     * Dashboard summary statistics.
     */
    public Map<String, Object> getCaseSummary(List<Map<String, Object>> cases) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byType   = new LinkedHashMap<>();

        for (Map<String, Object> c : cases) {
            String status = (String) c.getOrDefault("status", "UNKNOWN");
            String type   = (String) c.getOrDefault("caseType", "UNKNOWN");
            byStatus.merge(status, 1L, Long::sum);
            byType.merge(type, 1L, Long::sum);
        }

        long active = cases.stream()
            .filter(c -> !"CLOSED".equals(c.get("status")))
            .count();

        return Map.of(
            "totalCases",  cases.size(),
            "activeCases", active,
            "byStatus",    byStatus,
            "byType",      byType
        );
    }

    public boolean isCaseTypeValid(String type) {
        return CASE_TYPES.contains(type.toUpperCase());
    }

    public List<String> getSupportedCaseTypes() { return CASE_TYPES; }
}
