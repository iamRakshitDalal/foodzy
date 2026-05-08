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
public class AIServiceClaud implements AIService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String CLAUDE_MODEL = "claude-sonnet-4-20250514";

    private final WebClient webClient;

    public AIServiceClaud(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com/v1")
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

            log.info("Generating AI description (Claude) - size: {} bytes, MIME: {}",
                    imageBytes.length, mimeType);

            Map<String, Object> requestBody = Map.of(
                    "model", CLAUDE_MODEL,
                    "max_tokens", 150,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "image",
                                                    "source", Map.of(
                                                            "type", "base64",
                                                            "media_type", mimeType,
                                                            "data", base64Image)),
                                            Map.of(
                                                    "type", "text",
                                                    "text",
                                                    "You are a professional food copywriter. Write a mouthwatering, detailed description "
                                                            + "for the food in this image in 1-2 sentences (20-35 words). Mention ingredients, cooking style, texture, and flavor. "
                                                            + "Do not start with 'This is' or 'The image shows'.")))));

            return callClaude(requestBody);

        } catch (Exception e) {
            log.error("Error building Claude request: {}", e.getMessage(), e);
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
    private String callClaude(Map<String, Object> requestBody) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Claude API error [{}]: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("Claude API error: " + errorBody));
                                    }))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("Claude returned null response");
                return "Description not available 3";
            }

            // Claude response structure: { "content": [ { "type": "text", "text": "..." } ]
            // }
            List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
            if (contentBlocks == null || contentBlocks.isEmpty()) {
                log.warn("Claude returned no content blocks");
                return "Description not available 4";
            }

            String result = contentBlocks.stream()
                    .filter(block -> "text".equals(block.get("type")))
                    .map(block -> (String) block.get("text"))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                log.warn("Claude returned no text block");
                return "Description not available 5";
            }

            log.info("Claude generated: {}", result);
            return result.trim();

        } catch (Exception e) {
            log.error("Failed to call Claude API - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return "Description not available 6";
        }
    }
}