// service/UserSkillService.java
package service;

import dto.UserSkillDTO;
import model.*;
import model.UserSkill.SkillType;
import repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserSkillService {

    private final UserSkillRepository userSkillRepo;
    private final SkillRepository skillRepo;
    private final UserRepository userRepo;

    public UserSkillService(UserSkillRepository userSkillRepo,
                            SkillRepository skillRepo,
                            UserRepository userRepo) {
        this.userSkillRepo = userSkillRepo;
        this.skillRepo = skillRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public UserSkill addSkillToUser(Long userId, UserSkillDTO dto) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find existing skill OR auto-create it
        Skill skill;
        if (dto.getSkillId() != null) {
            skill = skillRepo.findById(dto.getSkillId())
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        } else if (dto.getSkillName() != null && !dto.getSkillName().isBlank()) {
            skill = skillRepo.findByNameIgnoreCase(dto.getSkillName().trim())
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setName(dto.getSkillName().trim());
                    s.setCategory(dto.getCategory() != null ? dto.getCategory() : "General");
                    return skillRepo.save(s);
                });
        } else {
            throw new RuntimeException("Provide skillId or skillName");
        }

        // Prevent duplicates
        if (userSkillRepo.existsByUserIdAndSkillIdAndType(userId, skill.getId(), dto.getType())) {
            throw new RuntimeException("You already have this skill listed");
        }

        UserSkill us = new UserSkill(user, skill, null, null);
        us.setUser(user);
        us.setSkill(skill);
        us.setType(dto.getType());
        us.setLevel(dto.getLevel() != null ? dto.getLevel() : UserSkill.SkillLevel.BEGINNER);
        return userSkillRepo.save(us);
    }

    // Returns { teach: [...], learn: [...] } — keeping "teach" key for dashboard compatibility
    public Map<String, List<Map<String, Object>>> getUserSkills(Long userId) {
        List<UserSkill> all = userSkillRepo.findByUserId(userId);
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("teach", mapSkills(all, SkillType.MENTOR)); // frontend expects "teach"
        result.put("learn", mapSkills(all, SkillType.LEARN));
        return result;
    }

    private List<Map<String, Object>> mapSkills(List<UserSkill> skills, SkillType type) {
        return skills.stream()
            .filter(us -> us.getType() == type)
            .map(us -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", us.getId());
                m.put("skillId", us.getSkill().getId());
                m.put("skillName", us.getSkill().getName());
                m.put("category", us.getSkill().getCategory());
                m.put("level", us.getLevel());
                return m;
            }).collect(Collectors.toList());
    }

    @Transactional
    public void removeUserSkill(Long userId, Long userSkillId) {
        UserSkill us = userSkillRepo.findById(userSkillId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        if (!us.getUser().getId().equals(userId))
            throw new RuntimeException("Unauthorized");
        userSkillRepo.delete(us);
    }

    // For browse-skills search page
 // In service/UserSkillService.java
 // Replace or add this method:

 // In service/UserSkillService.java — replace the broken method:

    public List<Map<String, Object>> searchMentorsBySkill(String query) {
        List<UserSkill> userSkills = userSkillRepo.searchMentorsBySkill(query);

        return userSkills.stream().map(us -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId",    us.getUser().getId());
            map.put("skillId",   us.getSkill().getId());
            map.put("userName",  us.getUser().getName());
            map.put("skillName", us.getSkill().getName());
            map.put("category",  us.getSkill().getCategory());
            map.put("level",     us.getLevel() != null ? us.getLevel().toString() : "BEGINNER");
            map.put("mode",      "ONLINE");
            return map;
        }).collect(Collectors.toList());
    }	

    public List<Skill> getAllSkills() {
        return skillRepo.findAll();
    }
}