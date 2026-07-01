package service;

import dto.MentorBrowseDTO;
import model.User;
import model.UserSkill;
import repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BrowseService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final BadgeService badgeService;

    public BrowseService(UserRepository userRepository,
                         UserSkillRepository userSkillRepository,
                         ReviewRepository reviewRepository,
                         SessionRepository sessionRepository,
                         BadgeService badgeService) {
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.badgeService = badgeService;
    }

    public List<MentorBrowseDTO> findMentors(Map<String, String> filters) {

        List<User> mentors = userRepository.findAll().stream()
                .filter(user -> !userSkillRepository
                        .findByUserIdAndType(user.getId(), UserSkill.SkillType.MENTOR)
                        .isEmpty())
                .collect(Collectors.toList());

        List<MentorBrowseDTO> result = new ArrayList<>();

        for (User user : mentors) {

            List<UserSkill> teachingSkills =
                    userSkillRepository.findByUserIdAndType(user.getId(), UserSkill.SkillType.MENTOR);

            // FIX: properly map skill name + level into SkillSummary
            List<MentorBrowseDTO.SkillSummary> skillSummaries = teachingSkills.stream()
                    .map(us -> {
                        String skillName = us.getSkill() != null ? us.getSkill().getName() : "Unknown";
                        String level = us.getLevel() != null ? us.getLevel().name() : "BEGINNER";
                        return new MentorBrowseDTO.SkillSummary(skillName, level);
                    })
                    .collect(Collectors.toList());

            Double avgRating = reviewRepository.findAverageRatingByReviewee(user);
            if (avgRating == null) avgRating = 0.0;

            long completedSessions = sessionRepository.countCompletedMentorSessions(user);

            long reviewCount = reviewRepository.countByReviewee(user);

         
            List<String> badges = badgeService.getUserBadgeNames(
            	    user,
            	    user.getReputation() != null ? user.getReputation() : 0,
            	    completedSessions,
            	    avgRating
            	);
            // FIX: use setters to populate all fields
            MentorBrowseDTO dto = new MentorBrowseDTO();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setLocation(user.getLocation());
            dto.setRating(avgRating);
            dto.setReputation(user.getReputation());
            dto.setSessionsCount((int) completedSessions);
            dto.setReviewsCount((int) reviewCount);
            dto.setSkills(skillSummaries);
            dto.setBadges(badges != null ? badges : List.of());
            dto.setModes(List.of("online", "in-person"));
            dto.setAvailability(List.of("week"));

            result.add(dto);
        }

        return result;
    }
}