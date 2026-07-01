package repository;
/*Provides data access for the User entity. 
 * It’s used by UserService and UserController 
 * (and likely by the authentication service) 
 * to fetch, save, and delete users. The existence methods are used 
 * for validation (e.g., checking if email is already taken).
*/
import enums.Role;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    void deleteByEmail(String email);
    List<User> findByRoleAndEnabled(Role role, boolean enabled);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE " +
           "(SELECT COUNT(s) FROM Session s WHERE (s.mentor = u OR s.learner = u) AND s.status = 'COMPLETED') >= :n")
    long countUsersWithMinCompletedSessions(@Param("n") long n);

    @Query("SELECT COUNT(u) FROM User u WHERE " +
           "COALESCE((SELECT AVG(r.rating) FROM Review r WHERE r.reviewee = u), 0) * 20 >= :n")
    long countUsersWithMinReputation(@Param("n") int n);
}