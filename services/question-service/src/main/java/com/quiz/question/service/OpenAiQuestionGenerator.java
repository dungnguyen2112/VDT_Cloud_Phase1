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
public class OpenAiQuestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenAiQuestionGenerator.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public OpenAiQuestionGenerator(
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4.1-mini}") String model,
            @Value("${app.ai.openai.timeout-seconds:20}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<Question> generate(String topic, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        try {
            String prompt = buildPrompt(topic, count);
            String requestBody = """
                    {
                      "model": %s,
                      "temperature": 0.2,
                      "messages": [
                        {
                          "role": "user",
                          "content": %s
                        }
                      ]
                    }
                    """.formatted(objectMapper.writeValueAsString(model), objectMapper.writeValueAsString(prompt));

            String endpoint = "https://api.openai.com/v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[EVENT] OpenAI API call failed status={} body={}", response.statusCode(), response.body());
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
                String correctAnswer = normalizeCorrectAnswer(node.path("correctAnswer").asText(null));
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

            log.info("[EVENT] OpenAI generated {} questions for topic={}", questions.size(), topic);
            return questions;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("[EVENT] OpenAI generation failed: {}", ex.getMessage());
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
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).path("message").path("content").asText(null);
    }

    private String extractJsonPayload(String rawText) {
        if (rawText == null) return null;
        String trimmed = rawText.trim();

        // If model wrapped output in markdown code block, extract inner.
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

