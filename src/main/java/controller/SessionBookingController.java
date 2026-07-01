package controller;

import dto.BookSessionRequest;
import dto.CreateSessionDTO;
import enums.SessionMode;
import enums.SessionStatus;
import model.CreditTransaction;
import model.MentorAvailability;
import model.Session;
import model.Skill;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import security.UserDetailsImpl;
import service.AiSummaryService;
import service.SessionEmailService;
import exception.AvailabilityException;
import exception.ResourceNotFoundException;

import org.springframework.transaction.annotation.Transactional;
import validator.InputSanitizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/sessions")
public class SessionBookingController {

    private static final Logger log = LoggerFactory.getLogger(SessionBookingController.class);

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SkillRepository skillRepository;
    private final MentorAvailabilityRepository availabilityRepository;
    private final SessionEmailService emailService;
    private final AiSummaryService aiSummaryService;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;

    public SessionBookingController(UserRepository userRepository,
                                    SessionRepository sessionRepository,
                                    SkillRepository skillRepository,
                                    MentorAvailabilityRepository availabilityRepository,
                                    SessionEmailService emailService,
                                    AiSummaryService aiSummaryService,
                                    CreditTransactionRepository creditTransactionRepository,
                                    AssessmentAttemptRepository assessmentAttemptRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.skillRepository = skillRepository;
        this.availabilityRepository = availabilityRepository;
        this.emailService = emailService;
        this.aiSummaryService = aiSummaryService;
        this.creditTransactionRepository = creditTransactionRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
    }

