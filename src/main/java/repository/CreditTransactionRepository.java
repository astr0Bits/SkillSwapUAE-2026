package repository;

import model.CreditTransaction;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t WHERE t.user = :user AND t.amount > 0")
    Long sumEarnedByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t " +
           "WHERE t.user = :user AND t.amount > 0 " +
           "AND t.createdAt >= :start AND t.createdAt < :end")
    Long sumEarnedByUserAndPeriod(@Param("user") User user,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    List<CreditTransaction> findByUserOrderByCreatedAtDesc(User user);
}
