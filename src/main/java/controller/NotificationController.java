// controller/NotificationController.java
package controller;

import dto.NotificationDTO;
import model.User;
import repository.UserRepository;
import service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final UserRepository userRepository;

    public NotificationController(NotificationService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(service.getUserNotifications(user));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication auth) {
        User user = getUser(auth);
        service.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> clearAll(Authentication auth) {
        User user = getUser(auth);
        service.clearAll(user);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}