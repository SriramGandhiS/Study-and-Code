package com.roi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ClientProfileService
 * Manages ROI client profiles in memory using a thread-safe ConcurrentHashMap.
 * Profiles are created with UUID IDs and support CRUD + search operations.
 */
@Service
public class ClientProfileService {

    private static final Logger log = LoggerFactory.getLogger(ClientProfileService.class);

    private static final List<String> REQUIRED_FIELDS = Arrays.asList("name", "contact");

    /** clientId -> profile map */
    private final ConcurrentHashMap<String, Map<String, Object>> profileStore = new ConcurrentHashMap<>();

    /**
     * Creates a new client profile. Required fields: name, contact.
     *
     * @param data map of string fields from the request
     * @return the created profile including generated id and metadata
     * @throws IllegalArgumentException if required fields are missing
     */
    public Map<String, Object> createProfile(Map<String, String> data) {
        for (String field : REQUIRED_FIELDS) {
            String val = data == null ? null : data.get(field);
            if (val == null || val.isBlank()) {
                throw new IllegalArgumentException("Required field missing: " + field);
            }
        }

        String clientId = UUID.randomUUID().toString();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id",        clientId);
        profile.put("name",      data.get("name").trim());
        profile.put("contact",   data.get("contact").trim());
        profile.put("email",     data.getOrDefault("email", ""));
        profile.put("address",   data.getOrDefault("address", ""));
        profile.put("notes",     data.getOrDefault("notes", ""));
        profile.put("active",    true);
        profile.put("createdAt", LocalDateTime.now().toString());
        profile.put("updatedAt", LocalDateTime.now().toString());

        profileStore.put(clientId, profile);
        log.info("[ClientProfile] Created profile {} for '{}'", clientId, profile.get("name"));
        return Collections.unmodifiableMap(profile);
    }

    /**
     * Updates mutable fields of an existing profile.
     *
     * @param clientId the profile's UUID
     * @param updates  map of fields to update (name, email, address, notes)
     * @return the updated profile
     */
    public Map<String, Object> updateProfile(String clientId, Map<String, String> updates) {
        Map<String, Object> profile = requireProfile(clientId);
        Set<String> mutableFields = Set.of("name", "email", "address", "notes", "contact");
        updates.forEach((key, val) -> {
            if (mutableFields.contains(key) && val != null) {
                profile.put(key, val.trim());
            }
        });
        profile.put("updatedAt", LocalDateTime.now().toString());
        log.info("[ClientProfile] Updated profile {}", clientId);
        return Collections.unmodifiableMap(profile);
    }

    /**
     * Returns the profile for a given clientId.
     */
    public Map<String, Object> getProfile(String clientId) {
        return Collections.unmodifiableMap(requireProfile(clientId));
    }

    /**
     * Marks a profile as inactive (soft delete).
     */
    public void deactivateProfile(String clientId) {
        Map<String, Object> profile = requireProfile(clientId);
        profile.put("active",    false);
        profile.put("updatedAt", LocalDateTime.now().toString());
        log.info("[ClientProfile] Deactivated profile {}", clientId);
    }

    /**
     * Case-insensitive search across all active profiles by name.
     */
    public List<Map<String, Object>> searchByName(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();
        String q = name.toLowerCase();
        return profileStore.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.get("active")))
                .filter(p -> p.get("name") != null &&
                             p.get("name").toString().toLowerCase().contains(q))
                .map(Collections::unmodifiableMap)
                .collect(Collectors.toList());
    }

    /**
     * Returns all currently active profiles.
     */
    public List<Map<String, Object>> getAllActive() {
        return profileStore.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.get("active")))
                .map(Collections::unmodifiableMap)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total number of profiles (active + inactive).
     */
    public long getProfileCount() {
        return profileStore.size();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> requireProfile(String clientId) {
        Map<String, Object> profile = profileStore.get(clientId);
        if (profile == null) throw new NoSuchElementException("Client not found: " + clientId);
        return profile;
    }
}
