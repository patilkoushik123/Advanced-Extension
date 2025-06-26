package com.research.assistant;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
        String prompt = buildPrompt(request);

        final String apiKey = (System.getenv("GEMINI_KEY") != null && !System.getenv("GEMINI_KEY").isBlank())
        	    ? System.getenv("GEMINI_KEY")
        	    : this.geminiApiKey;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            )
        );

        try {
            String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("generativelanguage.googleapis.com")
                    .path("/v1beta/models/gemini-2.0-flash:generateContent")
                    .queryParam("key", apiKey)
                    .build()
                )
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<?, ?> map = objectMapper.readValue(response, Map.class);
            List<?> candidates = (List<?>) map.get("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                List<?> parts = (List<?>) content.get("parts");

                if (parts != null && !parts.isEmpty()) {
                    Map<?, ?> part = (Map<?, ?>) parts.get(0);
                    return part.get("text").toString();
                }
            }

            return "No answer found.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during API call: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();

        switch (request.getOperation()) {
            case "summarize":
                prompt.append("Summarize the following content:\n\n");
                break;
            case "suggest":
                prompt.append("Based on the following content, suggest additional points:\n\n");
                break;
            case "mcq":
                prompt.append(
                    "when the selected content is in the form of mutliple choice questions or with question mark solve it and select correct option or answer for that questions and give that option dont summarize just check the question and options and solve it and give answer correct option"
                    
                    
                    
                      );
                break;

            case "generate-mcqs":
                prompt.append(
                    "Based on the following passage, generate exactly 10 multiple choice questions (MCQs). " +
                    "Each question should include 4 options (A, B, C, D), clearly indicate the correct answer, and include a 1-2 line explanation. " +
                    "Format:\n\n" +
                    "Q1. What is ...?\nA) ...\nB) ...\nC) ...\nD) ...\nAnswer: B\nExplanation: ...\n\n" +
                    "Passage:\n"
                );
                break;
            case "custom-question":
            	prompt.append("I will type question please provide correct answer");
              break;  
            default:
                throw new IllegalArgumentException("Unknown operation: " + request.getOperation());
        }

        prompt.append(request.getContent());
        return prompt.toString();
        
    }
    
    


}