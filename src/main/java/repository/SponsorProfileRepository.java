package repository;

import model.SponsorProfile;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SponsorProfileRepository extends JpaRepository<SponsorProfile, Long> {
    Optional<SponsorProfile> findByUser(User user);
}