    // ── Mentor: create a session ──────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<?> createSession(Authentication authentication,
                                           @RequestBody CreateSessionDTO dto) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User mentor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        // scheduledTime must be in the future
        if (dto.getScheduledTime() == null || !dto.getScheduledTime().isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scheduled time must be in the future"));
        }

        // durationMinutes 15–480
        int duration = dto.getDurationMinutes();
        if (duration < 15 || duration > 480) {
            return ResponseEntity.badRequest().body(Map.of("error", "Duration must be between 15 and 480 minutes"));
        }

        // maxLearners 1–50
        int maxLearners = dto.getMaxLearners() < 1 ? 1 : dto.getMaxLearners();
        if (maxLearners > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "Max learners must be between 1 and 50"));
        }

        // mode required
        String mode = dto.getMode();
        if (mode == null || mode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mode is required (ONLINE or IN_PERSON)"));
        }

        // mode-specific field validation
        if ("ONLINE".equalsIgnoreCase(mode)) {
            String link = dto.getMeetingLink();
            if (link == null || link.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Meeting link is required for online sessions"));
            }
            if (!link.toLowerCase().startsWith("https://")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Meeting link must be a valid https:// URL"));
            }
        } else if ("IN_PERSON".equalsIgnoreCase(mode)) {
            String loc = dto.getPhysicalLocation();
            if (loc == null || loc.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Physical location is required for in-person sessions"));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Mode must be ONLINE or IN_PERSON"));
        }

        // skill required
        if (dto.getSkillId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Skill is required"));
        }
        Skill skill = skillRepository.findById(dto.getSkillId()).orElse(null);
        if (skill == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Skill not found"));
        }

        Session session = new Session();
        session.setMentor(mentor);
        session.setSkill(skill);
        session.setMode(SessionMode.valueOf(mode.toUpperCase()));
        session.setStatus(SessionStatus.OPEN);
        session.setScheduledTime(dto.getScheduledTime());
        session.setDurationMinutes(duration);
        session.setMaxLearners(maxLearners);
        session.setCurrentLearners(0);
        String desc = dto.getDescription();
        session.setDescription(desc != null ? InputSanitizer.sanitize(desc) : null);
        String tools = dto.getToolsNeeded();
        session.setToolsNeeded(tools != null ? InputSanitizer.sanitize(tools) : null);

        if ("ONLINE".equalsIgnoreCase(mode)) {
            session.setMeetingLink(dto.getMeetingLink());
        } else {
            session.setPhysicalLocation(dto.getPhysicalLocation());
        }

        Session saved = sessionRepository.save(session);
        return ResponseEntity.status(201).body(toSessionDTO(saved));
    }

    // ── Learner: book a session ────────────────────────────────────────────────

    @PostMapping("/book")
    public ResponseEntity<?> bookSession(Authentication authentication,
                                         @RequestBody BookSessionRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        Long learnerId = principal.getId();
        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Session session;
        String skillName;

        if (request.getSessionId() != null) {
            // ── Book an existing OPEN session created by the mentor ──────────
            session = sessionRepository.findById(request.getSessionId()).orElse(null);
            if (session == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Session not found"));
            }
            if (session.getStatus() != SessionStatus.OPEN) {
                return ResponseEntity.badRequest().body(Map.of("error", "Session is no longer available for booking"));
            }
            if (session.getMentor().getId().equals(learnerId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You cannot book your own session"));
            }
            if (session.getCurrentLearners() >= session.getMaxLearners()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Session is full"));
            }

            int requiredCredits = calculateRequiredCredits(
                    session.getDurationMinutes() != null ? session.getDurationMinutes() : 60);
            int learnerCredits = learner.getCredits() != null ? learner.getCredits() : 0;
            if (learnerCredits < requiredCredits) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Insufficient credits. This session requires " + requiredCredits
                        + " credits, you have " + learnerCredits + "."));
            }

            session.setLearner(learner);
            session.setCurrentLearners(session.getCurrentLearners() + 1);
            if (session.getCurrentLearners() >= session.getMaxLearners()) {
                session.setStatus(SessionStatus.SCHEDULED);
            } else {
                session.setStatus(SessionStatus.PENDING);
            }
            sessionRepository.save(session);
            skillName = session.getSkill() != null ? session.getSkill().getName() : "Session";

        } else {
            // ── Create a new session request (learner-initiated flow) ─────────
            if (request.getMentorId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "mentorId is required"));
            }
            User mentor = userRepository.findById(request.getMentorId()).orElse(null);
            if (mentor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mentor not found"));
            }
            if (learner.getId().equals(mentor.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "You cannot book a session with yourself"));
            }

            LocalDateTime scheduledTime = request.getScheduledTime();
            if (scheduledTime == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "scheduledTime is required (ISO-8601, e.g. 2026-06-20T10:00:00)"));
            }
            if (scheduledTime.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "scheduledTime must be in the future"));
            }

            if (!isWithinMentorAvailability(mentor, scheduledTime, 60)) {
                throw new AvailabilityException("Requested time slot is outside mentor availability");
            }
            if (sessionRepository.hasMentorConflict(
                    mentor, scheduledTime.minusMinutes(60), scheduledTime.plusMinutes(60))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mentor already has a booking that overlaps this time slot"));
            }

            // Learner-initiated sessions default to 60 minutes = 1 credit
            int requiredCreditsNew = calculateRequiredCredits(60);
            int learnerCreditsNew = learner.getCredits() != null ? learner.getCredits() : 0;
            if (learnerCreditsNew < requiredCreditsNew) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Insufficient credits. This session requires " + requiredCreditsNew
                        + " credits, you have " + learnerCreditsNew + "."));
            }

            Skill skill = null;
            if (request.getSkillId() != null) {
                skill = skillRepository.findById(request.getSkillId()).orElse(null);
            }
            if (skill == null && request.getSkillName() != null) {
                skill = skillRepository.findByNameIgnoreCase(request.getSkillName()).orElse(null);
            }
            skillName = skill != null ? skill.getName()
                    : (request.getSkillName() != null ? request.getSkillName() : "Session");

            boolean isOnline = !"in-person".equalsIgnoreCase(request.getMode());

            session = new Session();
            session.setMentor(mentor);
            session.setLearner(learner);
            session.setSkill(skill);
            session.setMode(isOnline ? SessionMode.ONLINE : SessionMode.IN_PERSON);
            session.setStatus(SessionStatus.PENDING);
            session.setScheduledTime(scheduledTime);
            session.setDurationMinutes(60);
            session.setMaxLearners(1);
            session.setCurrentLearners(1);
            sessionRepository.save(session);
        }

        final User mentor = session.getMentor();
        final String finalSkillName = skillName;
        try {
            emailService.sendBookingConfirmation(learner, mentor, session, finalSkillName);
        } catch (Exception e) {
            System.err.println("=== LEARNER EMAIL FAILED: " + e.getMessage());
        }
        try {
            emailService.sendMentorNotification(mentor, learner, session, finalSkillName);
        } catch (Exception e) {
            System.err.println("=== MENTOR EMAIL FAILED: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "message", "Session booked successfully",
                "sessionId", session.getId(),
                "mode", session.getMode() != null ? session.getMode().name() : "",
                "scheduledTime", session.getScheduledTime() != null ? session.getScheduledTime().toString() : "",
                "meetingLink", session.getMeetingLink() != null ? session.getMeetingLink() : "",
                "physicalLocation", session.getPhysicalLocation() != null ? session.getPhysicalLocation() : ""
        ));
    }

    // ── Learner: upcoming + history sessions ──────────────────────────────────

    @GetMapping("/me/upcoming")
    public ResponseEntity<?> getMyUpcomingSessions(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Session> sessions = sessionRepository.findActiveSessionsForUser(user, LocalDateTime.now());
        return ResponseEntity.ok(sessions.stream().map(this::toSessionDTO).collect(Collectors.toList()));
    }

    @GetMapping("/me/history")
    public ResponseEntity<?> getMySessionHistory(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Session> sessions = sessionRepository.findCompletedSessionsForUser(user);
        return ResponseEntity.ok(sessions.stream().map(this::toSessionDTO).collect(Collectors.toList()));
    }

    // ── Mentor: pending requests for their sessions ───────────────────────────

    @GetMapping("/mentor/pending")
    public ResponseEntity<?> getMentorPendingSessions(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User mentor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Session> sessions = sessionRepository.findByMentorAndStatus(mentor, SessionStatus.PENDING);
        return ResponseEntity.ok(sessions.stream().map(this::toSessionDTO).collect(Collectors.toList()));
    }

    // ── Shared DTO helper ──────────────────────────────────────────────────────

    private Map<String, Object> toSessionDTO(Session s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("skillName", s.getSkill() != null ? s.getSkill().getName() : "");
        m.put("skillId", s.getSkill() != null ? s.getSkill().getId() : null);
        m.put("mentorId", s.getMentor() != null ? s.getMentor().getId() : null);
        m.put("mentorName", s.getMentor() != null ? s.getMentor().getName() : "");
        m.put("mentorEmail", s.getMentor() != null ? s.getMentor().getEmail() : "");
        m.put("learnerId", s.getLearner() != null ? s.getLearner().getId() : null);
        m.put("learnerName", s.getLearner() != null ? s.getLearner().getName() : "");
        m.put("learnerEmail", s.getLearner() != null ? s.getLearner().getEmail() : "");
        m.put("scheduledTime", s.getScheduledTime() != null ? s.getScheduledTime().toString() : "");
        m.put("durationMinutes", s.getDurationMinutes());
        m.put("mode", s.getMode() != null ? s.getMode().name() : "");
        m.put("status", s.getStatus() != null ? s.getStatus().name() : "");
        m.put("meetingLink", s.getMeetingLink() != null ? s.getMeetingLink() : "");
        m.put("physicalLocation", s.getPhysicalLocation() != null ? s.getPhysicalLocation()
                : (s.getLocation() != null ? s.getLocation() : ""));
        m.put("maxLearners", s.getMaxLearners());
        m.put("currentLearners", s.getCurrentLearners());
        m.put("description", s.getDescription() != null ? s.getDescription() : "");
        m.put("aiSummary", s.getAiSummary() != null ? s.getAiSummary() : "");
        return m;
    }

    // ── Mentor: mark session complete + trigger AI summary ────────────────────

    @PutMapping("/{sessionId}/complete")
    @Transactional
    public ResponseEntity<?> completeSession(@PathVariable Long sessionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();

        if (!session.getMentor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Only the mentor can mark a session as complete"));
        }
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session is already marked complete"));
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot complete a cancelled session"));
        }

        String skillName   = session.getSkill()    != null ? session.getSkill().getName()    : "General";
        int    duration    = session.getDurationMinutes() != null ? session.getDurationMinutes() : 60;
        String learnerName = session.getLearner()  != null ? session.getLearner().getName()  : "Learner";
        String mentorName  = session.getMentor().getName();

        String summary;
        try {
            summary = aiSummaryService.generateSummary(skillName, duration, learnerName, mentorName);
        } catch (Exception e) {
            summary = "Summary unavailable — please check back later.";
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setAiSummary(summary);
        sessionRepository.save(session);

        // Credit economy: mentor earns, learner pays (1 credit per hour, minimum 1)
        int creditsEarned = calculateRequiredCredits(duration);
        String reason = "Session completed: " + skillName;

        user.setCredits((user.getCredits() != null ? user.getCredits() : 50) + creditsEarned);
        userRepository.save(user);
        creditTransactionRepository.save(new CreditTransaction(user, creditsEarned, reason, session.getId()));

        if (session.getLearner() != null) {
            User learner = userRepository.findById(session.getLearner().getId()).orElse(null);
            if (learner != null) {
                learner.setCredits((learner.getCredits() != null ? learner.getCredits() : 0) - creditsEarned);
                userRepository.save(learner);
                creditTransactionRepository.save(new CreditTransaction(learner, -creditsEarned, reason, session.getId()));
            }
        }

        return ResponseEntity.ok(toSessionDTO(session));
    }

    // ── Learner: check for a post-session assessment that hasn't been taken yet ──

    @GetMapping("/me/pending-assessment")
    public ResponseEntity<?> pendingAssessment(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Session> completed = sessionRepository.findCompletedSessionsForLearner(user);
        for (Session s : completed) {
            Long skillId = s.getSkill().getId();
            if (!assessmentAttemptRepository.existsByUserAndSkillId(user, skillId)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("skillId", skillId);
                result.put("skillName", s.getSkill().getName());
                result.put("sessionId", s.getId());
                return ResponseEntity.ok(result);
            }
        }
        return ResponseEntity.noContent().build();
    }

    // ── Credit helpers ────────────────────────────────────────────────────────

    private int calculateRequiredCredits(int durationMinutes) {
        return Math.max(1, (int) Math.round(durationMinutes / 60.0));
    }

    // ── Availability helpers ───────────────────────────────────────────────────

    private boolean isWithinMentorAvailability(User mentor, LocalDateTime scheduledTime, int durationMinutes) {
        List<MentorAvailability> slots = availabilityRepository.findByMentor(mentor);
        if (slots.isEmpty()) return true;

        String requestedDay = scheduledTime.getDayOfWeek().name();
        LocalTime sessionStart = scheduledTime.toLocalTime();
        LocalTime sessionEnd = sessionStart.plusMinutes(durationMinutes);

        for (MentorAvailability slot : slots) {
            boolean dayMatches = slot.isRecurring()
                    ? requestedDay.equalsIgnoreCase(slot.getDayOfWeek())
                    : slot.getSpecificDate() != null && slot.getSpecificDate().equals(scheduledTime.toLocalDate());

            if (dayMatches) {
                LocalTime start = slot.getStartTime();
                LocalTime end = slot.getEndTime();
                if (start != null && end != null
                        && !sessionStart.isBefore(start)
                        && !sessionEnd.isAfter(end)) {
                    return true;
                }
            }
        }
        return false;
    }
}
