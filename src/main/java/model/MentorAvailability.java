package model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "mentor_availability")
public class MentorAvailability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User mentor;

    private String dayOfWeek;        // MONDAY, TUESDAY, etc.
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean recurring = true; // if false, treat as one-off date
    private LocalDate specificDate;   // used when recurring = false
	public MentorAvailability() {
		super();
		// TODO Auto-generated constructor stub
	}
	public MentorAvailability(Long id, User mentor, String dayOfWeek, LocalTime startTime, LocalTime endTime,
			boolean recurring, LocalDate specificDate) {
		super();
		this.id = id;
		this.mentor = mentor;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.recurring = recurring;
		this.specificDate = specificDate;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public User getMentor() {
		return mentor;
	}
	public void setMentor(User mentor) {
		this.mentor = mentor;
	}
	public String getDayOfWeek() {
		return dayOfWeek;
	}
	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	public LocalTime getStartTime() {
		return startTime;
	}
	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}
	public LocalTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}
	public boolean isRecurring() {
		return recurring;
	}
	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}
	public LocalDate getSpecificDate() {
		return specificDate;
	}
	public void setSpecificDate(LocalDate specificDate) {
		this.specificDate = specificDate;
	}

}