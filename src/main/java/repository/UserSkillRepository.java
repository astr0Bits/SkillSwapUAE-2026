package repository;

import model.UserSkill;
import model.UserSkill.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUserId(Long userId);

    List<UserSkill> findByUserIdAndType(Long userId, SkillType type);

    boolean existsByUserIdAndSkillIdAndType(Long userId, Long skillId, SkillType type);

    @Query("SELECT us FROM UserSkill us " +
            "JOIN FETCH us.user u " +
            "JOIN FETCH us.skill s " +
            "WHERE us.type = model.UserSkill$SkillType.MENTOR " +
            "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<UserSkill> searchMentorsBySkill(@Param("query") String query);
    @Query("SELECT us FROM UserSkill us JOIN FETCH us.user WHERE us.skill.id = :skillId AND us.type = 'MENTOR'")
    List<UserSkill> findMentorsBySkillId(@Param("skillId") Long skillId);

    @Query("SELECT us.skill.id, COUNT(us) FROM UserSkill us WHERE us.type = 'MENTOR' GROUP BY us.skill.id")
    List<Object[]> countMentorsPerSkill();

    @Modifying
    @Query(value = "DELETE FROM user_skills WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
}