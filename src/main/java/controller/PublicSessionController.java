package controller;

import dto.CreateSessionDTO;
import dto.SessionBrowseDTO;
import enums.SessionMode;
import model.Session;
import model.Skill;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import service.SessionEmailService;

import validator.InputSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import enums.SessionStatus;
@RestController
@RequestMapping("/api/public-sessions")
@CrossOrigin(origins = "*")
public class PublicSessionController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SkillRepository skillRepository;
    private final SessionEmailService emailService;

    public PublicSessionController(UserRepository userRepository,
                                   SessionRepository sessionRepository,
                                   SkillRepository skillRepository,
                                   SessionEmailService emailService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.skillRepository = skillRepository;
        this.emailService = emailService;
    }

    // ── CREATE a public session (mentor) ──────────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> createSession(Authentication authentication,
                                           @RequestBody CreateSessionDTO dto) {
        User mentor = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (mentor == null) return ResponseEntity.status(401).body("Not authenticated");

        // Validate
        if (dto.getScheduledTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scheduled time is required"));
        }
        // Allow times up to 1 hour in the past to handle timezone differences
        if (dto.getScheduledTime().isBefore(LocalDateTime.now().minusHours(1))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scheduled time must be in the future"));
        }
        if ("ONLINE".equalsIgnoreCase(dto.getMode()) &&
            (dto.getMeetingLink() == null || dto.getMeetingLink().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Meeting link is required for online sessions"));
        }
        if ("IN_PERSON".equalsIgnoreCase(dto.getMode()) &&
            (dto.getLocation() == null || dto.getLocation().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Location is required for in-person sessions"));
        }

        Skill skill = skillRepository.findById(dto.getSkillId()).orElse(null);
        if (skill == null) return ResponseEntity.badRequest().body(Map.of("error", "Skill not found"));

        Session session = new Session();
        session.setMentor(mentor);
        session.setSkill(skill);
        session.setMode(SessionMode.valueOf(dto.getMode().toUpperCase()));  session.setMeetingLink(dto.getMeetingLink());
        session.setLocation(dto.getLocation());
        String desc = dto.getDescription();
        session.setDescription(desc != null ? InputSanitizer.sanitize(desc) : null);
        String tools = dto.getToolsNeeded();
        session.setToolsNeeded(tools != null ? InputSanitizer.sanitize(tools) : null);
        session.setDurationMinutes(dto.getDurationMinutes() > 0 ? dto.getDurationMinutes() : 60);
        session.setMaxLearners(dto.getMaxLearners() > 0 ? dto.getMaxLearners() : 1);
        session.setCurrentLearners(0);
        session.setScheduledTime(dto.getScheduledTime());
        session.setStatus(SessionStatus.OPEN);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        Session saved = sessionRepository.save(session);

        return ResponseEntity.ok(Map.of(
            "message", "Session created and published successfully!",
            "sessionId", saved.getId()
        ));
    }

    // ── BROWSE all available sessions ─────────────────────────────
    @GetMapping("/available")
    public ResponseEntity<List<SessionBrowseDTO>> getAvailableSessions() {
        List<Session> sessions = sessionRepository.findAvailableSessions(LocalDateTime.now());
        List<SessionBrowseDTO> result = sessions.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── GET my created sessions (mentor) ──────────────────────────
    @GetMapping("/my-sessions")
    public ResponseEntity<?> getMySessions(Authentication authentication) {
        User mentor = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (mentor == null) return ResponseEntity.status(401).body("Not authenticated");

        List<Session> sessions = sessionRepository.findByMentorOrderByScheduledTimeDesc(mentor);
        return ResponseEntity.ok(sessions.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    // ── JOIN a session (learner books an open session) ────────────
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<?> joinSession(Authentication authentication,
                                         @PathVariable Long sessionId) {
        User learner = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (learner == null) return ResponseEntity.status(401).body("Not authenticated");

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.badRequest().body(Map.of("error", "Session not found"));
        if (session.getStatus() != SessionStatus.OPEN) return ResponseEntity.badRequest().body(Map.of("error", "Session is no longer available"));
        if (session.getMentor().getId().equals(learner.getId())) return ResponseEntity.badRequest().body(Map.of("error", "You cannot join your own session"));
        if (session.getCurrentLearners() >= session.getMaxLearners()) return ResponseEntity.badRequest().body(Map.of("error", "Session is full"));

        // Increment learner count
        session.setCurrentLearners(session.getCurrentLearners() + 1);
        session.setLearner(learner);

        // If full, mark as SCHEDULED
        if (session.getCurrentLearners() >= session.getMaxLearners()) {
        	session.setStatus(SessionStatus.SCHEDULED);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        String skillName = session.getSkill() != null ? session.getSkill().getName() : "Session";

        // Learner confirmation
        try {
            emailService.sendBookingConfirmation(learner, session.getMentor(), session, skillName);
        } catch (Exception e) {
            System.err.println("Learner email failed: " + e.getMessage());
        }
        // Mentor notification
        try {
            emailService.sendMentorNotification(session.getMentor(), learner, session, skillName);
        } catch (Exception e) {
            System.err.println("Mentor email failed: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "message", "Successfully joined! Check your email for details.",
            "mode", session.getMode(),
            "meetingLink", session.getMeetingLink() != null ? session.getMeetingLink() : ""
        ));
    }

    // ── DELETE own session ────────────────────────────────────────
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(Authentication authentication,
                                           @PathVariable Long sessionId) {
        User mentor = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (mentor == null) return ResponseEntity.status(401).body("Not authenticated");

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (!session.getMentor().getId().equals(mentor.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your session"));
        }

        sessionRepository.delete(session);
        return ResponseEntity.ok(Map.of("message", "Session deleted"));
    }

    private SessionBrowseDTO toDTO(Session s) {
        SessionBrowseDTO dto = new SessionBrowseDTO();
        dto.setId(s.getId());
        dto.setMentorId(s.getMentor().getId());
        dto.setMentorName(s.getMentor().getName());
        dto.setMentorEmail(s.getMentor().getEmail());
        dto.setSkillName(s.getSkill() != null ? s.getSkill().getName() : "");
        dto.setSkillCategory(s.getSkill() != null && s.getSkill().getCategory() != null
                ? s.getSkill().getCategory() : "General");
        dto.setMode(s.getMode());
        dto.setMeetingLink(s.getMeetingLink());
        dto.setLocation(s.getLocation());
        dto.setPhysicalLocation(s.getPhysicalLocation() != null ? s.getPhysicalLocation()
                : s.getLocation());
        dto.setDescription(s.getDescription());
        dto.setToolsNeeded(s.getToolsNeeded());
        dto.setDurationMinutes(s.getDurationMinutes());
        dto.setMaxLearners(s.getMaxLearners());
        dto.setCurrentLearners(s.getCurrentLearners());
        dto.setScheduledTime(s.getScheduledTime());
        dto.setStatus(s.getStatus() != null ? s.getStatus().name() : null); dto.setSkillId(s.getSkill() != null ? s.getSkill().getId() : null);
        return dto;
    }
}