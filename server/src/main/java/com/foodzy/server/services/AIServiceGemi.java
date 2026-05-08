package com.foodzy.server.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIServiceGemi implements AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public AIServiceGemi(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public String generateDescription(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("generateDescription called with null or empty bytes");
            return "Description not available 1";
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = detectMimeType(imageBytes);

            log.info("Generating AI description (Gemini) - size: {} bytes, MIME: {}",
                    imageBytes.length, mimeType);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text",
                                            "You are a professional food copywriter. Write a mouthwatering, detailed description "
                                                    + "for the food in this image in 1-2 sentences (20-35 words). Mention ingredients, cooking style, texture, and flavor. "
                                                    + "Do not start with 'This is' or 'The image shows'."),
                                    Map.of("inlineData", Map.of(
                                            "mimeType", mimeType,
                                            "data", base64Image))))),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 150,
                            "temperature", 0.7));

            return callGemini(requestBody);

        } catch (Exception e) {
            log.error("Error building Gemini request: {}", e.getMessage(), e);
            return "Description not available 2";
        }
    }

    @Override
    public String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4)
            return "image/jpeg";

        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47)
            return "image/png";
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8)
            return "image/jpeg";
        if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46)
            return "image/webp";
        if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x38)
            return "image/gif";

        return "image/jpeg";
    }

    @SuppressWarnings("unchecked")
    private String callGemini(Map<String, Object> requestBody) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/models/gemini-2.0-flash-lite:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Gemini API error [{}]: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("Gemini API error: " + errorBody));
                                    }))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("Gemini returned null response");
                return "Description not available 3";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return "Description not available 4";
            }

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");

            if (content == null) {
                log.warn("Gemini returned no content");
                return "Description not available 5";
            }

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.warn("Gemini returned no parts");
                return "Description not available 6";
            }

            String result = (String) parts.get(0).get("text");

            log.info("Gemini generated: {}", result);
            return result != null ? result.trim() : "Description not available 7";

        } catch (Exception e) {
            log.error("Failed to call Gemini API - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return "Description not available 8";
        }
    }
}