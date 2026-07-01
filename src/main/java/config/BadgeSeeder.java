package config;

import enums.BadgeCriteriaType;
import model.Badge;
import repository.BadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class BadgeSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BadgeSeeder.class);
    private final BadgeRepository badgeRepository;

    public BadgeSeeder(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(String... args) {
        if (badgeRepository.count() > 0) return;

        log.info("Seeding 10 default badges...");

        // SESSION_COUNT badges — ordered by threshold ascending
        badgeRepository.save(new Badge("Novice",
                "Complete your first session", "🌱",
                BadgeCriteriaType.SESSION_COUNT, 1));
        badgeRepository.save(new Badge("Apprentice",
                "Complete 5 sessions", "⚙️",
                BadgeCriteriaType.SESSION_COUNT, 5));
        badgeRepository.save(new Badge("Adept",
                "Complete 10 sessions", "🔥",
                BadgeCriteriaType.SESSION_COUNT, 10));
        badgeRepository.save(new Badge("Community Builder",
                "Complete 15 sessions", "🤝",
                BadgeCriteriaType.SESSION_COUNT, 15));
        badgeRepository.save(new Badge("Expert Swapper",
                "Complete 20 sessions", "🔄",
                BadgeCriteriaType.SESSION_COUNT, 20));
        badgeRepository.save(new Badge("Session Master",
                "Complete 50 sessions", "🏆",
                BadgeCriteriaType.SESSION_COUNT, 50));

        // RATING_THRESHOLD badges — reputation = avgRating * 20 (so 80 = avg 4.0, 90 = avg 4.5)
        badgeRepository.save(new Badge("Rising Star",
                "Reach a reputation score of 20", "⭐",
                BadgeCriteriaType.RATING_THRESHOLD, 20));
        badgeRepository.save(new Badge("Trusted Mentor",
                "Reach a reputation score of 50", "🌟",
                BadgeCriteriaType.RATING_THRESHOLD, 50));
        badgeRepository.save(new Badge("Five-Star Mentor",
                "Reach a reputation score of 80 (avg ≥ 4.0 stars)", "💫",
                BadgeCriteriaType.RATING_THRESHOLD, 80));
        badgeRepository.save(new Badge("Elite Mentor",
                "Reach a reputation score of 90 (avg ≥ 4.5 stars)", "🥇",
                BadgeCriteriaType.RATING_THRESHOLD, 90));

        log.info("Badge seeding complete — 10 badges created.");
    }
}
