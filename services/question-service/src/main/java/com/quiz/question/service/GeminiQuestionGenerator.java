package com.quiz.question.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiz.question.entity.Question;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiQuestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuestionGenerator.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String provider;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public GeminiQuestionGenerator(
            ObjectMapper objectMapper,
            @Value("${app.ai.provider:basic}") String provider,
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.1-flash-lite-preview}") String model,
            @Value("${app.ai.gemini.timeout-seconds:20}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().build();
        this.provider = provider;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<Question> generate(String topic, int count) {
        if (!"gemini".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        try {
            String prompt = buildPrompt(topic, count);
            String requestBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            { "text": %s }
                          ]
                        }
                      ]
                    }
                    """.formatted(objectMapper.writeValueAsString(prompt));

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model
                    + ":generateContent?key="
                    + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[EVENT] Gemini API call failed status={} body={}", response.statusCode(), response.body());
                return List.of();
            }

            String contentText = extractContentText(response.body());
            if (contentText == null || contentText.isBlank()) {
                return List.of();
            }

            String jsonPayload = extractJsonPayload(contentText);
            JsonNode root = objectMapper.readTree(jsonPayload);
            if (!root.isArray()) {
                return List.of();
            }

            List<Question> questions = new ArrayList<>();
            for (JsonNode node : root) {
                String correctAnswer = normalizeCorrectAnswer(node.path("correctAnswer").asText());
                if (correctAnswer == null) {
                    continue;
                }
                Question question = Question.builder()
                        .content(node.path("content").asText())
                        .optionA(node.path("optionA").asText())
                        .optionB(node.path("optionB").asText())
                        .optionC(node.path("optionC").asText())
                        .optionD(node.path("optionD").asText())
                        .correctAnswer(correctAnswer)
                        .build();
                if (isValid(question)) {
                    questions.add(question);
                }
            }

            log.info("[EVENT] Gemini generated {} questions for topic={}", questions.size(), topic);
            return questions;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("[EVENT] Gemini generation failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private String buildPrompt(String topic, int count) {
        return "Generate " + count + " multiple-choice quiz questions about '" + topic + "'. "
                + "Return only JSON array with exactly this schema for each item: "
                + "{\"content\":\"...\",\"optionA\":\"...\",\"optionB\":\"...\",\"optionC\":\"...\",\"optionD\":\"...\",\"correctAnswer\":\"A|B|C|D\"}. "
                + "No markdown, no explanations, no extra keys.";
    }

    private String extractContentText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).path("text").asText(null);
    }

    private String extractJsonPayload(String rawText) {
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine > 0) {
                trimmed = trimmed.substring(firstNewLine + 1, trimmed.length() - 3).trim();
            }
        }
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }
        return trimmed;
    }

    private String normalizeCorrectAnswer(String answer) {
        String normalized = answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "A", "B", "C", "D" -> normalized;
            default -> null;
        };
    }

    private boolean isValid(Question question) {
        return question.getContent() != null && !question.getContent().isBlank()
                && question.getOptionA() != null && !question.getOptionA().isBlank()
                && question.getOptionB() != null && !question.getOptionB().isBlank()
                && question.getOptionC() != null && !question.getOptionC().isBlank()
                && question.getOptionD() != null && !question.getOptionD().isBlank();
    }
}
