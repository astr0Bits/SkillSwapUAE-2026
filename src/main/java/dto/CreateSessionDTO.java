package dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class CreateSessionDTO {
    private Long skillId;
    private String mode;          // ONLINE or IN_PERSON
    private String meetingLink;   // for ONLINE
    private String location;         // for IN_PERSON (legacy)
    private String physicalLocation; // for IN_PERSON (preferred)
    private String description;   // what the session covers
    private String toolsNeeded;   // what attendees should bring/prepare
    private int durationMinutes;
    private int maxLearners;      // how many can join

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledTime;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhysicalLocation() {
        return physicalLocation != null ? physicalLocation : location;
    }
    public void setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getToolsNeeded() { return toolsNeeded; }
    public void setToolsNeeded(String toolsNeeded) { this.toolsNeeded = toolsNeeded; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getMaxLearners() { return maxLearners; }
    public void setMaxLearners(int maxLearners) { this.maxLearners = maxLearners; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}