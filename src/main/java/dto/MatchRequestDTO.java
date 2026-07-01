package dto;

import java.util.List;

public class MatchRequestDTO {
    private String learningGoals;
    private String currentLevel;
    private String preferredSchedule;
    private List<String> desiredSkills;

    public String getLearningGoals() { return learningGoals; }
    public void setLearningGoals(String learningGoals) { this.learningGoals = learningGoals; }

    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }

    public String getPreferredSchedule() { return preferredSchedule; }
    public void setPreferredSchedule(String preferredSchedule) { this.preferredSchedule = preferredSchedule; }

    public List<String> getDesiredSkills() { return desiredSkills; }
    public void setDesiredSkills(List<String> desiredSkills) { this.desiredSkills = desiredSkills; }
}
