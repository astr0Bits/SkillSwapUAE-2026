package model;

import enums.SessionMode;
import enums.SessionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne
    @JoinColumn(name = "learner_id", nullable = true)
    private User learner;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = true)
    private Skill skill;

    private LocalDateTime scheduledTime;
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    private SessionMode mode;

    private String location;
    private String meetingLink;

    @Column(name = "physical_location")
    private String physicalLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SessionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "tools_needed", length = 500)
    private String toolsNeeded;

    @Column(name = "max_learners")
    private int maxLearners = 1;

    @Column(name = "current_learners")
    private int currentLearners = 0;

    // ── Constructor ──────────────────────────────────────────────
    public Session() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getMentor() { return mentor; }
    public void setMentor(User mentor) { this.mentor = mentor; }

    public User getLearner() { return learner; }
    public void setLearner(User learner) { this.learner = learner; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getPhysicalLocation() { return physicalLocation; }
    public void setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getToolsNeeded() { return toolsNeeded; }
    public void setToolsNeeded(String toolsNeeded) { this.toolsNeeded = toolsNeeded; }

    public int getMaxLearners() { return maxLearners; }
    public void setMaxLearners(int maxLearners) { this.maxLearners = maxLearners; }

    public int getCurrentLearners() { return currentLearners; }
    public void setCurrentLearners(int currentLearners) { this.currentLearners = currentLearners; }
}