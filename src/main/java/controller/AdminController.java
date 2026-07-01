package controller;

import enums.BadgeCriteriaType;
import enums.SessionStatus;
import model.Badge;
import model.Session;
import model.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import repository.*;
import service.BadgeService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogRepository auditLogRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    public AdminController(UserRepository userRepository,
                           SessionRepository sessionRepository,
                           ReviewRepository reviewRepository,
                           AuditLogRepository auditLogRepository,
                           BadgeRepository badgeRepository,
                           BadgeService badgeService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogRepository = auditLogRepository;
        this.badgeRepository = badgeRepository;
        this.badgeService = badgeService;
    }

    // ── Overview ──────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        List<User> users = userRepository.findAll();
        long total = users.size();
        long enabled = users.stream().filter(User::isEnabled).count();
        return ResponseEntity.ok(Map.of(
            "totalUsers", total,
            "totalSessions", sessionRepository.count(),
            "totalReviews", reviewRepository.count(),
            "enabledUsers", enabled,
            "disabledUsers", total - enabled
        ));
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    @GetMapping("/sessions")
    public ResponseEntity<?> getAllSessions() {
        return ResponseEntity.ok(
            sessionRepository.findAll().stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("learnerName", s.getLearner() != null ? s.getLearner().getName() : "");
                m.put("learnerEmail", s.getLearner() != null ? s.getLearner().getEmail() : "");
                m.put("mentorName", s.getMentor() != null ? s.getMentor().getName() : "");
                m.put("mentorEmail", s.getMentor() != null ? s.getMentor().getEmail() : "");
                m.put("skillName", s.getSkill() != null ? s.getSkill().getName() : "");
                m.put("scheduledTime", s.getScheduledTime() != null ? s.getScheduledTime().toString() : "");
                m.put("durationMinutes", s.getDurationMinutes());
                m.put("mode", s.getMode() != null ? s.getMode().name() : "");
                m.put("status", s.getStatus() != null ? s.getStatus().name() : "");
                return m;
            }).collect(Collectors.toList())
        );
    }

    @PutMapping("/sessions/{id}/cancel")
    public ResponseEntity<?> cancelSession(@PathVariable Long id) {
        Session session = sessionRepository.findById(id).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);
        return ResponseEntity.ok(Map.of("message", "Session cancelled", "id", id));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        if (!sessionRepository.existsById(id)) return ResponseEntity.notFound().build();
        sessionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Session deleted"));
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() {
        return ResponseEntity.ok(
            reviewRepository.findAll().stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("reviewerName", r.getReviewer() != null ? r.getReviewer().getName() : "");
                m.put("reviewerEmail", r.getReviewer() != null ? r.getReviewer().getEmail() : "");
                m.put("revieweeName", r.getReviewee() != null ? r.getReviewee().getName() : "");
                m.put("revieweeEmail", r.getReviewee() != null ? r.getReviewee().getEmail() : "");
                m.put("rating", r.getRating());
                m.put("comment", r.getComment() != null ? r.getComment() : "");
                m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                return m;
            }).collect(Collectors.toList())
        );
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) return ResponseEntity.notFound().build();
        reviewRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    @GetMapping("/badges")
    public ResponseEntity<?> getAllBadges() {
        return ResponseEntity.ok(badgeService.getAllBadgesForAdmin());
    }

    @PostMapping("/badges/create")
    public ResponseEntity<?> createBadge(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        String iconName = (String) body.get("iconName");
        String criteriaTypeStr = (String) body.get("criteriaType");
        Object thresholdObj = body.get("thresholdValue");

        if (name == null || name.isBlank() || description == null || description.isBlank()
                || iconName == null || iconName.isBlank()
                || criteriaTypeStr == null || thresholdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        BadgeCriteriaType criteriaType;
        try {
            criteriaType = BadgeCriteriaType.valueOf(criteriaTypeStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid criteriaType"));
        }

        int threshold;
        try {
            threshold = Integer.parseInt(thresholdObj.toString());
            if (threshold < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "thresholdValue must be a positive integer"));
        }

        Badge badge = new Badge(name.trim(), description.trim(), iconName.trim(), criteriaType, threshold);
        badge.setPublished(false);
        Badge saved = badgeRepository.save(badge);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("message", "Badge created as draft");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/badges/{id}/publish")
    public ResponseEntity<?> publishBadge(@PathVariable Long id) {
        Badge badge = badgeRepository.findById(id).orElse(null);
        if (badge == null) return ResponseEntity.notFound().build();
        badge.setPublished(true);
        badgeRepository.save(badge);
        return ResponseEntity.ok(Map.of("message", "Badge published", "id", id));
    }

    @PutMapping("/badges/{id}/unpublish")
    public ResponseEntity<?> unpublishBadge(@PathVariable Long id) {
        Badge badge = badgeRepository.findById(id).orElse(null);
        if (badge == null) return ResponseEntity.notFound().build();
        badge.setPublished(false);
        badgeRepository.save(badge);
        return ResponseEntity.ok(Map.of("message", "Badge unpublished", "id", id));
    }

    @DeleteMapping("/badges/{id}")
    public ResponseEntity<?> deleteBadge(@PathVariable Long id) {
        Badge badge = badgeRepository.findById(id).orElse(null);
        if (badge == null) return ResponseEntity.notFound().build();
        long earned = badgeService.computeEarnedCount(badge);
        if (earned > 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cannot delete: this badge has been earned by " + earned + " user(s)"
            ));
        }
        badgeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Badge deleted"));
    }

    // ── Audit Logs ────────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(
            auditLogRepository.findAll(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "timestamp"))
            ).getContent().stream().map(log -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", log.getId());
                m.put("actorEmail", log.getUserEmail());
                m.put("action", log.getAction());
                m.put("ipAddress", log.getIpAddress());
                m.put("outcome", log.getStatus());
                m.put("timestamp", log.getTimestamp() != null ? log.getTimestamp().toString() : "");
                m.put("message", log.getMessage());
                return m;
            }).collect(Collectors.toList())
        );
    }

}
