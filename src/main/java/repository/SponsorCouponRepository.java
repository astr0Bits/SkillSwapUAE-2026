package repository;

import model.SponsorCoupon;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorCouponRepository extends JpaRepository<SponsorCoupon, Long> {
    List<SponsorCoupon> findBySponsor(User sponsor);
    List<SponsorCoupon> findBySponsorAndActiveTrue(User sponsor);
}
