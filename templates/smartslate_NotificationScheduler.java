package com.smartslate.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NotificationScheduler
 * Scheduled tasks for monitoring error rates, memory alerts,
 * daily digests, and housekeeping of old notification records.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${smartslate.alert.errorRateThreshold:5.0}")
    private double errorRateThreshold;

    @Value("${smartslate.alert.cpuThreshold:80.0}")
    private double cpuThreshold;

    @Value("${smartslate.alert.memoryThresholdMb:512}")
    private long memoryThresholdMb;

    /** In-memory notification store: id -> {type, message, createdAt} */
    private final ConcurrentHashMap<String, Map<String, Object>> notificationStore = new ConcurrentHashMap<>();
    private final AtomicInteger notifCounter = new AtomicInteger(0);

    /**
     * Checks high error rate every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    public void checkHighErrorRate() {
        double currentErrorRate = fetchCurrentErrorRate();
        if (currentErrorRate > errorRateThreshold) {
            String msg = String.format("[ERROR ALERT] Error rate %.2f%% exceeds threshold %.2f%% at %s",
                    currentErrorRate, errorRateThreshold, LocalDateTime.now().format(FMT));
            log.warn(msg);
            storeNotification("ERROR_RATE", msg);
            dispatchAlert("admin", msg);
        } else {
            log.info("[HealthCheck] Error rate OK: {:.2f}%", currentErrorRate);
        }
    }

    /**
     * Sends a daily digest at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyDigest() {
        String now = LocalDateTime.now().format(FMT);
        int totalNotifs = notifCounter.get();
        String msg = String.format(
                "[DAILY DIGEST] %s — Total notifications since startup: %d. System operational.",
                now, totalNotifs);
        log.info(msg);
        dispatchAlert("team-channel", msg);
        // Reset daily counter
        notifCounter.set(0);
    }

    /**
     * Checks available memory every 2 minutes.
     */
    @Scheduled(fixedRate = 120_000)
    public void checkMemoryAlerts() {
        Runtime rt = Runtime.getRuntime();
        long freeMemMb = rt.freeMemory() / (1024 * 1024);
        long totalMemMb = rt.totalMemory() / (1024 * 1024);
        long usedMemMb = totalMemMb - freeMemMb;

        if (usedMemMb > memoryThresholdMb) {
            String msg = String.format("[MEMORY ALERT] Used memory %dMB exceeds threshold %dMB at %s",
                    usedMemMb, memoryThresholdMb, LocalDateTime.now().format(FMT));
            log.warn(msg);
            storeNotification("MEMORY", msg);
            dispatchAlert("ops-team", msg);
        } else {
            log.debug("[MemCheck] Memory OK: {}MB used / {}MB total", usedMemMb, totalMemMb);
        }
    }

    /**
     * Cleans notifications older than 7 days at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanOldNotifications() {
        int before = notificationStore.size();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<String> toRemove = new ArrayList<>();
        notificationStore.forEach((id, data) -> {
            LocalDateTime created = (LocalDateTime) data.get("createdAt");
            if (created != null && created.isBefore(cutoff)) {
                toRemove.add(id);
            }
        });
        toRemove.forEach(notificationStore::remove);
        log.info("[Cleanup] Removed {} stale notifications (older than 7 days). Remaining: {}",
                toRemove.size(), notificationStore.size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private double fetchCurrentErrorRate() {
        // Placeholder — in production, query metrics store or Micrometer registry
        return Math.random() * 10;
    }

    private void storeNotification(String type, String message) {
        String id = "NOTIF-" + notifCounter.incrementAndGet();
        notificationStore.put(id, Map.of(
                "type", type,
                "message", message,
                "createdAt", LocalDateTime.now()
        ));
    }

    private void dispatchAlert(String channel, String message) {
        // Placeholder — integrate with email/Slack/webhook in production
        log.info("[ALERT -> {}] {}", channel, message);
    }
}
