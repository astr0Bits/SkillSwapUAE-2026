package service;

import model.Skill;
import repository.SkillRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public Skill getSkillByName(String name) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + name));
    }

    public Skill saveSkill(Skill skill) {
        return skillRepository.save(skill);
    }
}