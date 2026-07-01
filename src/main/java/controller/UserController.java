package controller;

import dto.UserUpdateDTO;
import dto.BadgeDTO;
import dto.UserStatsDTO;
import model.User;
import repository.AuditLogRepository;
import repository.CreditTransactionRepository;
import repository.MentorAvailabilityRepository;
import repository.NotificationRepository;
import repository.ReviewRepository;
import repository.SessionRepository;
import repository.UserPreferencesRepository;
import repository.UserRepository;
import repository.UserSkillRepository;
import service.BadgeService;
import service.UserService;
import service.UserStatsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import model.Review;
import model.UserSkill;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserStatsService userStatsService;
    private final BadgeService badgeService;
    private final SessionRepository sessionRepository;
    private final NotificationRepository notificationRepository;
    private final UserSkillRepository userSkillRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogRepository auditLogRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public UserController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          UserStatsService userStatsService,
                          BadgeService badgeService,
                          SessionRepository sessionRepository,
                          NotificationRepository notificationRepository,
                          UserSkillRepository userSkillRepository,
                          MentorAvailabilityRepository mentorAvailabilityRepository,
                          UserPreferencesRepository userPreferencesRepository,
                          ReviewRepository reviewRepository,
                          AuditLogRepository auditLogRepository,
                          CreditTransactionRepository creditTransactionRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userStatsService = userStatsService;
        this.badgeService = badgeService;
        this.sessionRepository = sessionRepository;
        this.notificationRepository = notificationRepository;
        this.userSkillRepository = userSkillRepository;
        this.mentorAvailabilityRepository = mentorAvailabilityRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogRepository = auditLogRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    @PutMapping("/update/{email}")
    public ResponseEntity<?> updateUser(
            @PathVariable String email,
            @Valid @RequestBody UserUpdateDTO updateDTO,
            Authentication auth) {

        if (!auth.getName().equals(email)) {
            return ResponseEntity.status(403).body("You can only update your own profile.");
        }

        try {
            User updatedUser = userService.updateUser(email, updateDTO, passwordEncoder);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @DeleteMapping("/delete/{email}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable String email, Authentication auth) {

        if (!auth.getName().equals(email)) {
            return ResponseEntity.status(403).body("You are not authorized to delete this account.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found.");
        }

        Long userId = user.getId();

        // 1. password history — native query since no repo
        entityManager.createNativeQuery(
            "DELETE FROM password_history WHERE user_id = :uid")
            .setParameter("uid", userId)
            .executeUpdate();

        // 2. Child tables via repositories
       // auditLogRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        userSkillRepository.deleteByUserId(userId);
        mentorAvailabilityRepository.deleteByUserId(userId);
        userPreferencesRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        sessionRepository.deleteByUserId(userId);

        // 3. Finally delete the user
        userRepository.delete(user);

        return ResponseEntity.ok("Account deleted successfully.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserStatsDTO stats = userStatsService.getUserStats(user);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",            user.getId());
        resp.put("email",         user.getEmail());
        resp.put("name",          user.getName());
        resp.put("role",          user.getRole().name());
        resp.put("location",      user.getLocation());
        resp.put("credits",       user.getCredits() != null ? user.getCredits() : 0);
        resp.put("totalSessions", stats.getTotalSessions());
        resp.put("avgRating",     stats.getReputation() > 0
                ? String.format("%.1f", stats.getReputation() / 20.0) : null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me/badges")
    public ResponseEntity<List<BadgeDTO>> getMyBadges(Authentication auth) {
        User user = getUserFromAuth(auth);
        UserStatsDTO stats = userStatsService.getUserStats(user);
        List<BadgeDTO> badges = badgeService.getUserBadges(user, stats.getPoints(), stats.getTotalSessions(), stats.getReputation());
        return ResponseEntity.ok(badges);
    }

    @GetMapping("/me/earnings")
    public ResponseEntity<Map<String, Object>> getMentorEarnings(Authentication auth) {
        User user = getUserFromAuth(auth);

        long totalCreditsEarned = creditTransactionRepository.sumEarnedByUser(user);
        long completedMentorSessions = sessionRepository.countCompletedMentorSessions(user);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long thisMonthCredits = creditTransactionRepository.sumEarnedByUserAndPeriod(user, monthStart, now.plusSeconds(1));

        List<Map<String, Object>> monthlyBreakdown = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            long credits = creditTransactionRepository.sumEarnedByUserAndPeriod(user, start, end);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", start.format(fmt));
            entry.put("creditsEarned", credits);
            monthlyBreakdown.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCreditsEarned", totalCreditsEarned);
        response.put("completedSessionsAsMentor", completedMentorSessions);
        response.put("thisMonthCredits", thisMonthCredits);
        response.put("monthlyBreakdown", monthlyBreakdown);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<?> getPublicProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        List<UserSkill> allSkills = userSkillRepository.findByUserId(userId);
        List<Review> reviews = reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
        Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
        long completedSessions = sessionRepository.countCompletedSessionsForUser(user);
        UserStatsDTO stats = userStatsService.getUserStats(user);
        List<BadgeDTO> earnedBadges = badgeService.getUserBadges(user, stats.getPoints(), stats.getTotalSessions(), stats.getReputation())
                .stream().filter(BadgeDTO::isEarned).collect(Collectors.toList());

        List<Map<String, Object>> mentorSkills = allSkills.stream()
                .filter(us -> us.getType() == UserSkill.SkillType.MENTOR)
                .map(us -> Map.of("skillName", (Object) us.getSkill().getName(),
                        "level", us.getLevel() != null ? us.getLevel().name() : "BEGINNER"))
                .collect(Collectors.toList());

        List<Map<String, Object>> learnSkills = allSkills.stream()
                .filter(us -> us.getType() == UserSkill.SkillType.LEARN)
                .map(us -> Map.of("skillName", (Object) us.getSkill().getName(),
                        "level", us.getLevel() != null ? us.getLevel().name() : "BEGINNER"))
                .collect(Collectors.toList());

        List<Map<String, Object>> reviewList = reviews.stream().limit(10).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reviewerName", r.getReviewer() != null ? r.getReviewer().getName() : "Anonymous");
            m.put("rating", r.getRating());
            m.put("comment", r.getComment() != null ? r.getComment() : "");
            m.put("date", r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "");
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("location", user.getLocation() != null ? user.getLocation() : "");
        profile.put("role", user.getRole().name());
        profile.put("mentorSkills", mentorSkills);
        profile.put("learnSkills", learnSkills);
        profile.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        profile.put("totalReviews", reviews.size());
        profile.put("completedSessions", completedSessions);
        profile.put("badges", earnedBadges.stream()
                .map(b -> Map.of("name", (Object) b.getName(), "icon", b.getIcon() != null ? b.getIcon() : ""))
                .collect(Collectors.toList()));
        profile.put("reviews", reviewList);

        return ResponseEntity.ok(profile);
    }

    // Helper method to extract User from Authentication
    private User getUserFromAuth(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}