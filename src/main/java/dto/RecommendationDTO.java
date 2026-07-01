// dto/RecommendationDTO.java
package dto;

public class RecommendationDTO {
    private String skillName;
    private Integer matchPercent;
    private String description;
    
    public RecommendationDTO(String skillName, Integer matchPercent, String description) {
        this.skillName = skillName;
        this.matchPercent = matchPercent;
        this.description = description;
    }
    
    public RecommendationDTO() {
		super();
		// TODO Auto-generated constructor stub
	}


	// getters, setters
	public String getSkillName() {
		return skillName;
	}
	public void setSkillName(String skillName) {
		this.skillName = skillName;
	}
	public Integer getMatchPercent() {
		return matchPercent;
	}
	public void setMatchPercent(Integer matchPercent) {
		this.matchPercent = matchPercent;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

    
}
