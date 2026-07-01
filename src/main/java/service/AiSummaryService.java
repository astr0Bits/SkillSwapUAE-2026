package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateSummary(String skillName, int durationMinutes,
                                  String learnerName, String mentorName) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY is not set — returning fallback summary.");
            return "Summary unavailable — please check back later.";
        }

        String prompt = "You are an educational assistant. Summarize the following mentorship session " +
                "in 3-5 sentences, highlighting the key topics covered, skills practised, and value " +
                "gained by the learner.\n\n" +
                "Session details:\n" +
                "- Skill: " + skillName + "\n" +
                "- Duration: " + durationMinutes + " minutes\n" +
                "- Mentor: " + mentorName + "\n" +
                "- Learner: " + learnerName + "\n\n" +
                "Write in a clear, encouraging tone suitable for a learner to review later as revision notes.";

        try {
            Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    log.error("Groq returned no choices for generateSummary. Full response: {}", response.getBody());
                    return "Summary unavailable — please check back later.";
                }
                String text = choices.get(0).path("message").path("content").asText("");
                if (!text.isBlank()) {
                    return text;
                }
                log.warn("Groq returned blank text for generateSummary. Full response: {}", response.getBody());
            } else {
                log.error("Groq non-2xx for generateSummary: status={} body={}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Groq rate limit hit for generateSummary (429) — returning fallback.");
            } else {
                log.error("Groq API error for generateSummary: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("Groq API call failed for generateSummary: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }

        return "Summary unavailable — please check back later.";
    }
}
