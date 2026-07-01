package config;

import model.Skill;
import repository.SkillRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1) 
public class DataLoader implements CommandLineRunner {

    private final SkillRepository skillRepository;

    public DataLoader(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Detect missing or corrupted seed data (null-named rows from old bug)
        boolean needsSeed = skillRepository.findByNameIgnoreCase("Python Programming").isEmpty();
        if (needsSeed) {
            // Remove any corrupted rows that have no name
            skillRepository.findAll().stream()
                .filter(s -> s.getName() == null || s.getName().isBlank())
                .forEach(skillRepository::delete);
        }
        if (!needsSeed) return;

            List<String[]> skills = Arrays.asList(
                // Technical / Programming
                new String[]{"Python Programming", "Technical / Programming"},
                new String[]{"JavaScript / TypeScript", "Technical / Programming"},
                new String[]{"SQL & Database Querying", "Technical / Programming"},
                new String[]{"Java Programming", "Technical / Programming"},
                new String[]{"C / C++ Programming", "Technical / Programming"},
                new String[]{"Rust Programming", "Technical / Programming"},
                new String[]{"Go (Golang)", "Technical / Programming"},
                new String[]{"HTML & CSS", "Technical / Programming"},
                new String[]{"React / Vue / Angular", "Technical / Programming"},
                new String[]{"Node.js", "Technical / Programming"},
                new String[]{"REST API Design", "Technical / Programming"},
                new String[]{"GraphQL", "Technical / Programming"},
                new String[]{"Docker & Containerization", "Technical / Programming"},
                new String[]{"Kubernetes", "Technical / Programming"},
                new String[]{"CI/CD Pipelines", "Technical / Programming"},
                new String[]{"Git & Version Control", "Technical / Programming"},
                new String[]{"Cloud Platforms (AWS/Azure/GCP)", "Technical / Programming"},
                new String[]{"Linux & Shell Scripting", "Technical / Programming"},
                new String[]{"Cybersecurity Fundamentals", "Technical / Programming"},
                new String[]{"Network Administration", "Technical / Programming"},
                new String[]{"Machine Learning", "Technical / Programming"},
                new String[]{"Deep Learning / Neural Networks", "Technical / Programming"},
                new String[]{"Prompt Engineering (AI)", "Technical / Programming"},
                new String[]{"Cybersecurity Awareness", "Technical / Programming"},
                new String[]{"Blockchain & Web3", "Technical / Programming"},
                new String[]{"IoT & Embedded Systems", "Technical / Programming"},
                new String[]{"AR / VR Development", "Technical / Programming"},

                // Data & Analytics
                new String[]{"Data Analysis", "Data & Analytics"},
                new String[]{"Data Visualization", "Data & Analytics"},
                new String[]{"Statistical Analysis", "Data & Analytics"},
                new String[]{"Microsoft Excel / Google Sheets", "Data & Analytics"},
                new String[]{"Power BI", "Data & Analytics"},
                new String[]{"Tableau", "Data & Analytics"},
                new String[]{"ETL & Data Pipelines", "Data & Analytics"},
                new String[]{"Big Data (Spark / Hadoop)", "Data & Analytics"},
                new String[]{"A/B Testing", "Data & Analytics"},
                new String[]{"Predictive Modeling", "Data & Analytics"},
                new String[]{"Natural Language Processing (NLP)", "Data & Analytics"},
                new String[]{"R Programming", "Data & Analytics"},
                new String[]{"Research & Literature Review", "Data & Analytics"},
                new String[]{"Data Storytelling", "Data & Analytics"},

                // Business & Management
                new String[]{"Project Management", "Business & Management"},
                new String[]{"Agile / Scrum", "Business & Management"},
                new String[]{"Product Management", "Business & Management"},
                new String[]{"Strategic Planning", "Business & Management"},
                new String[]{"Business Analysis", "Business & Management"},
                new String[]{"Financial Modeling", "Business & Management"},
                new String[]{"Budgeting & Forecasting", "Business & Management"},
                new String[]{"Risk Management", "Business & Management"},
                new String[]{"Change Management", "Business & Management"},
                new String[]{"Operations Management", "Business & Management"},
                new String[]{"Supply Chain Management", "Business & Management"},
                new String[]{"Sales Strategy", "Business & Management"},
                new String[]{"Marketing Strategy", "Business & Management"},
                new String[]{"SEO / SEM", "Business & Management"},
                new String[]{"Customer Relationship Management (CRM)", "Business & Management"},
                new String[]{"Contract Negotiation", "Business & Management"},
                new String[]{"Competitive Analysis", "Business & Management"},
                new String[]{"KPI & OKR Setting", "Business & Management"},
                new String[]{"Stakeholder Management", "Business & Management"},
                new String[]{"Process Improvement (Lean/Six Sigma)", "Business & Management"},
                new String[]{"Agile Coaching", "Business & Management"},
                new String[]{"E-Learning & Training Development", "Business & Management"},
                new String[]{"Workforce Planning & HR Analytics", "Business & Management"},

                // Soft Skills / Interpersonal
                new String[]{"Communication", "Soft Skills / Interpersonal"},
                new String[]{"Active Listening", "Soft Skills / Interpersonal"},
                new String[]{"Public Speaking", "Soft Skills / Interpersonal"},
                new String[]{"Written Communication", "Soft Skills / Interpersonal"},
                new String[]{"Conflict Resolution", "Soft Skills / Interpersonal"},
                new String[]{"Teamwork & Collaboration", "Soft Skills / Interpersonal"},
                new String[]{"Leadership", "Soft Skills / Interpersonal"},
                new String[]{"Emotional Intelligence (EQ)", "Soft Skills / Interpersonal"},
                new String[]{"Empathy", "Soft Skills / Interpersonal"},
                new String[]{"Adaptability", "Soft Skills / Interpersonal"},
                new String[]{"Critical Thinking", "Soft Skills / Interpersonal"},
                new String[]{"Problem Solving", "Soft Skills / Interpersonal"},
                new String[]{"Decision Making", "Soft Skills / Interpersonal"},
                new String[]{"Time Management", "Soft Skills / Interpersonal"},
                new String[]{"Mentoring & Coaching", "Soft Skills / Interpersonal"},
                new String[]{"Negotiation", "Soft Skills / Interpersonal"},
                new String[]{"Presentation Skills", "Soft Skills / Interpersonal"},
                new String[]{"Networking", "Soft Skills / Interpersonal"},
                new String[]{"Creativity", "Soft Skills / Interpersonal"},
                new String[]{"Growth Mindset", "Soft Skills / Interpersonal"},
                new String[]{"Foreign Language Proficiency", "Soft Skills / Interpersonal"},
                new String[]{"Customer Service", "Soft Skills / Interpersonal"},

                // Creative & Design
                new String[]{"Graphic Design", "Creative & Design"},
                new String[]{"UI/UX Design", "Creative & Design"},
                new String[]{"Figma / Sketch / Adobe XD", "Creative & Design"},
                new String[]{"Adobe Photoshop", "Creative & Design"},
                new String[]{"Adobe Illustrator", "Creative & Design"},
                new String[]{"Adobe Premiere / Video Editing", "Creative & Design"},
                new String[]{"Motion Graphics & Animation", "Creative & Design"},
                new String[]{"Photography", "Creative & Design"},
                new String[]{"Copywriting", "Creative & Design"},
                new String[]{"Content Writing & Blogging", "Creative & Design"},
                new String[]{"Brand Identity Design", "Creative & Design"},
                new String[]{"3D Modeling (Blender / Maya)", "Creative & Design"},
                new String[]{"User Research & Usability Testing", "Creative & Design"},
                new String[]{"Storyboarding", "Creative & Design"},
                new String[]{"Social Media Content Creation", "Creative & Design"},
                new String[]{"Podcast Production", "Creative & Design"},
                new String[]{"Technical Writing", "Creative & Design"},
                new String[]{"Accessibility & Inclusive Design", "Creative & Design"},

                // Cooking & Culinary
                new String[]{"Knife Skills & Food Prep", "Cooking & Culinary"},
                new String[]{"Baking & Pastry", "Cooking & Culinary"},
                new String[]{"Bread Making & Fermentation", "Cooking & Culinary"},
                new String[]{"Sauce & Stock Making", "Cooking & Culinary"},
                new String[]{"Meal Planning & Nutrition", "Cooking & Culinary"},
                new String[]{"World Cuisine (Asian, Italian...)", "Cooking & Culinary"},
                new String[]{"Grilling & BBQ", "Cooking & Culinary"},
                new String[]{"Food Preservation & Pickling", "Cooking & Culinary"},
                new String[]{"Cake Decorating", "Cooking & Culinary"},
                new String[]{"Barista & Coffee Brewing", "Cooking & Culinary"},
                new String[]{"Cocktail Mixing & Bartending", "Cooking & Culinary"},
                new String[]{"Wine & Spirits Knowledge", "Cooking & Culinary"},

                // Languages
                new String[]{"English (Fluent/Native)", "Languages"},
                new String[]{"Spanish", "Languages"},
                new String[]{"French", "Languages"},
                new String[]{"German", "Languages"},
                new String[]{"Mandarin Chinese", "Languages"},
                new String[]{"Japanese", "Languages"},
                new String[]{"Arabic", "Languages"},
                new String[]{"Portuguese", "Languages"},
                new String[]{"Russian", "Languages"},
                new String[]{"Hindi", "Languages"},
                new String[]{"Italian", "Languages"},
                new String[]{"Korean", "Languages"},
                new String[]{"Sign Language (ASL/BSL)", "Languages"},
                new String[]{"Translation & Interpretation", "Languages"},
                new String[]{"Linguistics & Grammar", "Languages"},

                // Crafts & Making
                new String[]{"Knitting & Crocheting", "Crafts & Making"},
                new String[]{"Sewing & Tailoring", "Crafts & Making"},
                new String[]{"Embroidery & Cross-Stitch", "Crafts & Making"},
                new String[]{"Woodworking & Carpentry", "Crafts & Making"},
                new String[]{"Furniture Making", "Crafts & Making"},
                new String[]{"Pottery & Ceramics", "Crafts & Making"},
                new String[]{"Sculpting", "Crafts & Making"},
                new String[]{"Jewelry Making", "Crafts & Making"},
                new String[]{"Leatherworking", "Crafts & Making"},
                new String[]{"Candle & Soap Making", "Crafts & Making"},
                new String[]{"Paper Craft & Origami", "Crafts & Making"},
                new String[]{"Screen Printing & Printmaking", "Crafts & Making"},
                new String[]{"Welding & Metalwork", "Crafts & Making"},
                new String[]{"3D Printing & Fabrication", "Crafts & Making"},
                new String[]{"Electronics & Soldering", "Crafts & Making"},

                // Music & Performing Arts
                new String[]{"Singing & Vocal Technique", "Music & Performing Arts"},
                new String[]{"Piano / Keyboard", "Music & Performing Arts"},
                new String[]{"Guitar (Acoustic/Electric)", "Music & Performing Arts"},
                new String[]{"Drums & Percussion", "Music & Performing Arts"},
                new String[]{"Violin / Strings", "Music & Performing Arts"},
                new String[]{"Music Theory & Composition", "Music & Performing Arts"},
                new String[]{"Music Production & DAW", "Music & Performing Arts"},
                new String[]{"DJing & Mixing", "Music & Performing Arts"},
                new String[]{"Acting & Theatre", "Music & Performing Arts"},
                new String[]{"Dancing (Contemporary/Ballet...)", "Music & Performing Arts"},
                new String[]{"Stand-up Comedy", "Music & Performing Arts"},

                // Sports & Fitness
                new String[]{"Strength & Weight Training", "Sports & Fitness"},
                new String[]{"Yoga & Pilates", "Sports & Fitness"},
                new String[]{"Running & Endurance Training", "Sports & Fitness"},
                new String[]{"Swimming", "Sports & Fitness"},
                new String[]{"Martial Arts (BJJ, Karate...)", "Sports & Fitness"},
                new String[]{"Rock Climbing & Bouldering", "Sports & Fitness"},
                new String[]{"Cycling & Mountain Biking", "Sports & Fitness"},
                new String[]{"Team Sports Coaching", "Sports & Fitness"},
                new String[]{"Nutrition & Sports Dietetics", "Sports & Fitness"},
                new String[]{"First Aid & CPR", "Sports & Fitness"},

                // Home & Lifestyle
                new String[]{"Home Repair & DIY", "Home & Lifestyle"},
                new String[]{"Plumbing Basics", "Home & Lifestyle"},
                new String[]{"Electrical Wiring Basics", "Home & Lifestyle"},
                new String[]{"Interior Design & Decorating", "Home & Lifestyle"},
                new String[]{"Gardening & Horticulture", "Home & Lifestyle"},
                new String[]{"Landscaping", "Home & Lifestyle"},
                new String[]{"Beekeeping", "Home & Lifestyle"},
                new String[]{"Car Maintenance & Mechanics", "Home & Lifestyle"},
                new String[]{"Personal Finance & Budgeting", "Home & Lifestyle"},
                new String[]{"Mindfulness & Meditation", "Home & Lifestyle"},
                new String[]{"Parenting & Child Development", "Home & Lifestyle"},
                new String[]{"Pet Care & Training", "Home & Lifestyle"}
            );

            for (String[] s : skills) {
                Skill skill = new Skill();
                skill.setName(s[0]);
                skill.setCategory(s[1]);
                skillRepository.save(skill);
            }
            System.out.println("Seeded " + skills.size() + " skills.");
    }
}