package controller;

import dto.BadgeDTO;
import dto.UserStatsDTO;
import model.User;
import repository.UserRepository;
import service.BadgeService;
import service.UserStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {
    private final UserStatsService statsService;
    private final BadgeService badgeService;
    private final UserRepository userRepository;

    public BadgeController(UserStatsService statsService, BadgeService badgeService, UserRepository userRepository) {
        this.statsService = statsService;
        this.badgeService = badgeService;
        this.userRepository = userRepository;
    }

    /** Legacy endpoint — used by dashboard and userProfile. */
    @GetMapping("/me")
    public ResponseEntity<List<BadgeDTO>> getBadges(Authentication auth) {
        User user = getUser(auth);
        UserStatsDTO stats = statsService.getUserStats(user);
        List<BadgeDTO> badges = badgeService.getUserBadges(user, stats.getPoints(), stats.getTotalSessions(), stats.getReputation());
        return ResponseEntity.ok(badges);
    }

    /** All published badge definitions (no personal data). */
    @GetMapping("/all")
    public ResponseEntity<List<BadgeDTO>> getAllBadges() {
        return ResponseEntity.ok(badgeService.getAllPublishedBadges());
    }

    /** Current user's progress on every published badge. */
    @GetMapping("/me/progress")
    public ResponseEntity<List<BadgeDTO>> getBadgeProgress(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(badgeService.getBadgeProgress(user));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}