package config;

import enums.Role;
import model.Skill;
import model.User;
import model.UserPreferences;
import model.UserSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import repository.SkillRepository;
import repository.UserPreferencesRepository;
import repository.UserRepository;
import repository.UserSkillRepository;

@Component
@Order(3)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      SkillRepository skillRepository,
                      UserSkillRepository userSkillRepository,
                      UserPreferencesRepository preferencesRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.preferencesRepository = preferencesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() >= 30) {
            log.info("DataSeeder: skipping — {} users already exist.", userRepository.count());
            return;
        }

        // ── Original 5 mentors ──────────────────────────────────────────────
        seedMentor("sara@skillswap.dev", "Sara Al Mansoori", "Dubai, UAE",
                new String[]{"Calligraphy", "Painting"},
                UserSkill.SkillLevel.EXPERT,
                "Art education, calligraphy techniques, and creative expression for beginners and intermediates.");

        seedMentor("ahmed@skillswap.dev", "Ahmed Khalil", "Abu Dhabi, UAE",
                new String[]{"Reading Mandarin", "English Speaking", "Chinese"},
                UserSkill.SkillLevel.INTERMEDIATE,
                "Language learning strategies, Mandarin and Chinese for English speakers.");

        seedMentor("priya@skillswap.dev", "Priya Nair", "Sharjah, UAE",
                new String[]{"Graphic Design", "Photography", "UI/UX Design"},
                UserSkill.SkillLevel.EXPERT,
                "Design thinking, visual communication, photography, and building user-centered digital products.");

        seedMentor("james@skillswap.dev", "James Okafor", "Dubai, UAE",
                new String[]{"Public Speaking", "English Writing", "Finance & Investing"},
                UserSkill.SkillLevel.INTERMEDIATE,
                "Communication skills, persuasive writing, and personal finance management.");

        seedMentor("lina@skillswap.dev", "Lina Haddad", "Remote",
                new String[]{"Excel & Spreadsheets", "Digital Marketing", "Finance & Investing"},
                UserSkill.SkillLevel.EXPERT,
                "Data-driven marketing, Excel automation, and investment fundamentals.");

        // ── Original 2 learners ─────────────────────────────────────────────
        seedLearner("learner1@skillswap.dev", "Test Learner One", "Dubai, UAE",
                new String[]{"Python Programming", "Machine Learning"});

        seedLearner("learner2@skillswap.dev", "Test Learner Two", "Abu Dhabi, UAE",
                new String[]{"Cooking", "Dancing"});

        // ── 7 new mentors ───────────────────────────────────────────────────
        seedMentor("yusuf@skillswap.dev", "Yusuf Al Rashidi", "Dubai, UAE",
                new String[]{"Web Development", "JavaScript", "React"},
                UserSkill.SkillLevel.EXPERT,
                "Full-stack web development, modern JavaScript frameworks, and building production-grade React apps.");

        seedMentor("maria@skillswap.dev", "Maria Santos", "Remote",
                new String[]{"Python Programming", "Machine Learning", "Data Science"},
                UserSkill.SkillLevel.EXPERT,
                "Python for data science, machine learning pipelines, and hands-on model building for beginners.");

        seedMentor("kenji@skillswap.dev", "Kenji Tanaka", "Dubai, UAE",
                new String[]{"Japanese Language", "Martial Arts", "Yoga"},
                UserSkill.SkillLevel.INTERMEDIATE,
                "Japanese language basics, mindfulness through yoga, and self-defence fundamentals.");

        seedMentor("fatima@skillswap.dev", "Fatima Al Zaabi", "Abu Dhabi, UAE",
                new String[]{"Arabic Calligraphy", "Islamic Art", "Quran Recitation"},
                UserSkill.SkillLevel.EXPERT,
                "Traditional Arabic calligraphy, Islamic geometric art, and Tajweed rules for Quran recitation.");

        seedMentor("david@skillswap.dev", "David Kim", "Sharjah, UAE",
                new String[]{"Music Production", "Guitar", "Piano"},
                UserSkill.SkillLevel.INTERMEDIATE,
                "Music theory, guitar and piano for beginners, and home studio music production with DAWs.");

        seedMentor("aisha@skillswap.dev", "Aisha Benali", "Dubai, UAE",
                new String[]{"Cooking", "Baking", "Nutrition"},
                UserSkill.SkillLevel.EXPERT,
                "Healthy meal planning, baking techniques, and practical nutrition for everyday wellness.");

        seedMentor("carlos@skillswap.dev", "Carlos Mendez", "Remote",
                new String[]{"Video Editing", "Content Creation", "YouTube Strategy"},
                UserSkill.SkillLevel.INTERMEDIATE,
                "Video editing with Premiere Pro, content strategy for social media, and growing a YouTube channel.");

        // ── 3 new learners ──────────────────────────────────────────────────
        seedLearner("zara@skillswap.dev", "Zara Ahmed", "Dubai, UAE",
                new String[]{"Web Development", "JavaScript"});

        seedLearner("ravi@skillswap.dev", "Ravi Patel", "Abu Dhabi, UAE",
                new String[]{"Music Production", "Guitar"});

        seedLearner("nour@skillswap.dev", "Nour El Hassan", "Remote",
                new String[]{"Digital Marketing", "Content Creation"});

        log.info("=== DataSeeder: seeded all test accounts (up to 17) ===");
    }

    private void seedMentor(String email, String name, String location,
                            String[] skillNames, UserSkill.SkillLevel level, String goals) {
        if (userRepository.existsByEmail(email)) return;

        User mentor = new User(email, passwordEncoder.encode(PASSWORD), name, Role.MENTOR);
        mentor.setLocation(location);
        mentor.setEnabled(true);
        mentor.setReputation(80);
        mentor.setCredits(100);
        userRepository.save(mentor);

        for (String skillName : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(skillName).orElseGet(() -> {
                Skill s = new Skill();
                s.setName(skillName);
                s.setCategory("General");
                return skillRepository.save(s);
            });

            if (!userSkillRepository.existsByUserIdAndSkillIdAndType(
                    mentor.getId(), skill.getId(), UserSkill.SkillType.MENTOR)) {
                UserSkill us = new UserSkill();
                us.setUser(mentor);
                us.setSkill(skill);
                us.setType(UserSkill.SkillType.MENTOR);
                us.setLevel(level);
                userSkillRepository.save(us);
            }
        }

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(mentor);
        prefs.setLearningGoals(goals);
        prefs.setWeeklyHours(10);
        prefs.setPreferredMode("Online");
        prefs.setNotificationsEnabled(true);
        preferencesRepository.save(prefs);

        log.info("DataSeeder: seeded mentor {}", email);
    }

    private void seedLearner(String email, String name, String location, String[] skillNames) {
        if (userRepository.existsByEmail(email)) return;

        User learner = new User(email, passwordEncoder.encode(PASSWORD), name, Role.LEARNER);
        learner.setLocation(location);
        learner.setEnabled(true);
        learner.setReputation(0);
        learner.setCredits(50);
        userRepository.save(learner);

        for (String skillName : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(skillName).orElseGet(() -> {
                Skill s = new Skill();
                s.setName(skillName);
                s.setCategory("General");
                return skillRepository.save(s);
            });

            if (!userSkillRepository.existsByUserIdAndSkillIdAndType(
                    learner.getId(), skill.getId(), UserSkill.SkillType.LEARN)) {
                UserSkill us = new UserSkill();
                us.setUser(learner);
                us.setSkill(skill);
                us.setType(UserSkill.SkillType.LEARN);
                us.setLevel(UserSkill.SkillLevel.BEGINNER);
                userSkillRepository.save(us);
            }
        }

        log.info("DataSeeder: seeded learner {}", email);
    }
}
