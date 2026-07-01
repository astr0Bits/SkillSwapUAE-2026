package controller;

import model.User;
import repository.UserRepository;
import service.AssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final UserRepository userRepository;

    public AssessmentController(AssessmentService assessmentService, UserRepository userRepository) {
        this.assessmentService = assessmentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate/{skillId}")
    public ResponseEntity<?> generate(@PathVariable Long skillId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        try {
            Map<String, Object> assessment = assessmentService.generateAssessment(skillId);
            return ResponseEntity.ok(assessment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{assessmentId}/submit")
    public ResponseEntity<?> submit(@PathVariable Long assessmentId,
                                    @RequestBody Map<String, List<Integer>> body,
                                    Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "User not found"));

        List<Integer> answers = body.get("answers");
        if (answers == null) return ResponseEntity.badRequest().body(Map.of("error", "answers field required"));

        try {
            Map<String, Object> result = assessmentService.submitAttempt(assessmentId, answers, user);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
