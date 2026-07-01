package model;

import enums.BadgeCriteriaType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 10)
    private String iconName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private BadgeCriteriaType criteriaType;

    @Column(nullable = false)
    private int thresholdValue;

    @Column(nullable = false)
    private boolean published = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Badge() {}

    public Badge(String name, String description, String iconName,
                 BadgeCriteriaType criteriaType, int thresholdValue) {
        this.name = name;
        this.description = description;
        this.iconName = iconName;
        this.criteriaType = criteriaType;
        this.thresholdValue = thresholdValue;
        this.published = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public BadgeCriteriaType getCriteriaType() { return criteriaType; }
    public void setCriteriaType(BadgeCriteriaType criteriaType) { this.criteriaType = criteriaType; }
    public int getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(int thresholdValue) { this.thresholdValue = thresholdValue; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
