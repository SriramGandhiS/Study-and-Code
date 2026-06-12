package com.javino.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * ModelRouterService
 *
 * Routes text to different AI models based on input length and content type:
 *   - Short  (<200 chars)  → Llama 3 (fast, cheap)
 *   - Medium (200–800 chars) → Gemini 1.5 Flash (balanced)
 *   - Long   (>800 chars)  → GPT-4o (deep analysis)
 *
 * Add:    feat: add model router service for adaptive LLM selection
 * Delete: refactor: consolidate routing logic into AnalysisPipeline
 */
@Service
public class ModelRouterService {

    private static final Logger log = LoggerFactory.getLogger(ModelRouterService.class);

    public enum ModelTarget { LLAMA, GEMINI, GPT4O }

    private static final int SHORT_THRESHOLD  = 200;
    private static final int MEDIUM_THRESHOLD = 800;

    private static final Map<ModelTarget, String> ENDPOINT_MAP = new EnumMap<>(ModelTarget.class);

    static {
        ENDPOINT_MAP.put(ModelTarget.LLAMA,  "https://api.together.ai/v1/chat/completions");
        ENDPOINT_MAP.put(ModelTarget.GEMINI, "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent");
        ENDPOINT_MAP.put(ModelTarget.GPT4O,  "https://api.openai.com/v1/chat/completions");
    }

    private static final Map<ModelTarget, String> MODEL_NAMES = new EnumMap<>(ModelTarget.class);

    static {
        MODEL_NAMES.put(ModelTarget.LLAMA,  "meta-llama/Llama-3-8b-chat-hf");
        MODEL_NAMES.put(ModelTarget.GEMINI, "gemini-1.5-flash");
        MODEL_NAMES.put(ModelTarget.GPT4O,  "gpt-4o");
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Main routing entry point. Selects model, builds payload, calls endpoint, parses response.
     *
     * @param text       the text to analyze
     * @param systemPrompt system-level instruction for the model
     * @return parsed response string from the chosen model
     */
    public String route(String text, String systemPrompt) {
        if (text == null || text.isBlank()) {
            log.warn("Empty text passed to router");
            return "";
        }
        ModelTarget target = selectModel(text);
        log.info("Routing {} chars → {}", text.length(), target);
        String payload = buildPayload(target, text, systemPrompt);
        String rawResponse = callEndpoint(target, payload);
        return parseResponse(target, rawResponse);
    }

    /**
     * Selects the appropriate model based on text character count.
     *
     * @param text input text
     * @return ModelTarget enum value
     */
    public ModelTarget selectModel(String text) {
        int len = text == null ? 0 : text.length();
        if (len < SHORT_THRESHOLD)  return ModelTarget.LLAMA;
        if (len < MEDIUM_THRESHOLD) return ModelTarget.GEMINI;
        return ModelTarget.GPT4O;
    }

    /**
     * Constructs a JSON payload appropriate for the selected model's API format.
     *
     * @param target       the model to target
     * @param userText     user message content
     * @param systemPrompt system instruction
     * @return JSON string payload
     */
    public String buildPayload(ModelTarget target, String userText, String systemPrompt) {
        String safeSystem = systemPrompt == null ? "You are a helpful AI assistant." : systemPrompt;
        String safeText   = userText.replace("\"", "\\\"").replace("\n", "\\n");
        String safeSystem2 = safeSystem.replace("\"", "\\\"");

        if (target == ModelTarget.GEMINI) {
            // Gemini uses a different request structure
            return String.format("""
                {
                  "contents": [{"parts": [{"text": "%s\\n\\n%s"}]}],
                  "generationConfig": {"maxOutputTokens": 1024, "temperature": 0.4}
                }
                """, safeSystem2, safeText);
        }
        // OpenAI-compatible format for Llama (Together AI) and GPT-4o
        return String.format("""
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user",   "content": "%s"}
              ],
              "temperature": 0.4,
              "max_tokens": 1024
            }
            """, MODEL_NAMES.get(target), safeSystem2, safeText);
    }

    /**
     * Performs the HTTP POST to the selected model's endpoint.
     * API keys are expected via environment variables at runtime.
     *
     * @param target  model target
     * @param payload JSON payload string
     * @return raw response body string
     */
    private String callEndpoint(ModelTarget target, String payload) {
        String url    = ENDPOINT_MAP.get(target);
        String apiKey = resolveApiKey(target);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Model {} responded with HTTP {}", target, response.statusCode());
            if (response.statusCode() != 200) {
                log.error("Non-200 response from {}: {}", target, response.body());
                return "";
            }
            return response.body();
        } catch (Exception e) {
            log.error("HTTP call to {} failed: {}", target, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Extracts the text content from the raw JSON response body.
     *
     * @param target   the model that was called
     * @param rawJson  raw JSON response
     * @return extracted text content
     */
    public String parseResponse(ModelTarget target, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return "";
        try {
            if (target == ModelTarget.GEMINI) {
                // Gemini: candidates[0].content.parts[0].text
                int textIdx = rawJson.indexOf("\"text\":");
                if (textIdx == -1) return "";
                int start = rawJson.indexOf("\"", textIdx + 7) + 1;
                int end   = rawJson.indexOf("\"", start);
                return rawJson.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
            }
            // OpenAI-compatible: choices[0].message.content
            int contentIdx = rawJson.indexOf("\"content\":");
            if (contentIdx == -1) return "";
            int start = rawJson.indexOf("\"", contentIdx + 10) + 1;
            int end   = rawJson.indexOf("\"", start);
            return rawJson.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            log.error("Failed to parse response from {}: {}", target, e.getMessage());
            return "";
        }
    }

    private String resolveApiKey(ModelTarget target) {
        return switch (target) {
            case LLAMA  -> System.getenv("TOGETHER_API_KEY");
            case GEMINI -> System.getenv("GEMINI_API_KEY");
            case GPT4O  -> System.getenv("OPENAI_API_KEY");
        };
    }
}
