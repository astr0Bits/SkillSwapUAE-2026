package security;
/*A Spring bean that handles what happens after a successful form-based login 
 * It redirects users to different pages based on their role.*/
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    //Called when a user logs in via the standard login form (e.g., /process-login). 
    //The authentication object contains the user’s details and authorities.
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        //Iterates over the user’s granted authorities (roles). 
    	//Converts the role name to lowercase for case‑insensitive comparison
    	for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority().toLowerCase();
            //Redirects based on role. If the role 
            if (role.contains("learner")) {
                response.sendRedirect("/learner.html");
                return;
            } else if (role.contains("sponsor")) {
                response.sendRedirect("/sponsor.html");
                return;
            }
        }
    	//If no suitable role is found, redirects to the login page with an error.
        response.sendRedirect("/login.html?error=unauthorized");
    }
}