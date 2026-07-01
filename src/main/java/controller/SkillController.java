package controller;

import model.Skill;
import repository.SkillRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillRepository skillRepository;

    public SkillController(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /**
     * Search for skills by name (case‑insensitive partial match).
     * @param query the search term (minimum 2 characters recommended)
     * @return list of matching skills, limited to 10 results
     */
    @GetMapping("/search")
    public ResponseEntity<List<Skill>> searchSkills(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            // Return empty list if query is too short
            return ResponseEntity.ok(List.of());
        }
        List<Skill> results = skillRepository.findByNameContainingIgnoreCase(query.trim());
        // Limit to 10 results to avoid large responses
        if (results.size() > 10) {
            results = results.subList(0, 10);
        }
        return ResponseEntity.ok(results);
    }

    /**
     * Get all skills (optional, may be used elsewhere).
     */
    @GetMapping
    public ResponseEntity<List<Skill>> getAllSkills() {
        return ResponseEntity.ok(skillRepository.findAll());
    }
}