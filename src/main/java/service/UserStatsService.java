// service/UserStatsService.java
package service;

import dto.UserStatsDTO;
import model.Session;
import model.User;
import repository.SessionRepository;
import repository.ReviewRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserStatsService {
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;

    public UserStatsService(SessionRepository sessionRepository, ReviewRepository reviewRepository) {
        this.sessionRepository = sessionRepository;
        this.reviewRepository = reviewRepository;
    }

    public UserStatsDTO getUserStats(User user) {
        long totalSessions = sessionRepository.countCompletedSessionsForUser(user); // fixed method name
        long upcomingSessions = sessionRepository.findUpcomingSessionsForUser(user, LocalDateTime.now()).size();
        Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
        int reputation = avgRating == null ? 0 : (int) Math.round(avgRating * 20); // 0-100
        int points = (int)(totalSessions * 10 + reputation);
        
        UserStatsDTO dto = new UserStatsDTO();
        dto.setCredits(user.getCredits() != null ? user.getCredits() : 0);
        dto.setReputation(reputation);
        dto.setPoints(points);
        dto.setTotalSessions(totalSessions);
        dto.setUpcomingSessions(upcomingSessions);
        return dto;
    }
    
    public Map<String, Object> getEarningsBreakdown(User mentor) {
        long totalCredits = sessionRepository.countCompletedMentorSessions(mentor) * 3; // existing method

        // Monthly breakdown (last 12 months)
        LocalDateTime now = LocalDateTime.now();
        Map<String, Integer> monthly = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime end = start.plusMonths(1);
            List<Session> monthSessions = sessionRepository.findCompletedSessionsByMentorAndDateRange(mentor, start, end);
            String monthKey = start.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthly.put(monthKey, monthSessions.size() * 3);
        }
        return Map.of("totalCredits", totalCredits, "monthly", monthly);
    }
}