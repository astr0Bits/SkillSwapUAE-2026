package repository;

import model.Assessment;
import model.AssessmentAttempt;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    long countByUserAndAssessmentAndPassed(User user, Assessment assessment, boolean passed);
    List<AssessmentAttempt> findByUserOrderByCompletedAtDesc(User user);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AssessmentAttempt a WHERE a.user = :user AND a.assessment.skill.id = :skillId")
    boolean existsByUserAndSkillId(@Param("user") User user, @Param("skillId") Long skillId);
}
