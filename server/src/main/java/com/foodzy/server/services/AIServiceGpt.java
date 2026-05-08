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
public class AIServiceGpt implements AIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public AIServiceGpt(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openai.com/v1")
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

            String imageDataUrl = "data:" + mimeType + ";base64," + base64Image;

            log.info("Generating AI description (OpenAI) - size: {} bytes, MIME: {}",
                    imageBytes.length, mimeType);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini", // Fixed: Correct model name
                    "messages", List.of( // Fixed: Correct field name
                            Map.of(
                                    "role", "user",
                                    "content", List.of( // Fixed: Correct content structure
                                            Map.of(
                                                    "type", "text", // Fixed: Correct type
                                                    "text",
                                                    "You are a professional food copywriter. Write a mouthwatering, detailed description "
                                                            +
                                                            "for the food in this image in 1-2 sentences (20-35 words). Mention ingredients, cooking style, texture, and flavor. "
                                                            +
                                                            "Do not start with 'This is' or 'The image shows'."),
                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of("url", imageDataUrl))))), // Fixed: Correct
                                                                                                  // image structure
                    "max_tokens", 150, // Fixed: Correct parameter name
                    "temperature", 0.7);

            return callOpenAI(requestBody);

        } catch (Exception e) {
            log.error("Error building OpenAI request: {}", e.getMessage(), e);
            return "Description not available 2";
        }
    }

    @Override
    public String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4)
            return "image/jpeg";

        // Fixed: Complete WebP magic number check
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
    private String callOpenAI(Map<String, Object> requestBody) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions") // Fixed: Correct endpoint
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("OpenAI API error [{}]: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("OpenAI API error: " + errorBody));
                                    }))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("OpenAI returned null response");
                return "Description not available 3";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("OpenAI returned no choices");
                return "Description not available 4";
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

            if (message == null) {
                log.warn("OpenAI returned no message");
                return "Description not available 5";
            }

            String result = (String) message.get("content");

            log.info("OpenAI generated: {}", result);
            return result != null ? result.trim() : "Description not available 6";

        } catch (Exception e) {
            log.error("Failed to call OpenAI API - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return "Description not available 7";
        }
    }
}