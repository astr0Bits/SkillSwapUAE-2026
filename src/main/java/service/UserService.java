package service;
/*Encapsulates business logic for user profile updates. 
 * It ensures data consistency (email uniqueness, password encoding) 
 * and uses the repository for persistence.
*/
import dto.UserUpdateDTO;
import model.User;
import repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;//Repository dependency.

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    //Helper method to fetch a user by email, 
    //returning Optional to gracefully handle absence.
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {//Persists a user entity.
        return userRepository.save(user);
    }

    //Takes the current email (used to locate the user), the DTO, and a PasswordEncoder (provided by the controller).
    public User updateUser(String email, @Valid UserUpdateDTO updateDTO, PasswordEncoder passwordEncoder) {
        //Throws an IllegalArgumentException if the user does not exist.
    	User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

    	//Updates name if provided.
        if (updateDTO.getName() != null) user.setName(updateDTO.getName());
        //If a new email is provided and it’s different from the current one, 
        //checks if it’s already taken. If not, updates the email.
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(updateDTO.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(updateDTO.getEmail());
        }
        //Updates location if provided.
        if (updateDTO.getLocation() != null) user.setLocation(updateDTO.getLocation());
        //If a new password is provided and not blank, encodes it with the given PasswordEncoder and updates the password.
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }
        // Credits and reputation could be updated by business logic, not directly via DTO
        //Saves the updated user and returns it.
        return userRepository.save(user);
    }
}