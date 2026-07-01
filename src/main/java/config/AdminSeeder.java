package config;

import enums.Role;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import repository.UserRepository;

@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL    = "admin@skillswap.dev";
    private static final String ADMIN_PASSWORD = "Admin@1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setName("System Admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setMfaEnabled(false);
        admin.setCredits(0);
        admin.setReputation(0);

        userRepository.save(admin);

        log.info("=== ADMIN ACCOUNT CREATED: email={} password={} ===", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
