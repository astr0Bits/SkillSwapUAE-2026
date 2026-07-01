package service;

import dto.BadgeDTO;
import enums.BadgeCriteriaType;
import model.Badge;
import model.User;
import model.UserBadge;
import repository.BadgeRepository;
import repository.ReviewRepository;
import repository.SessionRepository;
import repository.UserBadgeRepository;
import repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;

    public BadgeService(BadgeRepository badgeRepository,
                        SessionRepository sessionRepository,
                        ReviewRepository reviewRepository,
                        UserRepository userRepository,
                        UserBadgeRepository userBadgeRepository) {
        this.badgeRepository = badgeRepository;
        this.sessionRepository = sessionRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    // ── Public: all published badge definitions ──────────────────────────────

    public List<BadgeDTO> getAllPublishedBadges() {
        return badgeRepository.findByPublishedTrueOrderByThresholdValueAsc()
                .stream()
                .map(this::toDefinitionDTO)
                .collect(Collectors.toList());
    }

    // ── Authenticated user: progress on all published badges ─────────────────

    public List<BadgeDTO> getBadgeProgress(User user) {
        long completedSessions = sessionRepository.countCompletedSessionsForUser(user);
        Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
        int reputation = avgRating == null ? 0 : (int) Math.round(avgRating * 20);

        List<BadgeDTO> result = new ArrayList<>(
                badgeRepository.findByPublishedTrueOrderByThresholdValueAsc()
                        .stream()
                        .map(b -> toProgressDTO(b, completedSessions, reputation, user))
                        .collect(Collectors.toList()));

        // Also include CUSTOM assessment badges earned by this user even if unpublished
        Set<Long> alreadyIncluded = result.stream()
                .filter(b -> b.getId() != null)
                .map(BadgeDTO::getId)
                .collect(Collectors.toSet());

        for (UserBadge ub : userBadgeRepository.findByUser(user)) {
            Badge b = ub.getBadge();
            if (b.getCriteriaType() == BadgeCriteriaType.CUSTOM && !alreadyIncluded.contains(b.getId())) {
                BadgeDTO dto = toDefinitionDTO(b);
                dto.setEarned(true);
                dto.setProgress("Earned");
                dto.setProgressPercent(100);
                result.add(dto);
            }
        }

        return result;
    }

    // ── Admin: all badges (including unpublished) with earned counts ──────────

    public List<BadgeDTO> getAllBadgesForAdmin() {
        return badgeRepository.findAllByOrderByCriteriaTypeAscThresholdValueAsc()
                .stream()
                .map(b -> {
                    BadgeDTO dto = toDefinitionDTO(b);
                    dto.setPublished(b.isPublished());
                    dto.setEarnedCount(computeEarnedCount(b));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public long computeEarnedCount(Badge badge) {
        if (badge.getCriteriaType() == BadgeCriteriaType.SESSION_COUNT) {
            return userRepository.countUsersWithMinCompletedSessions(badge.getThresholdValue());
        }
        if (badge.getCriteriaType() == BadgeCriteriaType.RATING_THRESHOLD) {
            return userRepository.countUsersWithMinReputation(badge.getThresholdValue());
        }
        return 0; // CUSTOM badges — count is not applicable
    }

    // ── Legacy: for /api/badges/me and /api/users/me/badges ─────────────────

    public List<BadgeDTO> getUserBadges(User user, int points, long totalSessions, int reputation) {
        long sessions = sessionRepository.countCompletedSessionsForUser(user);
        Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
        int rep = avgRating == null ? 0 : (int) Math.round(avgRating * 20);

        List<Badge> badges = badgeRepository.findByPublishedTrueOrderByThresholdValueAsc();
        if (badges.isEmpty()) {
            return legacyHardcodedBadges(points, sessions, rep);
        }
        return badges.stream()
                .map(b -> toProgressDTO(b, sessions, rep, user))
                .collect(Collectors.toList());
    }

    // ── Legacy: for browse page (badge name strings) ─────────────────────────

    public List<String> getUserBadgeNames(User user, int reputation, long completedSessions, double avgRating) {
        List<String> badgeNames = new ArrayList<>();
        if (reputation >= 90) badgeNames.add("five-star");
        if (completedSessions >= 50) badgeNames.add("master");
        if (completedSessions >= 10 && avgRating >= 4.5) badgeNames.add("verified");
        if (completedSessions >= 20) badgeNames.add("expert");
        return badgeNames;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BadgeDTO toDefinitionDTO(Badge b) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(b.getId());
        dto.setName(b.getName());
        dto.setIcon(b.getIconName());
        dto.setDescription(b.getDescription());
        dto.setRequirement(b.getDescription());
        dto.setCriteriaType(b.getCriteriaType().name());
        dto.setThresholdValue(b.getThresholdValue());
        dto.setPublished(b.isPublished());
        return dto;
    }

    private BadgeDTO toProgressDTO(Badge b, long completedSessions, int reputation, User user) {
        if (b.getCriteriaType() == BadgeCriteriaType.CUSTOM) {
            BadgeDTO dto = toDefinitionDTO(b);
            boolean earned = userBadgeRepository.existsByUserAndBadge(user, b);
            dto.setEarned(earned);
            dto.setProgress(earned ? "Earned" : "Not yet earned");
            dto.setProgressPercent(earned ? 100 : 0);
            return dto;
        }
        BadgeDTO dto = toDefinitionDTO(b);
        int threshold = b.getThresholdValue();
        int current = b.getCriteriaType() == BadgeCriteriaType.SESSION_COUNT
                ? (int) Math.min(completedSessions, Integer.MAX_VALUE)
                : reputation;

        boolean earned = current >= threshold;
        int pct = threshold > 0
                ? Math.min(100, (int) Math.round((double) current / threshold * 100))
                : 0;
        String progress = Math.min(current, threshold) + "/" + threshold;

        dto.setEarned(earned);
        dto.setEarnedDate(null);
        dto.setProgress(progress);
        dto.setProgressPercent(pct);
        return dto;
    }

    private List<BadgeDTO> legacyHardcodedBadges(int points, long totalSessions, int reputation) {
        List<BadgeDTO> badges = new ArrayList<>();
        if (points >= 500) badges.add(new BadgeDTO("Master", "🏆", true, "500+ points"));
        else if (points >= 250) badges.add(new BadgeDTO("Adept", "🔥", true, "250+ points"));
        else if (points >= 100) badges.add(new BadgeDTO("Apprentice", "⚙️", true, "100+ points"));
        else badges.add(new BadgeDTO("Novice", "🌱", true, "0+ points"));
        badges.add(new BadgeDTO("Five-Star Mentor", "⭐", reputation >= 90, reputation >= 90 ? "Rating ≥ 4.5" : "Reach 4.5 stars"));
        badges.add(new BadgeDTO("Community Builder", "🤝", totalSessions >= 5, totalSessions >= 5 ? "5+ sessions" : "Complete 5 sessions"));
        badges.add(new BadgeDTO("Expert Swapper", "🔄", totalSessions >= 20, totalSessions >= 20 ? "20+ sessions" : "Complete 20 sessions"));
        return badges;
    }
}
