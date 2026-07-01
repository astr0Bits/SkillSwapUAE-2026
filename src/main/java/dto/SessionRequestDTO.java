// dto/SessionRequestDTO.java
package dto;

import enums.SessionMode;
import java.time.LocalDateTime;

public class SessionRequestDTO {
    private Long mentorId;
    private Long skillId;
    private LocalDateTime scheduledTime;
    private Integer durationMinutes;
    private SessionMode mode;
    private String location;
    // getters, setters
	public Long getMentorId() {
		return mentorId;
	}
	public void setMentorId(Long mentorId) {
		this.mentorId = mentorId;
	}
	public Long getSkillId() {
		return skillId;
	}
	public void setSkillId(Long skillId) {
		this.skillId = skillId;
	}
	public LocalDateTime getScheduledTime() {
		return scheduledTime;
	}
	public void setScheduledTime(LocalDateTime scheduledTime) {
		this.scheduledTime = scheduledTime;
	}
	public Integer getDurationMinutes() {
		return durationMinutes;
	}
	public void setDurationMinutes(Integer durationMinutes) {
		this.durationMinutes = durationMinutes;
	}
	public SessionMode getMode() {
		return mode;
	}
	public void setMode(SessionMode mode) {
		this.mode = mode;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
    
}