package security;

import model.User;
import enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
//Implements UserDetails, which provides core user information to Spring Security.
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;//For serialization.

    //Fields that mirror the User entity, plus authorities (roles). 
    //All fields are final, set in constructor.
    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final String location;
    private final Integer credits;
    private final Integer reputation;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String email, String password, String name,
                           String location, Integer credits, Integer reputation,
                           boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.location = location;
        this.credits = credits;
        this.reputation = reputation;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    /*Static factory method to build a UserDetailsImpl from a User entity. 
     * It creates a single GrantedAuthority with the role name 
     * (e.g., "ROLE_LEARNER" or "LEARNER" depending on convention;
     *  here it's just the role name as a string, 
     *  but typically roles are prefixed with "ROLE_").*/
    public static UserDetailsImpl build(User user) {
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                user.getLocation(),
                user.getCredits(),
                user.getReputation(),
                user.isEnabled(),
                Collections.singletonList(authority)
        );
    }

    @Override
    //Returns the user’s authorities (roles).
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    //Returns the password.
    public String getPassword() {
        return password;
    }

    @Override
    //Returns the email as the username. This is critical – 
    //Spring Security will use the email as the principal identifier.
    public String getUsername() {
        return email; // email is used as username for authentication
    }

    @Override
    //Account never expires.
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    //Account never locked.
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    //Credentials never expire.
    public boolean isCredentialsNonExpired() {
        return true;
    }

    //¿¿DO WE NEED??
    @Override
    //Returns the account enabled status from the user entity.
    public boolean isEnabled() {
        return enabled;
    }

    // Additional getters for custom fields (optional)
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public Integer getCredits() { return credits; }
    public Integer getReputation() { return reputation; }
}