// dto/UserPreferencesDTO.java
package dto;

import validator.NoHtml;

public class UserPreferencesDTO {
    @NoHtml
    private String learningGoals;
    private Integer weeklyHours;
    private String preferredMode;
    private Boolean notificationsEnabled;
	
    public UserPreferencesDTO(String learningGoals, Integer weeklyHours, String preferredMode,
			Boolean notificationsEnabled) {
		super();
		this.learningGoals = learningGoals;
		this.weeklyHours = weeklyHours;
		this.preferredMode = preferredMode;
		this.notificationsEnabled = notificationsEnabled;
	}
	public UserPreferencesDTO() {
	}
	
	public String getLearningGoals() {
		return learningGoals;
	}
	public void setLearningGoals(String learningGoals) {
		this.learningGoals = learningGoals;
	}
	public Integer getWeeklyHours() {
		return weeklyHours;
	}
	public void setWeeklyHours(Integer weeklyHours) {
		this.weeklyHours = weeklyHours;
	}
	public String getPreferredMode() {
		return preferredMode;
	}
	public void setPreferredMode(String preferredMode) {
		this.preferredMode = preferredMode;
	}
	public Boolean getNotificationsEnabled() {
		return notificationsEnabled;
	}
	public void setNotificationsEnabled(Boolean notificationsEnabled) {
		this.notificationsEnabled = notificationsEnabled;
	}


	
}