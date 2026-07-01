package repository;

import model.Assessment;
import model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findTopBySkillOrderByCreatedAtDesc(Skill skill);
}
