package security;

// handling HTTP requests/responses and exceptions.
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//imports SLF4J logging classes.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//Spring Security interface for handling authentication errors 
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component 
//Spring bean that implements AuthenticationEntryPoint. 
//This class will be invoked whenever an unauthenticated user 
//attempts to access a secured resource.
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

	//Creates a logger instance to log error messages.
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    //Overrides the commence method, which is called when authentication fails.
    //request – the HTTP request that caused the error.
    //response – the HTTP response to send.
    //authException – the exception that describes the authentication failure.
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String requestedUri = request.getRequestURI();//Retrieves the requested URI to log which endpoint was accessed.
        logger.error("Unauthorized access attempt to '{}'. Reason: {}", requestedUri, authException.getMessage()); //Logs an error with the URI and the reason for failure.

        //Sends a 401 Unauthorized HTTP status code with a generic error message. 
        //This is the standard response for unauthenticated requests.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
    }
}
