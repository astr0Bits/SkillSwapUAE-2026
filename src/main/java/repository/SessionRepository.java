package repository;

import model.Session;
import model.User;
import enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByMentorAndStatus(User mentor, SessionStatus status);
    List<Session> findByLearnerAndStatus(User learner, SessionStatus status);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE (s.mentor = :user OR s.learner = :user) AND s.status = 'COMPLETED'")
    long countCompletedSessionsForUser(@Param("user") User user);
    
    @Query("SELECT s FROM Session s WHERE (s.mentor = :user OR s.learner = :user) AND s.status = 'SCHEDULED' AND s.scheduledTime > :now ORDER BY s.scheduledTime ASC")
    List<Session> findUpcomingSessionsForUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Session s WHERE (s.mentor = :user OR s.learner = :user) AND s.status = 'COMPLETED' ORDER BY s.scheduledTime DESC")
    List<Session> findCompletedSessionsForUser(@Param("user") User user);

    @Query("SELECT s FROM Session s WHERE s.learner = :user AND s.status = 'COMPLETED' AND s.skill IS NOT NULL ORDER BY s.updatedAt DESC")
    List<Session> findCompletedSessionsForLearner(@Param("user") User user);
    
    @Query("SELECT s FROM Session s WHERE s.mentor = :mentor AND s.status = 'COMPLETED' AND s.scheduledTime BETWEEN :start AND :end")
    List<Session> findCompletedSessionsByMentorAndDateRange(@Param("mentor") User mentor,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.mentor = :mentor AND s.status = :status")
    long countCompletedMentorSessions(@Param("mentor") User mentor, @Param("status") SessionStatus status);
    
    default long countCompletedMentorSessions1(User mentor) {
        return countCompletedMentorSessions(mentor, SessionStatus.COMPLETED);
    }
    // True if the mentor has any non-cancelled session whose window [start, start+duration)
    // overlaps the new session's window [windowStart, windowEnd).
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Session s " +
           "WHERE s.mentor = :mentor " +
           "AND s.status <> enums.SessionStatus.CANCELLED " +
           "AND s.scheduledTime < :windowEnd " +
           "AND s.scheduledTime > :windowStart")
    boolean hasMentorConflict(@Param("mentor") User mentor,
                              @Param("windowStart") LocalDateTime windowStart,
                              @Param("windowEnd") LocalDateTime windowEnd);

    @Query("SELECT s FROM Session s WHERE (s.mentor = :user OR s.learner = :user) " +
           "AND s.status IN ('PENDING','SCHEDULED','OPEN') " +
           "AND s.scheduledTime > :now ORDER BY s.scheduledTime ASC")
    List<Session> findActiveSessionsForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = "DELETE FROM sessions WHERE mentor_id = :userId OR learner_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
 // ADD to existing SessionRepository:

 // Find all OPEN sessions for browsing
    @Query("SELECT s FROM Session s WHERE s.status = enums.SessionStatus.OPEN AND s.scheduledTime > :now ORDER BY s.scheduledTime ASC")
    List<Session> findAvailableSessions(@Param("now") LocalDateTime now);
 // Find sessions created by a mentor
 List<Session> findByMentorOrderByScheduledTimeDesc(User mentor);

 // Count completed mentor sessions (already used in BrowseService)
 @Query("SELECT COUNT(s) FROM Session s WHERE s.mentor = :user AND s.status = 'COMPLETED'")
 long countCompletedMentorSessions(@Param("user") User user);
}