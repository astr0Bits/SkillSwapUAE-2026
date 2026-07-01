package repository;

import model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByPublishedTrueOrderByThresholdValueAsc();
    List<Badge> findAllByOrderByCriteriaTypeAscThresholdValueAsc();
    Optional<Badge> findByName(String name);
}
