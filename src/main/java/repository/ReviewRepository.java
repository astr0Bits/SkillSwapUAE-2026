package repository;

import model.Review;
import model.Session;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee);
    List<Review> findByReviewerOrderByCreatedAtDesc(User reviewer);
    List<Review> findBySessionOrderByCreatedAtDesc(Session session);
    long countByReviewee(User reviewee);

    Optional<Review> findBySessionAndReviewer(Session session, User reviewer);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee = :reviewee")
    Double findAverageRatingByReviewee(@Param("reviewee") User reviewee);

    @Modifying
    @Query(value = "DELETE FROM reviews WHERE reviewer_id = :userId OR reviewee_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
}