package repository;

import model.SponsorProgram;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorProgramRepository extends JpaRepository<SponsorProgram, Long> {
    List<SponsorProgram> findBySponsor(User sponsor);
    List<SponsorProgram> findBySponsorAndStatus(User sponsor, String status);
}
