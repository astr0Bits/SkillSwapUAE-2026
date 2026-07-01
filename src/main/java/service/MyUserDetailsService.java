package service;
/* This is the bridge between Spring Security and the user data. 
 * It loads user details (including password, authorities, enabled status) by email.
 * It is used both by form login and by JWT authentication.*/
import model.User;
import repository.UserRepository;
import security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
//Implements Spring Security’s UserDetailsService. It will be used by the authentication manager to load users by username (which is email).
public class MyUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailsService.class);

    @Autowired //Injects the user repository.
    private UserRepository userRepository;

    @Override
    //Overrides the interface method. email is the login identifier (from the login form or JWT). Logs the attempt.
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.info("Attempting to load user by email: {}", email);

        //Tries to find the user by email. 
        //If not found, logs a warning and throws UsernameNotFoundException.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User Not Found with email: " + email);
                });

        //Logs success and the user’s role.
        logger.info("User found: {}", user.getEmail());
        logger.debug("Assigned role: {}", user.getRole());

        //Converts the User entity into a UserDetailsImpl object, 
        //which is returned to Spring Security.
        return UserDetailsImpl.build(user);
    }
}