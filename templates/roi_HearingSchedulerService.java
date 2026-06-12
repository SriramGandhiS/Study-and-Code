package com.roi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * HearingSchedulerService
 * Manages court hearing schedules in memory.
 * Supports create, reschedule, cancel, upcoming query, and reminders.
 */
@Service
public class HearingSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(HearingSchedulerService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** hearingId -> hearing detail map */
    private final ConcurrentHashMap<String, Map<String, Object>> hearingStore = new ConcurrentHashMap<>();

    /**
     * Schedules a new hearing for a case.
     *
     * @param caseId    the case identifier
     * @param dateStr   date in yyyy-MM-dd format
     * @param courtRoom the courtroom name/number
     * @return the created hearing map including UUID hearingId
     */
    public Map<String, Object> scheduleHearing(String caseId, String dateStr, String courtRoom) {
        if (caseId == null || caseId.isBlank())    throw new IllegalArgumentException("caseId required");
        if (dateStr == null || dateStr.isBlank())  throw new IllegalArgumentException("dateStr required");
        if (courtRoom == null || courtRoom.isBlank()) throw new IllegalArgumentException("courtRoom required");

        LocalDate hearingDate = parseDate(dateStr);
        if (hearingDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Hearing date must be in the future: " + dateStr);
        }

        String hearingId = UUID.randomUUID().toString();
        Map<String, Object> hearing = new LinkedHashMap<>();
        hearing.put("hearingId",    hearingId);
        hearing.put("caseId",       caseId);
        hearing.put("hearingDate",  hearingDate.toString());
        hearing.put("courtRoom",    courtRoom.trim());
        hearing.put("status",       "SCHEDULED");
        hearing.put("reschedules",  0);
        hearing.put("cancelReason", null);
        hearing.put("createdAt",    LocalDateTime.now().toString());
        hearing.put("updatedAt",    LocalDateTime.now().toString());

        hearingStore.put(hearingId, hearing);
        log.info("[HearingScheduler] Scheduled hearing {} for case {} on {}", hearingId, caseId, hearingDate);
        return Collections.unmodifiableMap(hearing);
    }

    /**
     * Reschedules an existing hearing to a new date with a reason.
     */
    public Map<String, Object> rescheduleHearing(String hearingId, String newDateStr, String reason) {
        Map<String, Object> hearing = requireHearing(hearingId);
        if ("CANCELLED".equals(hearing.get("status"))) {
            throw new IllegalStateException("Cannot reschedule a cancelled hearing: " + hearingId);
        }

        LocalDate newDate = parseDate(newDateStr);
        hearing.put("hearingDate",     newDate.toString());
        hearing.put("status",          "RESCHEDULED");
        hearing.put("rescheduleReason", reason != null ? reason : "");
        hearing.put("reschedules",     ((int) hearing.get("reschedules")) + 1);
        hearing.put("updatedAt",       LocalDateTime.now().toString());

        log.info("[HearingScheduler] Rescheduled {} to {} — reason: {}", hearingId, newDate, reason);
        return Collections.unmodifiableMap(hearing);
    }

    /**
     * Returns all hearings scheduled within the next N days from today.
     */
    public List<Map<String, Object>> getUpcomingHearings(int days) {
        if (days <= 0) throw new IllegalArgumentException("days must be positive");
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(days);

        return hearingStore.values().stream()
                .filter(h -> !"CANCELLED".equals(h.get("status")))
                .filter(h -> {
                    try {
                        LocalDate d = LocalDate.parse((String) h.get("hearingDate"), DATE_FMT);
                        return !d.isBefore(today) && !d.isAfter(until);
                    } catch (Exception e) { return false; }
                })
                .sorted(Comparator.comparing(h -> (String) h.get("hearingDate")))
                .map(Collections::unmodifiableMap)
                .collect(Collectors.toList());
    }

    /**
     * Cancels a hearing with a given reason.
     */
    public void cancelHearing(String hearingId, String reason) {
        Map<String, Object> hearing = requireHearing(hearingId);
        hearing.put("status",       "CANCELLED");
        hearing.put("cancelReason", reason != null ? reason : "No reason provided");
        hearing.put("updatedAt",    LocalDateTime.now().toString());
        log.info("[HearingScheduler] Cancelled hearing {} — reason: {}", hearingId, reason);
    }

    /**
     * Returns all hearings for a specific case.
     */
    public List<Map<String, Object>> getHearingsByCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) return Collections.emptyList();
        return hearingStore.values().stream()
                .filter(h -> caseId.equals(h.get("caseId")))
                .sorted(Comparator.comparing(h -> (String) h.get("hearingDate")))
                .map(Collections::unmodifiableMap)
                .collect(Collectors.toList());
    }

    /**
     * Logs reminders for all hearings scheduled for tomorrow.
     * In production this would integrate with email / SMS / push notifications.
     */
    public void sendReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Map<String, Object>> tomorrowHearings = hearingStore.values().stream()
                .filter(h -> !"CANCELLED".equals(h.get("status")))
                .filter(h -> {
                    try {
                        return LocalDate.parse((String) h.get("hearingDate"), DATE_FMT).equals(tomorrow);
                    } catch (Exception e) { return false; }
                })
                .collect(Collectors.toList());

        if (tomorrowHearings.isEmpty()) {
            log.info("[HearingScheduler] No hearings tomorrow ({}). No reminders needed.", tomorrow);
            return;
        }

        tomorrowHearings.forEach(h ->
            log.info("[REMINDER] Hearing {} for case {} is scheduled TOMORROW ({}) in {}",
                    h.get("hearingId"), h.get("caseId"), tomorrow, h.get("courtRoom"))
        );
        log.info("[HearingScheduler] Sent {} reminder(s) for {}", tomorrowHearings.size(), tomorrow);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> requireHearing(String hearingId) {
        Map<String, Object> h = hearingStore.get(hearingId);
        if (h == null) throw new NoSuchElementException("Hearing not found: " + hearingId);
        return h;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format (expected yyyy-MM-dd): " + dateStr);
        }
    }
}
