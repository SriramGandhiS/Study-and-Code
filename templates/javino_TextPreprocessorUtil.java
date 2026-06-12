package com.javino.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.*;

/**
 * TextPreprocessorUtil
 *
 * Utility class providing text normalization, sanitization, and readability
 * analysis functions used before feeding text to AI analysis models.
 *
 * Add:    feat: add text preprocessor utility for input normalization
 * Delete: refactor: move preprocessing into AnalysisRequestFilter
 */
public final class TextPreprocessorUtil {

    private static final Logger log = LoggerFactory.getLogger(TextPreprocessorUtil.class);

    private static final Pattern HTML_TAG_PATTERN    = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN  = Pattern.compile("\\s{2,}");
    private static final Pattern SENTENCE_SPLITTER   = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern LANGUAGE_DETECT_EN  = Pattern.compile("\\b(the|is|are|was|were|has|have|had|and|but|for|with|from)\\b", Pattern.CASE_INSENSITIVE);

    /** Average English chars per token (rough GPT/Llama approximation). */
    private static final double AVG_CHARS_PER_TOKEN = 4.0;

    private TextPreprocessorUtil() {}

    /**
     * Strips all HTML tags from the input string.
     *
     * @param html text that may contain HTML markup
     * @return plain text without any HTML tags
     */
    public static String removeHtmlTags(String html) {
        if (html == null || html.isBlank()) return "";
        String cleaned = HTML_TAG_PATTERN.matcher(html).replaceAll(" ");
        // Also decode common HTML entities
        cleaned = cleaned
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&nbsp;", " ");
        log.debug("removeHtmlTags: input {} chars → output {} chars", html.length(), cleaned.length());
        return cleaned.strip();
    }

    /**
     * Collapses multiple consecutive whitespace characters into a single space
     * and trims leading/trailing whitespace.
     *
     * @param text raw text
     * @return normalized text
     */
    public static String normalizeWhitespace(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = WHITESPACE_PATTERN.matcher(text.replace("\t", " ")
                .replace("\r\n", " ")
                .replace("\r", " ")
                .replace("\n", " "))
                .replaceAll(" ")
                .strip();
        log.debug("normalizeWhitespace: {} → {} chars", text.length(), normalized.length());
        return normalized;
    }

    /**
     * Truncates text to approximately {@code maxTokens} tokens by character estimation.
     * Truncation happens at a word boundary to avoid cutting mid-word.
     *
     * @param text      input text
     * @param maxTokens maximum token budget
     * @return truncated text (with ellipsis if truncated)
     */
    public static String truncateToTokenLimit(String text, int maxTokens) {
        if (text == null) return "";
        int charLimit = (int) (maxTokens * AVG_CHARS_PER_TOKEN);
        if (text.length() <= charLimit) return text;

        // Find last space before charLimit to cut at word boundary
        int cutIndex = text.lastIndexOf(' ', charLimit);
        if (cutIndex == -1) cutIndex = charLimit;
        String truncated = text.substring(0, cutIndex) + "…";
        log.info("Text truncated from {} chars to {} chars (maxTokens={})", text.length(), truncated.length(), maxTokens);
        return truncated;
    }

    /**
     * Simple heuristic language detection. Returns "en" for English,
     * "unknown" otherwise. Not production-grade — use LangDetect for real use.
     *
     * @param text input text
     * @return ISO 639-1 language code or "unknown"
     */
    public static String detectLanguage(String text) {
        if (text == null || text.length() < 20) return "unknown";
        Matcher m = LANGUAGE_DETECT_EN.matcher(text);
        int hits = 0;
        while (m.find()) hits++;
        String[] words = text.split("\\s+");
        double ratio = words.length == 0 ? 0 : (double) hits / words.length;
        String lang = ratio > 0.05 ? "en" : "unknown";
        log.debug("detectLanguage: ratio={} → {}", ratio, lang);
        return lang;
    }

    /**
     * Splits text into individual sentences using punctuation as delimiters.
     *
     * @param text input text
     * @return list of non-empty sentence strings
     */
    public static List<String> extractSentences(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String[] parts = SENTENCE_SPLITTER.split(text.strip());
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) sentences.add(trimmed);
        }
        log.debug("extractSentences: found {} sentences", sentences.size());
        return sentences;
    }

    /**
     * Computes a Flesch-Kincaid-inspired readability score (0–100).
     * Higher = easier to read.
     * Formula: 206.835 - 1.015*(words/sentences) - 84.6*(syllables/words)
     *
     * @param text input text
     * @return readability score clamped to [0, 100]
     */
    public static double computeReadabilityScore(String text) {
        if (text == null || text.isBlank()) return 0.0;
        List<String> sentences = extractSentences(text);
        String[] words = text.split("\\s+");
        int totalWords     = words.length;
        int totalSentences = Math.max(1, sentences.size());
        int totalSyllables = Arrays.stream(words).mapToInt(TextPreprocessorUtil::countSyllables).sum();

        double asl  = (double) totalWords / totalSentences;        // avg sentence length
        double asw  = (double) totalSyllables / Math.max(1, totalWords); // avg syllables per word
        double score = 206.835 - (1.015 * asl) - (84.6 * asw);
        double clamped = Math.max(0.0, Math.min(100.0, score));
        log.debug("readabilityScore={} (words={}, sentences={}, syllables={})", clamped, totalWords, totalSentences, totalSyllables);
        return Math.round(clamped * 10.0) / 10.0;
    }

    /**
     * Applies the full preprocessing pipeline: strip HTML → normalize whitespace →
     * truncate to token limit → return cleaned text.
     *
     * @param raw       raw user input (may contain HTML)
     * @param maxTokens token budget for the downstream model
     * @return fully preprocessed text
     */
    public static String preprocess(String raw, int maxTokens) {
        String step1 = removeHtmlTags(raw);
        String step2 = normalizeWhitespace(step1);
        String step3 = truncateToTokenLimit(step2, maxTokens);
        log.info("preprocess complete: {} chars raw → {} chars final", raw == null ? 0 : raw.length(), step3.length());
        return step3;
    }

    // ─── syllable counting heuristic ────────────────────────────────────────

    private static int countSyllables(String word) {
        if (word == null || word.isEmpty()) return 1;
        word = word.toLowerCase().replaceAll("[^a-z]", "");
        if (word.isEmpty()) return 1;
        if (word.endsWith("e")) word = word.substring(0, word.length() - 1);
        Matcher vowelGroups = Pattern.compile("[aeiouy]+").matcher(word);
        int count = 0;
        while (vowelGroups.find()) count++;
        return Math.max(1, count);
    }
}
