package repository;

import model.PasswordHistory;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findTop10ByUserOrderByCreatedAtDesc(User user);
    List<PasswordHistory> findByUserOrderByCreatedAtAsc(User user);
}
