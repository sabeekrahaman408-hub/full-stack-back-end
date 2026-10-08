package com.apollocare.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads an uploaded medical file (prescription, lab report, injection / vaccination record, scan report ...)
 * with Claude and returns a plain-language summary. Needs an Anthropic API key on the server
 * (environment variable ANTHROPIC_API_KEY). Without a key the upload still works; only the reading is skipped.
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PDF_BYTES = 25L * 1024 * 1024;

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are a careful medical-document reading assistant inside a patient's health-records app.",
            "You read the uploaded file and explain it in simple, plain language for the patient.",
            "Rules:",
            "- Only report what is actually visible in the document. If something is unreadable or missing, say so. Never invent values, names or doses.",
            "- You are NOT diagnosing. Do not change or replace the doctor's treatment.",
            "- Keep it concise and well organised. Use these exact headings, each on its own line, and skip a heading if it does not apply:",
            "Document type:",
            "Summary:",
            "Medicines / injections / vaccines:   (name, dose, how often, how long - exactly as written)",
            "Test results:   (test, value, unit, reference range, and mark High/Low only if the document itself shows it)",
            "Points to discuss with your doctor:",
            "- If the file is not a medical document, say that in one sentence and stop.",
            "- Finish with one line: 'This is an automatic reading to help you understand your file - please confirm everything with your doctor.'");

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final RestClient client;

    public AiAnalysisService(@Value("${app.ai.enabled:true}") boolean enabled,
                             @Value("${app.ai.api-key:}") String apiKey,
                             @Value("${app.ai.model:claude-sonnet-5-5}") String model,
                             @Value("${app.ai.base-url:https://api.anthropic.com}") String baseUrl,
                             @Value("${app.ai.timeout-seconds:90}") int timeoutSeconds) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return enabled && !apiKey.isEmpty();
    }

    public record Result(String status, String text) {
    }

    /** Never throws - returns status done / failed / skipped with a message the user can read. */
    public Result analyze(Path file, String mimeType, String displayName) {
        if (!enabled) {
            return new Result("skipped", "AI reading is turned off on this server.");
        }
        if (apiKey.isEmpty()) {
            return new Result("skipped", "AI reading is not set up yet. Ask the administrator to add the ANTHROPIC_API_KEY setting on the server.");
        }
        try {
            long size = Files.size(file);
            boolean pdf = "application/pdf".equals(mimeType);
            if (!pdf && size > MAX_IMAGE_BYTES) {
                return new Result("skipped", "This image is larger than 5 MB, so it was saved but not read. Upload a smaller photo to get an automatic reading.");
            }
            if (pdf && size > MAX_PDF_BYTES) {
                return new Result("skipped", "This PDF is too large to read automatically (limit 25 MB).");
            }
            String data = Base64.getEncoder().encodeToString(Files.readAllBytes(file));

            Map<String, Object> block = Map.of(
                    "type", pdf ? "document" : "image",
                    "source", Map.of("type", "base64", "media_type", mimeType, "data", data));
            Map<String, Object> text = Map.of("type", "text",
                    "text", "Please read this file (" + displayName + ") and explain it for the patient.");

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 1500,
                    "system", SYSTEM_PROMPT,
                    "messages", List.of(Map.of("role", "user", "content", List.of(block, text))));

            JsonNode response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            StringBuilder out = new StringBuilder();
            if (response != null && response.has("content")) {
                for (JsonNode part : response.get("content")) {
                    if ("text".equals(part.path("type").asText())) {
                        out.append(part.path("text").asText());
                    }
                }
            }
            String summary = out.toString().trim();
            if (summary.isEmpty()) {
                return new Result("failed", "The AI returned an empty answer. Please try again.");
            }
            return new Result("done", summary);

        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            log.warn("AI analysis HTTP {}: {}", code, e.getResponseBodyAsString());
            String msg = switch (code) {
                case 401, 403 -> "The AI key on the server is not valid. Ask the administrator to check ANTHROPIC_API_KEY.";
                case 413 -> "The file is too large for automatic reading.";
                case 429 -> "The AI service is busy right now. Please press 'Analyze again' in a minute.";
                default -> "The AI service could not read this file (error " + code + "). Please try again later.";
            };
            return new Result("failed", msg);
        } catch (IOException e) {
            log.warn("AI analysis could not read file: {}", e.getMessage());
            return new Result("failed", "The stored file could not be read.");
        } catch (Exception e) {
            log.warn("AI analysis failed: {}", e.toString());
            return new Result("failed", "Automatic reading failed (" + e.getClass().getSimpleName() + "). The file itself is saved.");
        }
    }
}
