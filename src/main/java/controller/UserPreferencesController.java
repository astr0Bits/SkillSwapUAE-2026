// controller/UserPreferencesController.java
package controller;

import dto.UserPreferencesDTO;
import model.User;
import repository.UserRepository;
import service.UserPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferencesController {
    private final UserPreferencesService service;
    private final UserRepository userRepository;

    public UserPreferencesController(UserPreferencesService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserPreferencesDTO> getPreferences(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(service.getPreferences(user));
    }

    @PutMapping
    public ResponseEntity<UserPreferencesDTO> updatePreferences(@Valid @RequestBody UserPreferencesDTO dto, Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(service.updatePreferences(user, dto));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}