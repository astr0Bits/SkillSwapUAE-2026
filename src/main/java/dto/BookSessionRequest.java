package dto;

import java.time.LocalDateTime;

public class BookSessionRequest {
    private Long mentorId;
    private Long sessionId; // optional: ID of an existing OPEN session to book
    private String skillName;
    private Long skillId;
    private String mode; // "online" or "in-person"
    private LocalDateTime scheduledTime; // ISO-8601, e.g. "2026-06-20T10:00:00"

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
}