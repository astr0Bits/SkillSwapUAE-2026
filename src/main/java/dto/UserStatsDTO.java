// dto/UserStatsDTO.java
package dto;

public class UserStatsDTO {
    private Integer credits;
    private Integer reputation;   // 0-100 scale
    private Integer points; 
    private Long totalSessions;
    private Long upcomingSessions;

    // constructors
    public UserStatsDTO() {}
    public UserStatsDTO(Integer credits, Integer reputation, Integer points, Long totalSessions, Long upcomingSessions) {
        this.credits = credits;
        this.reputation = reputation;
        this.points = points;
        this.totalSessions = totalSessions;
        this.upcomingSessions = upcomingSessions;
    }

    // getters & setters
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public Integer getReputation() { return reputation; }
    public void setReputation(Integer reputation) { this.reputation = reputation; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long totalSessions) { this.totalSessions = totalSessions; }
    public Long getUpcomingSessions() { return upcomingSessions; }
    public void setUpcomingSessions(Long upcomingSessions) { this.upcomingSessions = upcomingSessions; }
}