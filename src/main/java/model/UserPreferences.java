// model/UserPreferences.java
package model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(length = 500)
    private String learningGoals; // comma separated

    private Integer weeklyHours; // availability in hours

    private String preferredMode; // "Online", "In-person", "Hybrid"

    private Boolean notificationsEnabled = true;

	public UserPreferences(Long id, User user, String learningGoals, Integer weeklyHours, String preferredMode,
			Boolean notificationsEnabled) {
		super();
		this.id = id;
		this.user = user;
		this.learningGoals = learningGoals;
		this.weeklyHours = weeklyHours;
		this.preferredMode = preferredMode;
		this.notificationsEnabled = notificationsEnabled;
	}
	

	public UserPreferences() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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