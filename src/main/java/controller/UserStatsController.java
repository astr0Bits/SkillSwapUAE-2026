// controller/UserStatsController.java
package controller;

import dto.UserStatsDTO;
import model.User;
import repository.UserRepository;
import service.UserStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/stats")
public class UserStatsController {
    private final UserStatsService statsService;
    private final UserRepository userRepository;

    public UserStatsController(UserStatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserStatsDTO> getStats(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(statsService.getUserStats(user));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @GetMapping("/earnings")
    public ResponseEntity<?> getEarnings(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(statsService.getEarningsBreakdown(user));
    }
}