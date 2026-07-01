package model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "sponsor_coupons")
public class SponsorCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sponsor_id", nullable = false)
    private User sponsor;

    @Column(unique = true)
    private String code;

    private Integer discount;
    private String expiryDate;
    private Integer usedCount = 0;
    private Integer maxUses;
    private boolean active = true;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public User getSponsor() {
		return sponsor;
	}
	public void setSponsor(User sponsor) {
		this.sponsor = sponsor;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Integer getDiscount() {
		return discount;
	}
	public void setDiscount(Integer discount) {
		this.discount = discount;
	}
	public String getExpiryDate() {
		return expiryDate;
	}
	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}
	public Integer getUsedCount() {
		return usedCount;
	}
	public void setUsedCount(Integer usedCount) {
		this.usedCount = usedCount;
	}
	public Integer getMaxUses() {
		return maxUses;
	}
	public void setMaxUses(Integer maxUses) {
		this.maxUses = maxUses;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}

    

}
