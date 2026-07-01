package payload;

public class BookingRequest {
    private Long mentorId;
    private String sessionType;
    private Integer duration;
    private String mode;        // "online" or "in-person"
    private String skillFocus;
    private String goals;
    private String experience;
    private String selectedSlot;

    // Getters and setters
    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getSkillFocus() { return skillFocus; }
    public void setSkillFocus(String skillFocus) { this.skillFocus = skillFocus; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(String selectedSlot) { this.selectedSlot = selectedSlot; }
}