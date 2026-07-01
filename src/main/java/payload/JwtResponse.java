package payload;
/*A standard DTO to send back to the client 
 * after authentication, containing the JWT 
 * and user information.
 * */
import java.util.List;

public class JwtResponse { // Defines the structure of the response after a successful login.

    private String token; //JWT string
    private String type = "Bearer"; //"Bearer" (default for JWT)
    private Long id;
    private String email;
    private String name;
    private String role;        
    private Integer credits;     
    private List<String> roles;// kept for backward compatibility

    public JwtResponse(String token, Long id, String email, String name, String role, Integer credits) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.credits = credits;
    }

    // Getters
    public String getToken() { return token; }
    public String getTokenType() { return type; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public Integer getCredits() { return credits; }
    public List<String> getRoles() { return roles; }  // optional, for role list
}