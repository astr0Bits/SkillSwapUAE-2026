package model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "sponsor_notification_settings")
public class SponsorNotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private boolean notifApplications = true;
    private boolean notifCoupons = true;
    private boolean notifReports = false;
    private boolean notifAnnouncements = true;


    public SponsorNotificationSettings(User user) {
        this.user = user;
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


	public boolean isNotifApplications() {
		return notifApplications;
	}


	public void setNotifApplications(boolean notifApplications) {
		this.notifApplications = notifApplications;
	}


	public boolean isNotifCoupons() {
		return notifCoupons;
	}


	public void setNotifCoupons(boolean notifCoupons) {
		this.notifCoupons = notifCoupons;
	}


	public boolean isNotifReports() {
		return notifReports;
	}


	public void setNotifReports(boolean notifReports) {
		this.notifReports = notifReports;
	}


	public boolean isNotifAnnouncements() {
		return notifAnnouncements;
	}


	public void setNotifAnnouncements(boolean notifAnnouncements) {
		this.notifAnnouncements = notifAnnouncements;
	}

    
}
