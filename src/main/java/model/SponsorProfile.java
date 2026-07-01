package model;

import jakarta.persistence.*;

@Entity
@Table(name = "sponsor_profiles")
public class SponsorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String name;
    private String industry;

    @Column(length = 1000)
    private String bio;

    private String website;
    private String location;
    private String contactName;
    private String contactTitle;
    private String contactEmail;
    private String contactPhone;
    private String linkedin;
    private Integer foundedYear;
    private String companySize;

    public SponsorProfile() {}

    public SponsorProfile(User user) {
        this.user = user;
        this.name = user.getName();
        this.location = user.getLocation();
        this.contactEmail = user.getEmail();
        this.industry = "Technology";
        this.companySize = "1-10";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactTitle() { return contactTitle; }
    public void setContactTitle(String contactTitle) { this.contactTitle = contactTitle; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
}
