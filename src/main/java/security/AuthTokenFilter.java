package security;

import security.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom JWT authentication filter that intercepts every request once per request.
 * It extracts the JWT from the Authorization header, validates it, and sets the
 * authentication in the Spring Security context if the token is valid.
 * <p>
 * This filter skips public and static resources to avoid unnecessary processing.
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    /** Utility class for JWT operations (generation, validation, extraction). */
    private final JwtUtils jwtUtils;

    /** Service to load user details by email (username). */
    private final UserDetailsService userDetailsService;

    /**
     * Constructor for dependency injection.
     *
     * @param jwtUtils            the JWT utility bean
     * @param userDetailsService  the user details service
     */
    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core filtering method called for every HTTP request.
     * It performs the following steps:
     * <ol>
     *   <li>Skips static resources and public endpoints (e.g., /api/auth/**).</li>
     *   <li>Extracts the JWT from the "Authorization" header.</li>
     *   <li><strong>Validates the JWT</strong> using {@link JwtUtils#validateJwtToken(String)}.</li>
     *   <li>If valid, loads the user details by email and sets authentication in the security context.</li>
     * </ol>
     * If the token is missing or invalid, the request continues without authentication.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip static resources and public authentication endpoints
        if (uri.matches(".*\\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|otf|eot|webp|avif)$") ||
                uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") ||
                uri.startsWith("/static/") || uri.startsWith("/webjars/") ||
                uri.startsWith("/api/auth/") || uri.equals("/") || uri.endsWith(".html")) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("AuthTokenFilter invoked for request URI: {}", uri);

        try {
            // Attempt to extract JWT from the Authorization header
            String jwt = parseJwt(request);

            // ---------- JWT VALIDATION OCCURS HERE ----------
            // The method jwtUtils.validateJwtToken(jwt) checks token signature, expiration, and format.
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Extract the user's email (subject) from the token
                String email = jwtUtils.getEmailFromJwtToken(jwt);

                // Load user details (including authorities) using the email as username
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Create an authentication token with the user details and authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Attach request details (remote address, session id, etc.) to the authentication
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the security context – user is now authenticated
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // If any error occurs (e.g., invalid token, user not found), we log it but do not block the request.
            // The security context remains empty, so the request will be treated as unauthenticated.
            logger.error("Cannot set user authentication: {}", e.getMessage(), e);
        }

        // Continue the filter chain – subsequent filters and controllers will see the authenticated user if set.
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to extract the JWT token from the HTTP Authorization header.
     * <p>
     * The header must have the format: {@code Bearer <token>}.
     *
     * @param request the HTTP request
     * @return the JWT string if present and correctly prefixed, otherwise {@code null}
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}