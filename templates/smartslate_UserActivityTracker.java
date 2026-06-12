package com.smartslate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * UserActivityTracker
 * Tracks per-user events in memory using a thread-safe ConcurrentHashMap.
 * Each event stores type, detail, and timestamp.
 */
@Service
public class UserActivityTracker {

    private static final Logger log = LoggerFactory.getLogger(UserActivityTracker.class);

    /** userId -> list of event maps */
    private final ConcurrentHashMap<String, List<Map<String, Object>>> activityStore =
            new ConcurrentHashMap<>();

    /**
     * Records a single event for a user.
     *
     * @param userId    the user identifier
     * @param eventType e.g. "LOGIN", "SEARCH", "EXPORT"
     * @param detail    additional context string
     */
    public void recordEvent(String userId, String eventType, String detail) {
        if (userId == null || userId.isBlank()) {
            log.warn("[ActivityTracker] Attempted to record event with blank userId");
            return;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType",  eventType != null ? eventType : "UNKNOWN");
        event.put("detail",     detail != null ? detail : "");
        event.put("recordedAt", LocalDateTime.now().toString());

        activityStore.compute(userId, (k, existing) -> {
            if (existing == null) existing = Collections.synchronizedList(new ArrayList<>());
            existing.add(event);
            return existing;
        });
        log.debug("[ActivityTracker] Recorded {} for user {}", eventType, userId);
    }

    /**
     * Returns all recorded events for a given user, newest last.
     */
    public List<Map<String, Object>> getUserActivity(String userId) {
        return activityStore.getOrDefault(userId, Collections.emptyList());
    }

    /**
     * Returns the top N most active users sorted by event count descending.
     */
    public Map<String, Long> getTopActiveUsers(int limit) {
        return activityStore.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size(),
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
    }

    /**
     * Returns the total event count for a specific user.
     */
    public long getEventCount(String userId) {
        List<Map<String, Object>> events = activityStore.get(userId);
        return events == null ? 0L : events.size();
    }

    /**
     * Removes events older than the specified number of days for all users.
     */
    public void clearOldEvents(int daysOld) {
        if (daysOld <= 0) throw new IllegalArgumentException("daysOld must be positive");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        int[] removed = {0};

        activityStore.forEach((userId, events) -> {
            int before = events.size();
            events.removeIf(evt -> {
                try {
                    LocalDateTime ts = LocalDateTime.parse((String) evt.get("recordedAt"));
                    return ts.isBefore(cutoff);
                } catch (Exception e) {
                    return false;
                }
            });
            removed[0] += (before - events.size());
        });

        // Remove users with no remaining events
        activityStore.entrySet().removeIf(e -> e.getValue().isEmpty());
        log.info("[ActivityTracker] Cleared {} events older than {} days", removed[0], daysOld);
    }

    /**
     * Returns a summary map: total users, total events, top user.
     */
    public Map<String, Object> getSummary() {
        long totalUsers  = activityStore.size();
        long totalEvents = activityStore.values().stream().mapToLong(List::size).sum();

        String topUser = activityStore.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers",  totalUsers);
        summary.put("totalEvents", totalEvents);
        summary.put("topUser",     topUser);
        summary.put("snapshotAt",  LocalDateTime.now().toString());
        return summary;
    }
}
