package controller;

import dto.MatchRequestDTO;
import dto.MentorMatchDTO;
import dto.RecommendationDTO;
import dto.UserPreferencesDTO;
import enums.Role;
import model.User;
import model.UserSkill;
import repository.ReviewRepository;
import repository.SessionRepository;
import repository.UserRepository;
import repository.UserSkillRepository;
import service.BadgeService;
import service.GroqMatchingService;
import service.RecommendationService;
import service.UserPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserPreferencesService preferencesService;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final BadgeService badgeService;
    private final GroqMatchingService geminiMatchingService;

    public RecommendationController(RecommendationService recommendationService,
                                    UserPreferencesService preferencesService,
                                    UserRepository userRepository,
                                    UserSkillRepository userSkillRepository,
                                    ReviewRepository reviewRepository,
                                    SessionRepository sessionRepository,
                                    BadgeService badgeService,
                                    GroqMatchingService geminiMatchingService) {
        this.recommendationService = recommendationService;
        this.preferencesService = preferencesService;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.badgeService = badgeService;
        this.geminiMatchingService = geminiMatchingService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(Authentication auth) {
        User user = getUser(auth);
        UserPreferencesDTO prefs = preferencesService.getPreferences(user);
        return ResponseEntity.ok(recommendationService.getRecommendations(user, prefs.getLearningGoals()));
    }

    @PostMapping("/match")
    public ResponseEntity<List<MentorMatchDTO>> matchMentors(
            @RequestBody MatchRequestDTO request,
            Authentication auth) {

        // Load all enabled mentors from the database
        List<User> mentors = userRepository.findByRoleAndEnabled(Role.MENTOR, true);

        List<GroqMatchingService.MentorSummary> summaries = new ArrayList<>();
        for (User mentor : mentors) {
            List<UserSkill> skills = userSkillRepository
                    .findByUserIdAndType(mentor.getId(), UserSkill.SkillType.MENTOR);
            if (skills.isEmpty()) continue;

            String skillNames = skills.stream()
                    .map(us -> us.getSkill().getName())
                    .collect(Collectors.joining(", "));

            Double avgRating = reviewRepository.findAverageRatingByReviewee(mentor);
            if (avgRating == null) avgRating = 0.0;
            double rating = Math.round(avgRating * 10.0) / 10.0;

            long totalSessions = sessionRepository.countCompletedMentorSessions(mentor);

            List<String> badgeList = badgeService.getUserBadgeNames(
                    mentor,
                    mentor.getReputation() != null ? mentor.getReputation() : 0,
                    totalSessions,
                    avgRating);
            String badges = String.join(", ", badgeList);
            String location = mentor.getLocation() != null ? mentor.getLocation() : "";

            summaries.add(new GroqMatchingService.MentorSummary(
                    mentor.getId(), mentor.getName(), location, skillNames,
                    rating, (int) totalSessions, badges));
        }

        if (summaries.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        String desiredSkillsStr = (request.getDesiredSkills() != null && !request.getDesiredSkills().isEmpty())
                ? String.join(", ", request.getDesiredSkills()) : "";

        List<Long> rankedIds = geminiMatchingService.rankMentors(
                nvl(request.getLearningGoals()),
                nvl(request.getCurrentLevel()),
                nvl(request.getPreferredSchedule()),
                desiredSkillsStr,
                summaries);

        // Fallback: if Gemini returns no IDs, sort by rating descending
        if (rankedIds.isEmpty()) {
            rankedIds = summaries.stream()
                    .sorted((a, b) -> Double.compare(b.averageRating, a.averageRating))
                    .map(s -> s.userId)
                    .collect(Collectors.toList());
        }

        Map<Long, GroqMatchingService.MentorSummary> byId = summaries.stream()
                .collect(Collectors.toMap(s -> s.userId, s -> s));

        List<MentorMatchDTO> result = new ArrayList<>();
        int rank = 0;
        for (Long uid : rankedIds) {
            GroqMatchingService.MentorSummary s = byId.get(uid);
            if (s == null) continue;

            MentorMatchDTO dto = new MentorMatchDTO();
            dto.setUserId(s.userId);
            dto.setName(s.name);
            dto.setLocation(s.location);
            dto.setSkills(skillList(s.skills));
            dto.setAverageRating(s.averageRating);
            dto.setTotalSessions(s.totalSessions);
            dto.setBadges(badgeList(s.badges));
            dto.setMatchPercentage(matchPct(rank));
            result.add(dto);
            rank++;
        }

        return ResponseEntity.ok(result);
    }

    private static int matchPct(int rank) {
        if (rank == 0) return 95;
        if (rank == 1) return 88;
        if (rank == 2) return 82;
        if (rank == 3) return 76;
        return Math.max(40, 70 - (rank - 4) * 3);
    }

    private static List<String> skillList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",\\s*"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static List<String> badgeList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",\\s*"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
