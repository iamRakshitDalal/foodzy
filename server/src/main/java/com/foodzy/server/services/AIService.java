package com.foodzy.server.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public AIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String generateDescription(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("generateDescription called with null or empty bytes");
            return "Description not available";
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String detectedMime = detectMimeType(imageBytes);
            log.info("Generating AI description - size: {} bytes, MIME: {}", imageBytes.length, detectedMime);

            // Build parts list
            List<Map<String, Object>> parts = List.of(
                    Map.of("text",
                            "You are a professional food copywriter. Write a mouthwatering, detailed description"
                            + " for the food in this image in 1-2 sentences (about 20-35 words). Mention the main"
                            + " ingredients, cooking style, texture, and flavor profile. Make it sound appetizing"
                            + " and restaurant-quality. Do not start with 'This is' or 'The image shows'."),
                    Map.of("inline_data", Map.of(
                            "mime_type", detectedMime,
                            "data", base64Image))
            );

            // Use HashMap to avoid Map.of() type inference issues
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 150);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(Map.of("parts", parts)));
            requestBody.put("generationConfig", generationConfig);

            return callGemini(requestBody);

        } catch (Exception e) {
            log.error("Error building Gemini request: {}", e.getMessage(), e);
            return "Description not available";
        }
    }

    /**
     * Detects image MIME type from magic bytes.
     */
    private String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return "image/jpeg";
        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47)
            return "image/png";
        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)
            return "image/jpeg";
        // WebP: RIFF....WEBP
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46)
            return "image/webp";
        // GIF: GIF8
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46)
            return "image/gif";
        return "image/jpeg";
    }

    @SuppressWarnings("unchecked")
    private String callGemini(Map<String, Object> requestBody) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/gemini-1.5-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("Gemini API error [{}]: {}", clientResponse.statusCode(), errorBody);
                                return Mono.error(new RuntimeException("Gemini API error: " + errorBody));
                            })
                    )
                    .bodyToMono(Map.class)
                    .map(m -> (Map<String, Object>) m)
                    .block();

            if (response == null) {
                log.warn("Gemini returned null response");
                return "Description not available";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return "Description not available";
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.warn("Gemini returned no parts");
                return "Description not available";
            }

            String result = (String) parts.get(0).get("text");
            log.info("Gemini generated: {}", result);
            return result != null ? result.trim() : "Description not available";

        } catch (Exception e) {
            log.error("Failed to call Gemini API - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return "Description not available";
        }
    }
}
