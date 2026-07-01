package model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sponsor_programs")
public class SponsorProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sponsor_id", nullable = false)
    private User sponsor;

    private String title;

    @Column(length = 500)
    private String description;

    private String status; // ACTIVE, PENDING, INACTIVE

    @Column(precision = 10, scale = 2)
    private BigDecimal fundingAmount;

    private String paymentStatus; // PENDING, PAID, FAILED

    private String stripeSessionId;

    public SponsorProgram() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSponsor() { return sponsor; }
    public void setSponsor(User sponsor) { this.sponsor = sponsor; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getFundingAmount() { return fundingAmount; }
    public void setFundingAmount(BigDecimal fundingAmount) { this.fundingAmount = fundingAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
}
