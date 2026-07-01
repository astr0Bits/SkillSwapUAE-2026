// repository/SkillRepository.java
package repository;

import model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByNameContainingIgnoreCase(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(DISTINCT us.user.id) FROM UserSkill us WHERE us.skill.id = :skillId AND us.type = 'MENTOR'")
    long countMentorsBySkillId(@Param("skillId") Long skillId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id IN " +
           "(SELECT us.user.id FROM UserSkill us WHERE us.skill.id = :skillId AND us.type = 'MENTOR')")
    Double findAverageRatingBySkillId(@Param("skillId") Long skillId);

    @Query("SELECT DISTINCT s.category FROM Skill s WHERE s.category IS NOT NULL AND s.category <> '' ORDER BY s.category ASC")
    List<String> findDistinctCategories();

    long countByCategory(String category);
}