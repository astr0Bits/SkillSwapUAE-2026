// dto/UserSkillDTO.java
package dto;

import model.UserSkill.SkillLevel;
import model.UserSkill.SkillType;

public class UserSkillDTO {
    private Long skillId;
    private String skillName;   // for creating new skill on the fly
    private String category;
    private SkillType type;     // MENTOR or LEARN
    private SkillLevel level;   // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public SkillType getType() { return type; }
    public void setType(SkillType type) { this.type = type; }
    public SkillLevel getLevel() { return level; }
    public void setLevel(SkillLevel level) { this.level = level; }
}