package dto;

import java.util.List;

public class MentorBrowseDTO {

    private Long id;
    private String name;
    private String email;
    private String location;
    private Double rating;
    private Integer reputation;
    private Integer sessionsCount;
    private Integer reviewsCount;
    private List<String> badges;
    private List<SkillSummary> skills;
    private List<String> availability;
    private List<String> modes;

    public static class SkillSummary {
        private String name;
        private String level;   // String instead of enum — avoids mismatch

        public SkillSummary() {}

        public SkillSummary(String name, String level) {
            this.name = name;
            this.level = level;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getReputation() { return reputation; }
    public void setReputation(Integer reputation) { this.reputation = reputation; }
    public Integer getSessionsCount() { return sessionsCount; }
    public void setSessionsCount(Integer sessionsCount) { this.sessionsCount = sessionsCount; }
    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    public List<SkillSummary> getSkills() { return skills; }
    public void setSkills(List<SkillSummary> skills) { this.skills = skills; }
    public List<String> getAvailability() { return availability; }
    public void setAvailability(List<String> availability) { this.availability = availability; }
    public List<String> getModes() { return modes; }
    public void setModes(List<String> modes) { this.modes = modes; }
}