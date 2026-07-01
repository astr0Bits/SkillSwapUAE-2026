package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import enums.BadgeCriteriaType;
import model.*;
import model.UserBadge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import repository.*;
import repository.UserBadgeRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SkillRepository skillRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentAttemptRepository attemptRepository,
                             SkillRepository skillRepository,
                             BadgeRepository badgeRepository,
                             UserBadgeRepository userBadgeRepository) {
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.skillRepository = skillRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    @Transactional
    public Map<String, Object> generateAssessment(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        String questionsJson = fetchQuestionsFromGemini(skill.getName());

        Assessment assessment = new Assessment();
        assessment.setSkill(skill);
        assessment.setQuestions(questionsJson);
        assessment.setPassingScore(70);
        assessmentRepository.save(assessment);

        return buildClientResponse(assessment);
    }

    @Transactional
    public Map<String, Object> submitAttempt(Long assessmentId, List<Integer> answers, User user) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        List<Map<String, Object>> questions;
        try {
            questions = objectMapper.readValue(assessment.getQuestions(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse assessment questions");
        }

        int correct = 0;
        for (int i = 0; i < Math.min(answers.size(), questions.size()); i++) {
            Object correctIdx = questions.get(i).get("correctAnswerIndex");
            if (correctIdx != null && answers.get(i).equals(((Number) correctIdx).intValue())) {
                correct++;
            }
        }

        int total = questions.size();
        int score = total > 0 ? (int) Math.round(correct * 100.0 / total) : 0;
        boolean passed = score >= assessment.getPassingScore();

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(user);
        attempt.setAssessment(assessment);
        attempt.setScore(score);
        attempt.setPassed(passed);
        attemptRepository.save(attempt);

        String badgeEarned = null;
        if (passed) {
            long passCount = attemptRepository.countByUserAndAssessmentAndPassed(user, assessment, true);
            if (passCount <= 1) {
                String badgeName = "Skill Validated: " + assessment.getSkill().getName();
                Badge awardedBadge = badgeRepository.findByName(badgeName).orElseGet(() -> {
                    Badge b = new Badge(badgeName,
                            "Passed the " + assessment.getSkill().getName() + " assessment",
                            "🏅", BadgeCriteriaType.CUSTOM, 0);
                    b.setPublished(false);
                    return badgeRepository.save(b);
                });
                if (!userBadgeRepository.existsByUserAndBadge(user, awardedBadge)) {
                    userBadgeRepository.save(new UserBadge(user, awardedBadge));
                }
                badgeEarned = badgeName;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("passed", passed);
        result.put("correct", correct);
        result.put("total", total);
        result.put("passingScore", assessment.getPassingScore());
        result.put("badgeEarned", badgeEarned);
        result.put("skillName", assessment.getSkill().getName());
        return result;
    }

    private String fetchQuestionsFromGemini(String skillName) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY not set — using placeholder questions for {}", skillName);
            return buildPlaceholderQuestions(skillName);
        }

        String prompt = "Generate exactly 5 multiple-choice quiz questions to test a learner's knowledge of: " + skillName + ".\n\n" +
                "Rules:\n" +
                "- Return ONLY a valid JSON array. No markdown, no code fences, no explanation outside the JSON.\n" +
                "- Each element must have exactly three keys:\n" +
                "    \"question\"         : the full question text (string)\n" +
                "    \"options\"          : array of exactly 4 distinct answer strings (write out the full answer text, NOT single letters)\n" +
                "    \"correctAnswerIndex\": integer 0-3 indicating which option is correct\n\n" +
                "Example of one valid element:\n" +
                "{\"question\":\"What does the 'S' in the SOLID principles stand for?\"," +
                "\"options\":[\"Single Responsibility\",\"Simple Design\",\"Static Binding\",\"Structured Programming\"]," +
                "\"correctAnswerIndex\":0}\n\n" +
                "Generate 5 such questions about: " + skillName + ". Vary difficulty from basic to advanced.";

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
                    log.error("Groq returned no choices for skill '{}'. Full response: {}", skillName, response.getBody());
                    return buildPlaceholderQuestions(skillName);
                }
                String text = choices.get(0).path("message").path("content").asText("");

                // Strip possible markdown code fences
                String cleaned = text.trim();
                Pattern fence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
                Matcher m = fence.matcher(cleaned);
                if (m.find()) cleaned = m.group(1).trim();

                // Validate it parses as a JSON array
                objectMapper.readValue(cleaned, new TypeReference<List<Map<String, Object>>>() {});
                return cleaned;
            } else {
                log.error("Groq non-2xx for skill '{}': status={} body={}", skillName, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Groq question generation failed for '{}': {} — {}", skillName, e.getClass().getSimpleName(), e.getMessage(), e);
        }

        return buildPlaceholderQuestions(skillName);
    }

    private String buildPlaceholderQuestions(String skillName) {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("question", "Sample question " + i + " about " + skillName + "?");
            q.put("options", List.of("Option A", "Option B", "Option C", "Option D"));
            q.put("correctAnswerIndex", 0);
            questions.add(q);
        }
        try {
            return objectMapper.writeValueAsString(questions);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Object> buildClientResponse(Assessment assessment) {
        List<Map<String, Object>> rawQuestions;
        try {
            rawQuestions = objectMapper.readValue(assessment.getQuestions(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            rawQuestions = List.of();
        }

        // Strip correctAnswerIndex before sending to client
        List<Map<String, Object>> clientQuestions = rawQuestions.stream().map(q -> {
            Map<String, Object> safe = new LinkedHashMap<>(q);
            safe.remove("correctAnswerIndex");
            return safe;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("assessmentId", assessment.getId());
        resp.put("skillId", assessment.getSkill().getId());
        resp.put("skillName", assessment.getSkill().getName());
        resp.put("passingScore", assessment.getPassingScore());
        resp.put("questions", clientQuestions);
        return resp;
    }
}
