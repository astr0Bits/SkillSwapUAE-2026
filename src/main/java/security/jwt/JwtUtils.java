package security.jwt;

/*
 * JwtUtils provides all core JWT operations:
 * generating tokens, validating them, extracting the user's email,
 * and handling the signing key. It acts as a central service
 * for JWT management in the application.
 */

import io.jsonwebtoken.*; // JJWT library for creating and parsing JWT tokens.
// helpers for decoding Base64 and creating HMAC keys.
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
// for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // make this class a Spring bean
import org.springframework.security.core.Authentication; // Spring Security’s authentication object.
import org.springframework.util.StringUtils; // helper for checking if a string has text

import config.JwtProperties; // custom configuration bean
import security.UserDetailsImpl; // a custom implementation of Spring Security’s UserDetails

import jakarta.servlet.http.HttpServletRequest; // to extract the token from the HTTP request header.
import java.security.Key; // for the signing key.
import java.util.Date; // for token expiration timestamps.

@Component // Spring bean that can be autowired.
public class JwtUtils {

	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class); // Creates a SLF4J logger for this class.
	private final JwtProperties jwtProperties; // injected via constructor to access the secret.
	private final int jwtExpirationMs = 86400000; // expiration time in milliseconds (24 hours)

	public JwtUtils(JwtProperties jwtProperties) { // Constructor‑based injection of JwtProperties
		this.jwtProperties = jwtProperties;
	}

	//	/**
	//	 * Generates a JWT token from an Authentication object.
	//	 * The subject of the token is set to the user's email.
	//	 */
	//	public String generateJwtToken(Authentication authentication) {
	//		UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal(); // Extracts the UserDetailsImpl
	//		// userPrincipal.getUsername() returns the email
	//		return Jwts.builder() // start building the token
	//				.setSubject(userPrincipal.getUsername()) // sets the subject to the user's email
	//				.setIssuedAt(new Date()) // sets the issue time to now
	//				.setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // sets the expiration time
	//				.signWith(key(), SignatureAlgorithm.HS256) // signs the token using HMAC‑SHA256
	//				.compact(); // builds the final token string
	//	}

	public String generateJwtToken(Authentication authentication) {
		UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
		// 🔥 Use configured expiration
		Date expiry = new Date((new Date()).getTime() + jwtProperties.getExpirationMs());
		return Jwts.builder()
				.setSubject(userPrincipal.getUsername())
				.setIssuedAt(new Date())
				.setExpiration(expiry)
				.signWith(key(), SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * Builds the signing key from the Base64‑encoded secret in JwtProperties.
	 */
	private Key key() {
		String secret = jwtProperties.getSecret().trim(); // Retrieves the Base64‑encoded secret
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); // Decodes and converts to HMAC‑SHA key
	}

	/**
	 * Extracts the user's email (subject) from a JWT token.
	 */
	public String getEmailFromJwtToken(String token) {
		return Jwts.parserBuilder() // create a parser
				.setSigningKey(key()) // set the signing key for verification
				.build()
				.parseClaimsJws(token) // parses the token and validates signature/structure
				.getBody()
				.getSubject(); // retrieves the email (subject)
	}

	/**
	 * Checks if a JWT token is valid.
	 */
	public boolean validateJwtToken(String authToken) {
		try { // Tries to parse the token; if successful, returns true
			Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
			return true;
		} catch (Exception e) { // Catches any exception (expired, malformed, signature mismatch) and logs it
			logger.error("JWT error: {}", e.getMessage());
		}
		return false;
	}

	/**
	 * Extracts the user's email directly from the HTTP request's Authorization header.
	 */
	public String getEmailFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization"); // Retrieves the Authorization header
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			String token = bearerToken.substring(7); // Removes the "Bearer " prefix
			return getEmailFromJwtToken(token); // returns the email
		}
		return null;
	}

	//	// Generates a temporary token (short expiry, e.g., 5 minutes)
	//	public String generateTempToken(String email) {
	//		int tempExpirationMs = 300000; // 5 minutes in milliseconds
	//		return Jwts.builder()
	//				.setSubject(email)
	//				.setIssuedAt(new Date())
	//				.setExpiration(new Date(System.currentTimeMillis() + tempExpirationMs))
	//				.signWith(key(), SignatureAlgorithm.HS256)
	//				.compact();
	//	}

	public String generateTempToken(String email) {
		int tempExpirationMs = 300000; // 5 minutes – this could also be configured
		return Jwts.builder()
				.setSubject(email)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + tempExpirationMs))
				.signWith(key(), SignatureAlgorithm.HS256)
				.compact();
	}

	// Extracts email from a temporary token (same as getUserNameFromJwtToken)
	public String getEmailFromTempToken(String token) {
		return getEmailFromJwtToken(token);
	}
}