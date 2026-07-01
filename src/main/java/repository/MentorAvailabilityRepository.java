package repository;

import model.MentorAvailability;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {
    List<MentorAvailability> findByMentor(User mentor);
    List<MentorAvailability> findByMentorAndRecurringTrue(User mentor);
    List<MentorAvailability> findByMentorAndSpecificDate(User mentor, LocalDate date);
    @Modifying
    @Query(value = "DELETE FROM mentor_availability WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);}