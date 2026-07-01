package controller;

import dto.MentorBrowseDTO;
import model.Skill;
import model.User;
import model.UserSkill;
import repository.*;
import service.BadgeService;
import service.BrowseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/browse")
@CrossOrigin(origins = "*")
public class BrowseController {

    private final BrowseService browseService;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final MentorAvailabilityRepository availabilityRepository;
    private final BadgeService badgeService;

    public BrowseController(BrowseService browseService,
                             SkillRepository skillRepository,
                             UserSkillRepository userSkillRepository,
                             ReviewRepository reviewRepository,
                             SessionRepository sessionRepository,
                             MentorAvailabilityRepository availabilityRepository,
                             BadgeService badgeService) {
        this.browseService = browseService;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.availabilityRepository = availabilityRepository;
        this.badgeService = badgeService;
    }

    // ── GET /api/browse/mentors (unchanged — used by dashboard mentor dropdown) ──
    @GetMapping("/mentors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MentorBrowseDTO>> browseMentors(
            @RequestParam(required = false) Map<String, String> filters) {
        return ResponseEntity.ok(browseService.findMentors(filters));
    }

    // ── GET /api/browse/skills?sort=mentorCount|name|rating ──────────────────
    @GetMapping("/skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSkills(@RequestParam(defaultValue = "mentorCount") String sort) {
        List<Skill> skills = skillRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        // Batch-fetch mentor counts to avoid N+1
        Map<Long, Long> mentorCountMap = new java.util.HashMap<>();
        for (Object[] row : userSkillRepository.countMentorsPerSkill()) {
            mentorCountMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        for (Skill skill : skills) {
            long mentorCount = mentorCountMap.getOrDefault(skill.getId(), 0L);
            Double avgRating = skillRepository.findAverageRatingBySkillId(skill.getId());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", skill.getId());
            m.put("name", skill.getName());
            m.put("category", skill.getCategory() != null ? skill.getCategory() : "");
            m.put("mentorCount", mentorCount);
            m.put("averageRating", avgRating != null
                    ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
            result.add(m);
        }

        if ("name".equals(sort)) {
            result.sort(Comparator.comparing(m -> (String) m.get("name")));
        } else if ("rating".equals(sort)) {
            result.sort((a, b) -> Double.compare(
                    ((Number) b.get("averageRating")).doubleValue(),
                    ((Number) a.get("averageRating")).doubleValue()));
        } else {
            result.sort((a, b) -> Long.compare(
                    ((Number) b.get("mentorCount")).longValue(),
                    ((Number) a.get("mentorCount")).longValue()));
        }

        return ResponseEntity.ok(result);
    }

    // ── GET /api/browse/skills/search?q={query} ───────────────────────────────
    @GetMapping("/skills/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchSkills(@RequestParam(defaultValue = "") String q) {
        List<Skill> skills = q.trim().length() < 1
                ? skillRepository.findAll()
                : skillRepository.findByNameContainingIgnoreCase(q.trim());

        List<Map<String, Object>> result = skills.stream().map(skill -> {
            long mentorCount = skillRepository.countMentorsBySkillId(skill.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", skill.getId());
            m.put("name", skill.getName());
            m.put("category", skill.getCategory() != null ? skill.getCategory() : "");
            m.put("mentorCount", mentorCount);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── GET /api/browse/skills/{skillId}/mentors?sort=rating|sessions|level ──
    @GetMapping("/skills/{skillId}/mentors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMentorsForSkill(@PathVariable Long skillId,
                                                 @RequestParam(defaultValue = "rating") String sort) {
        if (!skillRepository.existsById(skillId)) {
            return ResponseEntity.notFound().build();
        }

        List<UserSkill> mentorSkills = userSkillRepository.findMentorsBySkillId(skillId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (UserSkill us : mentorSkills) {
            User user = us.getUser();
            if (user == null) continue;

            Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
            if (avgRating == null) avgRating = 0.0;
            long totalSessions = sessionRepository.countCompletedMentorSessions(user);
            long reviewCount = reviewRepository.countByReviewee(user);
            boolean availableThisWeek = !availabilityRepository.findByMentor(user).isEmpty();
            List<String> badges = badgeService.getUserBadgeNames(
                    user,
                    user.getReputation() != null ? user.getReputation() : 0,
                    totalSessions,
                    avgRating);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", user.getId());
            m.put("name", user.getName());
            m.put("location", user.getLocation() != null ? user.getLocation() : "");
            m.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
            m.put("reviewCount", reviewCount);
            m.put("totalSessions", totalSessions);
            m.put("badges", badges != null ? badges : List.of());
            m.put("proficiencyLevel", us.getLevel() != null ? us.getLevel().name() : "BEGINNER");
            m.put("availableThisWeek", availableThisWeek);
            result.add(m);
        }

        if ("sessions".equals(sort)) {
            result.sort((a, b) -> Long.compare(
                    ((Number) b.get("totalSessions")).longValue(),
                    ((Number) a.get("totalSessions")).longValue()));
        } else if ("level".equals(sort)) {
            result.sort(Comparator.comparingInt(m -> levelOrder((String) m.get("proficiencyLevel"))));
        } else {
            result.sort((a, b) -> Double.compare(
                    ((Number) b.get("averageRating")).doubleValue(),
                    ((Number) a.get("averageRating")).doubleValue()));
        }

        return ResponseEntity.ok(result);
    }

    // ── GET /api/browse/categories ────────────────────────────────────────────
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCategories() {
        List<String> categories = skillRepository.findDistinctCategories();
        List<Map<String, Object>> result = categories.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(cat -> {
                    long count = skillRepository.countByCategory(cat);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", cat);
                    m.put("count", count);
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private int levelOrder(String level) {
        if (level == null) return 4;
        return switch (level) {
            case "EXPERT"       -> 0;
            case "ADVANCED"     -> 1;
            case "INTERMEDIATE" -> 2;
            default             -> 3;
        };
    }
}
