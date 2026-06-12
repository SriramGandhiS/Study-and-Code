package com.hiresense.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * AudioTranscriptionService
 * Handles audio file upload and transcription via Groq Whisper API.
 * Supports: .mp3, .wav, .webm, .m4a
 */
@Service
public class AudioTranscriptionService {

    private static final Logger log = Logger.getLogger(AudioTranscriptionService.class.getName());

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.transcription.model:whisper-large-v3}")
    private String model;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final int TIMEOUT_MS  = 60_000;

    /**
     * Transcribe an uploaded audio file using Groq Whisper.
     * @param audio the multipart audio file
     * @return transcribed text or empty string on failure
     */
    public String transcribe(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            log.warning("Empty audio file received - skipping transcription");
            return "";
        }
        try {
            File tempFile = File.createTempFile("audio_", "_" + audio.getOriginalFilename());
            audio.transferTo(tempFile);
            String result = callGroqWhisper(tempFile);
            tempFile.delete();
            return result;
        } catch (IOException e) {
            log.severe("Transcription failed: " + e.getMessage());
            return "";
        }
    }

    private String callGroqWhisper(File audioFile) throws IOException {
        String boundary = "----HiresenseBoundary" + System.currentTimeMillis();
        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + groqApiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (OutputStream out = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out))) {
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            writer.append(model).append("\r\n");
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                  .append(audioFile.getName()).append("\"\r\n");
            writer.append("Content-Type: audio/mpeg\r\n\r\n");
            writer.flush();
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buf = new byte[4096]; int n;
                while ((n = fis.read(buf)) != -1) out.write(buf, 0, n);
            }
            writer.append("\r\n--").append(boundary).append("--\r\n");
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            return new String(conn.getInputStream().readAllBytes());
        }
        log.warning("Groq API returned " + code);
        return "";
    }

    public String getModel() { return model; }
}
