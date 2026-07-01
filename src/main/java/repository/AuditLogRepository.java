package repository;

import model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	@Modifying
	@Query(value = "DELETE FROM audit_log WHERE user_id = :userId", nativeQuery = true)
	void deleteByUserId(@Param("userId") Long userId);	

	
}
