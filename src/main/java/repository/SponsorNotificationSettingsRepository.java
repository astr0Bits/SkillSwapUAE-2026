package repository;

import model.SponsorNotificationSettings;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SponsorNotificationSettingsRepository extends JpaRepository<SponsorNotificationSettings, Long> {
    Optional<SponsorNotificationSettings> findByUser(User user);
}
