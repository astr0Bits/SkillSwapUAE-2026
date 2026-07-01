package controller;

import dto.UserSkillDTO;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.UserRepository;
import service.UserSkillService;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final UserRepository userRepository;

    public UserSkillController(UserSkillService userSkillService,
                               UserRepository userRepository) {
        this.userSkillService = userSkillService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/skills")
    public ResponseEntity<?> getMySkills(Authentication authentication) {
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).body("User not found");
        // Returns { teach: [...], learn: [...] }
        return ResponseEntity.ok(userSkillService.getUserSkills(user.getId()));
    }

    // POST body: { "skillName": "Python", "type": "MENTOR", "level": "INTERMEDIATE" }
    @PostMapping("/skills")
    public ResponseEntity<?> addSkill(Authentication authentication,
                                      @RequestBody UserSkillDTO dto) {
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).body("User not found");
        try {
            return ResponseEntity.ok(userSkillService.addSkillToUser(user.getId(), dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // skillId here is the user_skills.id (not skills.id)
    @DeleteMapping("/skills/{userSkillId}")
    public ResponseEntity<?> removeSkill(Authentication authentication,
                                         @PathVariable Long userSkillId) {
        User user = resolveUser(authentication);
        if (user == null) return ResponseEntity.status(401).body("User not found");
        try {
            userSkillService.removeUserSkill(user.getId(), userSkillId);
            return ResponseEntity.ok(Map.of("message", "Skill removed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}