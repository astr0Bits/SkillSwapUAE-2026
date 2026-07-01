package controller;

import dto.ReviewDTO;
import dto.SubmitReviewDTO;
import enums.SessionStatus;
import model.Review;
import model.Session;
import model.User;
import repository.ReviewRepository;
import repository.SessionRepository;
import repository.UserRepository;
import service.NotificationService;
import exception.ReviewAlreadyExistsException;
import validator.InputSanitizer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    public ReviewController(ReviewRepository reviewRepository,
                            UserRepository userRepository,
                            SessionRepository sessionRepository,
                            NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    // ── Existing: reviews received by the current user ───────────────────────

    @GetMapping("/me")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(Authentication auth) {
        User user = getUserFromAuth(auth);
        List<Review> reviews = reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(reviews.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    // ── NEW: submit a review ──────────────────────────────────────────────────

    @PostMapping("/submit")
    public ResponseEntity<?> submitReview(@Valid @RequestBody SubmitReviewDTO dto, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        User reviewer = getUserFromAuth(auth);

        Session session = sessionRepository.findById(dto.getSessionId()).orElse(null);
        if (session == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session not found"));
        }
        if (session.getStatus() != SessionStatus.COMPLETED) {
            return ResponseEntity.badRequest().body(Map.of("error", "You can only review completed sessions"));
        }

        User mentor  = session.getMentor();
        User learner = session.getLearner();
        boolean isLearner = learner != null && learner.getId().equals(reviewer.getId());
        boolean isMentor  = mentor  != null && mentor.getId().equals(reviewer.getId());

        if (!isLearner && !isMentor) {
            return ResponseEntity.badRequest().body(Map.of("error", "You are not a participant in this session"));
        }

        if (reviewRepository.findBySessionAndReviewer(session, reviewer).isPresent()) {
            throw new ReviewAlreadyExistsException("You have already reviewed this session");
        }

        User reviewee = isLearner ? mentor : learner;
        if (reviewee == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot determine reviewee for this session"));
        }

        String comment = dto.getComment() != null ? InputSanitizer.sanitize(dto.getComment()) : null;

        Review review = new Review();
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setSession(session);
        review.setRating(dto.getRating());
        review.setComment(comment);
        reviewRepository.save(review);

        notificationService.addNotification(reviewee,
                "You received a new " + dto.getRating() + "-star review");

        Double avgRating = reviewRepository.findAverageRatingByReviewee(reviewee);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("review", toDTO(review));
        response.put("revieweeAvgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        return ResponseEntity.status(201).body(response);
    }

    // ── NEW: reviews for a specific session (check "already reviewed") ────────

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionReviews(@PathVariable Long sessionId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        User user = getUserFromAuth(auth);
        User mentor  = session.getMentor();
        User learner = session.getLearner();
        boolean isParticipant = (mentor  != null && mentor.getId().equals(user.getId())) ||
                                (learner != null && learner.getId().equals(user.getId()));
        if (!isParticipant) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        List<ReviewDTO> dtos = reviewRepository.findBySessionOrderByCreatedAtDesc(session)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ── NEW: can the current user still review this session? ──────────────────

    @GetMapping("/can-review/{sessionId}")
    public ResponseEntity<?> canReview(@PathVariable Long sessionId, Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (auth == null || !auth.isAuthenticated()) {
            result.put("canReview", false);
            result.put("reason", "not_authenticated");
            return ResponseEntity.ok(result);
        }
        User user = getUserFromAuth(auth);
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            result.put("canReview", false);
            result.put("reason", "session_not_found");
            return ResponseEntity.ok(result);
        }
        if (session.getStatus() != SessionStatus.COMPLETED) {
            result.put("canReview", false);
            result.put("reason", "not_completed");
            return ResponseEntity.ok(result);
        }
        User mentor  = session.getMentor();
        User learner = session.getLearner();
        boolean isParticipant = (mentor  != null && mentor.getId().equals(user.getId())) ||
                                (learner != null && learner.getId().equals(user.getId()));
        if (!isParticipant) {
            result.put("canReview", false);
            result.put("reason", "not_participant");
            return ResponseEntity.ok(result);
        }
        if (reviewRepository.findBySessionAndReviewer(session, user).isPresent()) {
            result.put("canReview", false);
            result.put("reason", "already_reviewed");
            return ResponseEntity.ok(result);
        }
        result.put("canReview", true);
        result.put("reason", null);
        return ResponseEntity.ok(result);
    }

    // ── NEW: public view of a mentor's reviews ────────────────────────────────

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<?> getMentorReviews(@PathVariable Long mentorId) {
        User mentor = userRepository.findById(mentorId).orElse(null);
        if (mentor == null) {
            return ResponseEntity.notFound().build();
        }
        List<Review> reviews = reviewRepository.findByRevieweeOrderByCreatedAtDesc(mentor);
        Double avgRating = reviewRepository.findAverageRatingByReviewee(mentor);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reviews", reviews.stream().map(this::toDTO).collect(Collectors.toList()));
        response.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        response.put("totalReviews", reviews.size());
        return ResponseEntity.ok(response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());
        dto.setReviewerName(r.getReviewer() != null ? r.getReviewer().getName() : "Anonymous");
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
