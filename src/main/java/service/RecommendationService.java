package service;

import dto.RecommendationDTO;
import model.User;
import model.UserSkill;
import repository.SkillRepository;
import repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    private final GroqMatchingService geminiMatchingService;

    public RecommendationService(UserSkillRepository userSkillRepository,
                                 SkillRepository skillRepository,
                                 GroqMatchingService geminiMatchingService) {
        this.userSkillRepository = userSkillRepository;
        this.skillRepository = skillRepository;
        this.geminiMatchingService = geminiMatchingService;
    }

    public List<RecommendationDTO> getRecommendations(User user, String learningGoals) {
        List<UserSkill> learnSkills = userSkillRepository
                .findByUserIdAndType(user.getId(), UserSkill.SkillType.LEARN);

        if (learnSkills.isEmpty()) {
            return defaultRecommendations(learningGoals);
        }

        String learnerLevel = learnSkills.get(0).getLevel() != null
                ? learnSkills.get(0).getLevel().name()
                : "BEGINNER";

        Map<Long, User> mentorMap = new LinkedHashMap<>();
        for (UserSkill ls : learnSkills) {
            for (UserSkill ms : userSkillRepository.findMentorsBySkillId(ls.getSkill().getId())) {
                mentorMap.putIfAbsent(ms.getUser().getId(), ms.getUser());
            }
        }

        if (mentorMap.isEmpty()) {
            return defaultRecommendations(learningGoals);
        }

        List<GroqMatchingService.MentorSummary> summaries = new ArrayList<>();
        for (User mentor : mentorMap.values()) {
            List<UserSkill> mentorSkills = userSkillRepository
                    .findByUserIdAndType(mentor.getId(), UserSkill.SkillType.MENTOR);
            String skillNames = mentorSkills.stream()
                    .map(ms -> ms.getSkill().getName())
                    .collect(Collectors.joining(", "));
            double rating = mentor.getReputation() != null ? mentor.getReputation() / 20.0 : 0.0;
            String location = mentor.getLocation() != null ? mentor.getLocation() : "";
            summaries.add(new GroqMatchingService.MentorSummary(
                    mentor.getId(), mentor.getName(), location, skillNames, rating, 0, ""));
        }

        String goals = learningGoals != null ? learningGoals : "";
        List<Long> rankedIds = geminiMatchingService.rankMentors(goals, learnerLevel, "", "", summaries);

        Map<Long, GroqMatchingService.MentorSummary> byId = summaries.stream()
                .collect(Collectors.toMap(s -> s.userId, s -> s));

        List<RecommendationDTO> result = new ArrayList<>();
        int rank = 0;
        for (Long uid : rankedIds) {
            GroqMatchingService.MentorSummary s = byId.get(uid);
            if (s == null) continue;
            int matchPct = Math.max(50, 100 - rank * 10);
            result.add(new RecommendationDTO(s.name, matchPct, s.skills));
            rank++;
            if (result.size() >= 5) break;
        }

        return result.isEmpty() ? defaultRecommendations(learningGoals) : result;
    }

    private List<RecommendationDTO> defaultRecommendations(String learningGoals) {
        List<RecommendationDTO> recs = new ArrayList<>();
        if (learningGoals != null && learningGoals.toLowerCase().contains("data")) {
            recs.add(new RecommendationDTO("Data Visualization", 92, "Tableau & Power BI"));
            recs.add(new RecommendationDTO("SQL & Database Querying", 88, "Advanced queries"));
        } else if (learningGoals != null && learningGoals.toLowerCase().contains("javascript")) {
            recs.add(new RecommendationDTO("JavaScript Development", 94, "Modern JS frameworks"));
            recs.add(new RecommendationDTO("React / Vue / Angular", 89, "Frontend mastery"));
        } else {
            recs.add(new RecommendationDTO("Python Programming", 85, "Automation & scripting"));
            recs.add(new RecommendationDTO("Public Speaking", 78, "Presentation skills"));
        }
        return recs;
    }
}
