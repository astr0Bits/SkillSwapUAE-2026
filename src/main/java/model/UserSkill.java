// model/UserSkill.java
package model;

import enums.UserSkillType;
import jakarta.persistence.*;

@Entity
@Table(name = "user_skills")
public class UserSkill {
	
	public UserSkill() {
	}

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    // Match your DB enum exactly: LEARN, MENTOR
    public enum SkillType { LEARN, MENTOR }

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SkillType type;

    // Match your DB enum exactly: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    public enum SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private SkillLevel level;

    public UserSkill(User user2, Skill skill2, UserSkillType mentor, enums.SkillLevel level2) {
		// TODO Auto-generated constructor stub
	}
	// Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public SkillType getType() { return type; }
    public void setType(SkillType type) { this.type = type; }
    public SkillLevel getLevel() { return level; }
    public void setLevel(SkillLevel level) { this.level = level; }
}