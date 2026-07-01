package controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import service.AuditLogService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final ReviewRepository reviewRepo;
    private final UserSkillRepository userSkillRepo;
    private final UserPreferencesRepository userPrefsRepo;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public UserManagementController(UserRepository userRepo,
                                    SessionRepository sessionRepo,
                                    ReviewRepository reviewRepo,
                                    UserSkillRepository userSkillRepo,
                                    UserPreferencesRepository userPrefsRepo,
                                    AuditLogService auditLogService) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.reviewRepo = reviewRepo;
        this.userSkillRepo = userSkillRepo;
        this.userPrefsRepo = userPrefsRepo;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(
            userRepo.findAll().stream().map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("role", u.getRole() != null ? u.getRole().name() : "");
                m.put("enabled", u.isEnabled());
                m.put("credits", u.getCredits() != null ? u.getCredits() : 0);
                m.put("reputation", u.getReputation() != null ? u.getReputation() : 0);
                m.put("location", u.getLocation() != null ? u.getLocation() : "");
                return m;
            }).collect(Collectors.toList())
        );
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id,
                                          Authentication auth,
                                          HttpServletRequest request) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        user.setEnabled(!user.isEnabled());
        userRepo.save(user);

        String adminEmail = auth != null ? auth.getName() : "admin";
        String action = user.isEnabled() ? "enabled" : "disabled";
        auditLogService.logSuccess(adminEmail, "ADMIN_TOGGLE_USER_STATUS", request,
                "Admin " + action + " user ID: " + id + " (" + user.getEmail() + ")");

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "enabled", user.isEnabled(),
            "message", user.isEnabled() ? "User enabled" : "User disabled"
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        Authentication auth,
                                        HttpServletRequest request) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();

        // Cascade delete in FK dependency order via native SQL for completeness
        entityManager.createNativeQuery("DELETE FROM password_history WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_skills WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM mentor_availability WHERE mentor_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_preferences WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reviews WHERE reviewer_id = :uid OR reviewee_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM assessment_attempts WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_badges WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM credit_transactions WHERE user_id = :uid")
                .setParameter("uid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM sessions WHERE mentor_id = :uid OR learner_id = :uid")
                .setParameter("uid", id).executeUpdate();
        userRepo.deleteById(id);

        String adminEmail = auth != null ? auth.getName() : "admin";
        auditLogService.logSuccess(adminEmail, "ADMIN_DELETE_USER", request,
                "Admin permanently deleted user ID: " + id);

        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }
}
