package tn.esprit.tic.civiAgora.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.tools.ToolExecutor;
import tn.esprit.tic.civiAgora.dto.chatDto.OllamaChatResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolExecutor toolExecutor;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:qwen2.5:7b}")
    private String model;

    public OllamaClient(ToolExecutor toolExecutor) {
        this.toolExecutor = toolExecutor;
    }

    /**
     * Chat with Ollama. If the model calls tools, execute them and loop back.
     *
     * @param systemPrompt   system prompt (tenant + auth aware)
     * @param messages        conversation [{role, content}]
     * @param includeTools    true for authenticated users
     */
    public OllamaChatResult chat(String systemPrompt, List<Map<String, Object>> messages,
                                 boolean includeTools, String currentUserMessage) {
        List<OllamaChatResult.ExecutedToolResult> executedToolResults = new ArrayList<>();
        try {
            List<Map<String, Object>> ollamaMessages = new ArrayList<>();
            ollamaMessages.add(Map.of("role", "system", "content", systemPrompt));
            ollamaMessages.addAll(messages);

            int maxIterations = 5;
            for (int i = 0; i < maxIterations; i++) {
                ObjectNode body = buildRequestBody(ollamaMessages, includeTools);
                JsonNode response = callApi(body);

                JsonNode message = response.path("message");
                String content = message.path("content").asText("");
                JsonNode toolCalls = message.path("tool_calls");

                if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                    // Add assistant message with tool_calls
                    ollamaMessages.add(mapper.convertValue(message, Map.class));

                    // Execute each tool
                    for (JsonNode toolCall : toolCalls) {
                        String toolName = toolCall.path("function").path("name").asText();
                        JsonNode argsNode = toolCall.path("function").path("arguments");

                        @SuppressWarnings("unchecked")
                        Map<String, Object> args = mapper.convertValue(argsNode, Map.class);

                        log.info("Executing chatbot tool: {}", toolName);
                        String result = toolExecutor.execute(toolName, args, currentUserMessage);
                        executedToolResults.add(new OllamaChatResult.ExecutedToolResult(toolName,
                                Collections.unmodifiableMap(new LinkedHashMap<>(args)), result));

                        ollamaMessages.add(Map.of("role", "tool", "content", result));
                    }
                } else {
                    return new OllamaChatResult(content, List.copyOf(executedToolResults));
                }
            }

            return new OllamaChatResult("I encountered an issue processing your request. Please try again.",
                    List.copyOf(executedToolResults));

        } catch (Exception e) {
            log.error("Ollama API error", e);
            return new OllamaChatResult("Sorry, I'm having trouble connecting to the AI service. "
                    + "Make sure Ollama is running (ollama serve).", List.copyOf(executedToolResults));
        }
    }

    private ObjectNode buildRequestBody(List<Map<String, Object>> messages, boolean includeTools) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.set("messages", mapper.valueToTree(messages));

        ObjectNode options = mapper.createObjectNode();
        options.put("temperature", 0.2);
        body.set("options", options);

        if (includeTools) {
            body.set("tools", mapper.valueToTree(buildOllamaTools()));
        }

        return body;
    }

    private List<Map<String, Object>> buildOllamaTools() {
        return toolExecutor.getToolsForApi().stream()
                .map(tool -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.get("name"),
                                "description", tool.get("description"),
                                "parameters", tool.get("input_schema")
                        )
                ))
                .toList();
    }

    private JsonNode callApi(ObjectNode body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Ollama error {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Ollama returned " + response.statusCode());
        }

        return mapper.readTree(response.body());
    }
}
