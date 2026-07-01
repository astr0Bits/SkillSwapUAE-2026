package dto;

import enums.SessionMode;
import enums.SessionStatus;

import java.time.LocalDateTime;

public class SessionBrowseDTO {

    private Long id;
    private Long mentorId;
    private String mentorName;
    private String mentorEmail;

    private Long skillId;        // ← ADDED
    private String skillName;
    private String skillCategory;

    private SessionMode mode;
    private String meetingLink;
    private String location;
    private String physicalLocation;
    private String description;
    private String toolsNeeded;

    private int durationMinutes;
    private int maxLearners;
    private int currentLearners;

    private LocalDateTime scheduledTime;
    private String status;      // ← CHANGED from SessionStatus enum to String

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getMentorEmail() { return mentorEmail; }
    public void setMentorEmail(String mentorEmail) { this.mentorEmail = mentorEmail; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getSkillCategory() { return skillCategory; }
    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }

    public SessionMode getMode() { return mode; }
    public void setMode(SessionMode mode) { this.mode = mode; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhysicalLocation() { return physicalLocation; }
    public void setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getToolsNeeded() { return toolsNeeded; }
    public void setToolsNeeded(String toolsNeeded) { this.toolsNeeded = toolsNeeded; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getMaxLearners() { return maxLearners; }
    public void setMaxLearners(int maxLearners) { this.maxLearners = maxLearners; }

    public int getCurrentLearners() { return currentLearners; }
    public void setCurrentLearners(int currentLearners) { this.currentLearners = currentLearners; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }}