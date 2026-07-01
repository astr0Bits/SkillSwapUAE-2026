package repository;

import model.Badge;
import model.User;
import model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    boolean existsByUserAndBadge(User user, Badge badge);
    List<UserBadge> findByUser(User user);
}
