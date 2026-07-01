// repository/NotificationRepository.java
package repository;

import model.Notification;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    void deleteByUser(User user);
    @Modifying
    @Query(value = "DELETE FROM notifications WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);}