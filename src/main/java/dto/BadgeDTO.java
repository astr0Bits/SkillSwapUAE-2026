package dto;

public class BadgeDTO {

    // Core definition fields
    private Long id;
    private String name;
    private String icon;
    private String description;
    private String requirement;     // backward-compat alias
    private String criteriaType;    // "SESSION_COUNT" or "RATING_THRESHOLD"
    private int thresholdValue;

    // Progress fields (populated by /me/progress endpoint)
    private boolean earned;
    private String earnedDate;      // null — not stored in DB
    private String progress;        // e.g. "3/10"
    private int progressPercent;    // 0-100

    // Admin fields
    private boolean published;
    private long earnedCount;

    public BadgeDTO() {}

    /** Legacy constructor used by profile-page and browse-page callers. */
    public BadgeDTO(String name, String icon, boolean earned, String requirement) {
        this.name = name;
        this.icon = icon;
        this.earned = earned;
        this.requirement = requirement;
        this.description = requirement;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
    public String getCriteriaType() { return criteriaType; }
    public void setCriteriaType(String criteriaType) { this.criteriaType = criteriaType; }
    public int getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(int thresholdValue) { this.thresholdValue = thresholdValue; }
    public boolean isEarned() { return earned; }
    public void setEarned(boolean earned) { this.earned = earned; }
    public String getEarnedDate() { return earnedDate; }
    public void setEarnedDate(String earnedDate) { this.earnedDate = earnedDate; }
    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public long getEarnedCount() { return earnedCount; }
    public void setEarnedCount(long earnedCount) { this.earnedCount = earnedCount; }
}