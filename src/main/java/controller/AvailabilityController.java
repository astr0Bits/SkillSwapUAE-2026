package controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import model.MentorAvailability;
import model.User;
import repository.UserRepository;
import service.AvailabilityService;

@RestController
@RequestMapping("/api/users/me/availability")
public class AvailabilityController {
	private final AvailabilityService service;
	private final UserRepository userRepository;

	public AvailabilityController(AvailabilityService service, UserRepository userRepository) {
		this.service = service;
		this.userRepository = userRepository;
	}

	@GetMapping
	public ResponseEntity<List<MentorAvailability>> getMyAvailability(Authentication auth) {
		User user = getUser(auth);
		return ResponseEntity.ok(service.getAvailability(user));
	}

	@PostMapping
	public ResponseEntity<?> addSlot(@RequestBody MentorAvailability slot, Authentication auth) {
		User user = getUser(auth);
		MentorAvailability saved = service.addSlot(user, slot);
		return ResponseEntity.ok(saved);
	}

	@DeleteMapping("/{slotId}")
	public ResponseEntity<?> removeSlot(@PathVariable Long slotId, Authentication auth) {
		User user = getUser(auth);
		service.removeSlot(slotId, user);
		return ResponseEntity.ok(Map.of("message", "Slot removed"));
	}

	private User getUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName())
				.orElseThrow(() -> new RuntimeException("User not found"));
	}
}