package service;

import enums.Role;
import model.PasswordHistory;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payload.JwtResponse;
import dto.LoginRequest;
import payload.MessageResponse;
import dto.SignupRequest;
import repository.PasswordHistoryRepository;
import repository.UserRepository;
import security.UserDetailsImpl;
import security.jwt.JwtUtils;
import exception.PasswordReuseException;
import validator.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final EmailService emailService;
	private final OtpService otpService;
	private final PasswordHistoryRepository passwordHistoryRepository;

	public AuthService(AuthenticationManager authenticationManager,
			JwtUtils jwtUtils,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService,
			EmailService emailService,
			OtpService otpService,
			PasswordHistoryRepository passwordHistoryRepository) {
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.emailService = emailService;
		this.otpService = otpService;
		this.passwordHistoryRepository = passwordHistoryRepository;
	}

	/**
	 * Authenticates a user with email and password, generates a JWT, and returns a JwtResponse.
	 *
	 * @param loginRequest the login credentials (email and password)
	 * @param request      the HttpServletRequest to capture IP address for audit logging
	 * @return JwtResponse containing the token and user details
	 * @throws AuthenticationException if authentication fails
	 */
	@Transactional
	public JwtResponse authenticate(LoginRequest loginRequest, HttpServletRequest request)
			throws AuthenticationException {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();

		logger.info("Attempting to authenticate user: {}", email);

		try {
			// 1. Authenticate using Spring Security's AuthenticationManager
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, password));

			// 2. Set authentication in security context
			SecurityContextHolder.getContext().setAuthentication(authentication);

			// 3. Generate JWT token
			String jwt = jwtUtils.generateJwtToken(authentication);

			// 4. Retrieve user details from the authenticated principal
			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

			// 5. Fetch full user entity to get additional fields like credits
			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new RuntimeException("User not found after authentication"));

			// 6. Log success
			auditLogService.logSuccess(email, "LOGIN", request, "User logged in successfully");

			// 7. Build and return response
			return new JwtResponse(
					jwt,
					user.getId(),
					user.getEmail(),
					user.getName(),
					user.getRole().name(),
					user.getCredits()
					);

		} catch (AuthenticationException e) {
			// Log failure
			auditLogService.logFailure(email, "LOGIN", request,
					"Authentication failed: " + e.getMessage());
			logger.warn("Authentication failed for {}: {}", email, e.getMessage());
			throw e;
		}
	}

	/**
	 * Registers a new user, sends an OTP for email verification.
	 *
	 * @param signupRequest the registration data
	 * @param request       the HttpServletRequest for audit logging
	 * @return MessageResponse indicating success (OTP sent)
	 */
	@Transactional
	public MessageResponse register(SignupRequest signupRequest, HttpServletRequest request) {
		String email = signupRequest.getEmail();
		// 1. Check if email is already taken
		if (userRepository.existsByEmail(email)) {
			logger.warn("Registration failed: email {} already exists", email);
			auditLogService.logFailure(email, "REGISTER", request, "Email already in use");
			throw new IllegalArgumentException("Email already in use");
		}
		// 2. Validate role (must be LEARNER or SPONSOR – admin creation is separate)
		Role role;
		try {
			role = Role.valueOf(signupRequest.getRole().toString().toUpperCase());
			if (role != Role.LEARNER && role != Role.SPONSOR) {
				throw new IllegalArgumentException("Invalid role type for registration");
			}
		} catch (IllegalArgumentException e) {
			logger.warn("Registration failed: invalid role");
			auditLogService.logFailure(email, "REGISTER", request, email);
			throw new IllegalArgumentException("Invalid role type");
		}
		// 3. Create new user (disabled until email verification)
		User user = new User();
		user.setEmail(email);
		String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
		user.setPassword(encodedPassword);
		user.setName(InputSanitizer.sanitize(signupRequest.getName()));
		String loc = signupRequest.getLocation();
		user.setLocation(loc != null ? InputSanitizer.sanitize(loc) : null);
		user.setRole(role);
		user.setEnabled(false);
		user.setMfaEnabled(false);
		user.setCredits(50);
		user.setReputation(0);
		// 4. Save to database
		userRepository.save(user);
		// 5. Record initial password in history
		recordPasswordHistory(user, encodedPassword);
		// 5. Generate OTP and send email
		String otp = generateOtp();
		otpService.saveOtp(email, otp);
		emailService.sendOtpEmail(email, otp);
		// 6. Log success
		auditLogService.logSuccess(email, "REGISTER", request, "User registered successfully, OTP sent");
		logger.info("User registered successfully: {}", email);

		return new MessageResponse("OTP sent to email. Please verify.");
	}

	/**
	 * Verifies OTP and enables the user account.
	 *
	 * @param email   the user's email
	 * @param otp     the OTP code
	 * @param request the HttpServletRequest for audit logging
	 * @return true if verification succeeded, false otherwise
	 */
	@Transactional
	public boolean verifyOtp(String email, String otp, HttpServletRequest request) {
		if (otpService.verifyOtp(email, otp)) {
			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new IllegalArgumentException("User not found"));
			user.setEnabled(true);
			userRepository.save(user);
			auditLogService.logSuccess(email, "OTP_VERIFIED", request, "OTP verified, account enabled");
			return true;
		} else {
			auditLogService.logFailure(email, "OTP_VERIFICATION_FAILED", request, "Invalid OTP entered");
			return false;
		}
	}

//	/**
//	 * Requests a password reset: generates a reset code, stores it in the user record,
//	 * and sends it via email.
//	 *
//	 * @param email   the user's email
//	 * @param request the HttpServletRequest for audit logging
//	 * @return MessageResponse indicating success or failure
//	 */
//	@Transactional
//	public MessageResponse requestPasswordReset(String email, HttpServletRequest request) {
//		Optional<User> userOpt = userRepository.findByEmail(email);
//		if (userOpt.isEmpty()) {
//			throw new IllegalArgumentException("Email not found");
//		}
//
//		User user = userOpt.get();
//		String resetCode = generateOtp();
//		user.setResetCode(resetCode);
//		userRepository.save(user);
//
//		emailService.sendOtpEmail(email, resetCode); // reuse OTP email template
//		auditLogService.logSuccess(email, "PASSWORD_RESET_REQUEST", request, "Password reset code sent");
//		return new MessageResponse("Reset code sent to your email.");
//	}
//
//	/**
//	 * Resets the password after verifying the reset code.
//	 *
//	 * @param email        the user's email
//	 * @param resetCode    the code sent to the user
//	 * @param newPassword  the new password
//	 * @param request      the HttpServletRequest for audit logging
//	 * @return MessageResponse indicating success or failure
//	 */
//	@Transactional
//	public MessageResponse resetPassword(String email, String resetCode, String newPassword, HttpServletRequest request) {
//		User user = userRepository.findByEmail(email).orElse(null);
//		if (user == null || !resetCode.equals(user.getResetCode())) {
//			auditLogService.logFailure(email, "PASSWORD_RESET_FAILED", request, "Invalid email or reset code");
//			throw new IllegalArgumentException("Invalid email or reset code.");
//		}
//
//		user.setPassword(passwordEncoder.encode(newPassword));
//		user.setResetCode(null); // clear the reset code
//		userRepository.save(user);
//
//		auditLogService.logSuccess(email, "PASSWORD_RESET_SUCCESS", request, "Password reset successfully");
//		return new MessageResponse("Password reset successful. You can now log in.");
//	}
	
	
	// In AuthService.java

	@Transactional
	public MessageResponse requestPasswordReset(String email, HttpServletRequest request) {
	    Optional<User> userOpt = userRepository.findByEmail(email);
	    if (userOpt.isEmpty()) {
	        throw new IllegalArgumentException("Email not found");
	    }

	    User user = userOpt.get();
	    String resetCode = generateOtp();
	    user.setResetCode(resetCode);
	    user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(15));
	    userRepository.save(user);

	    emailService.sendOtpEmail(email, resetCode);
	    auditLogService.logSuccess(email, "PASSWORD_RESET_REQUEST", request, "Password reset code sent");
	    return new MessageResponse("Reset code sent to your email.");
	}

	@Transactional
	public MessageResponse resetPassword(String email, String resetCode, String newPassword, HttpServletRequest request) {
	    User user = userRepository.findByEmail(email).orElse(null);
	    if (user == null || !resetCode.equals(user.getResetCode()) ||
	            user.getResetCodeExpiry() == null ||
	            user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
	        auditLogService.logFailure(email, "PASSWORD_RESET_FAILED", request, "Invalid or expired reset code");
	        throw new IllegalArgumentException("Invalid or expired reset code.");
	    }
	    validateAndStorePasswordHistory(user, newPassword);
	    user.setPassword(passwordEncoder.encode(newPassword));
	    user.setResetCode(null);
	    user.setResetCodeExpiry(null);
	    userRepository.save(user);

	    auditLogService.logSuccess(email, "PASSWORD_RESET_SUCCESS", request, "Password reset successfully");
	    return new MessageResponse("Password reset successful. You can now log in.");
	}

	/**
	 * Sends an OTP for email change or other sensitive operations.
	 *
	 * @param email   the target email address
	 * @param request the HttpServletRequest for audit logging
	 * @return MessageResponse indicating success
	 */
	@Transactional
	public MessageResponse sendOtp(String email, HttpServletRequest request) {
		String otp = generateOtp();
		otpService.saveOtp(email, otp);
		emailService.sendOtpEmail(email, otp);
		auditLogService.logSuccess(email, "OTP_SENT", request, "OTP sent");
		return new MessageResponse("OTP sent to " + email);
	}

	/**
	 * Verifies OTP without enabling account (e.g., for email change verification).
	 *
	 * @param email   the user's email
	 * @param otp     the OTP code
	 * @param request the HttpServletRequest for audit logging
	 * @return true if OTP matches, false otherwise
	 */
	public boolean verifyOtpOnly(String email, String otp, HttpServletRequest request) {
		boolean valid = otpService.verifyOtp(email, otp);
		if (valid) {
			auditLogService.logSuccess(email, "OTP_VERIFIED_ONLY", request, "OTP verified");
		} else {
			auditLogService.logFailure(email, "OTP_VERIFICATION_FAILED", request, "Invalid OTP entered");
		}
		return valid;
	}

	/**
	 * Checks that newRawPassword has not been used recently (current password + last 10 history),
	 * then saves the new hash to password history. Call this BEFORE setting user.password.
	 */
	@Transactional
	public void validateAndStorePasswordHistory(User user, String newRawPassword) {
		if (passwordEncoder.matches(newRawPassword, user.getPassword())) {
			throw new PasswordReuseException(
					"You cannot reuse a recent password. Please choose a different one.");
		}
		List<PasswordHistory> history =
				passwordHistoryRepository.findTop10ByUserOrderByCreatedAtDesc(user);
		for (PasswordHistory h : history) {
			if (passwordEncoder.matches(newRawPassword, h.getPasswordHash())) {
				throw new PasswordReuseException(
						"You cannot reuse a recent password. Please choose a different one.");
			}
		}
		recordPasswordHistory(user, passwordEncoder.encode(newRawPassword));
	}

	/** Saves an already-encoded password to history and trims to 10 records. */
	@Transactional
	public void recordPasswordHistory(User user, String encodedPassword) {
		passwordHistoryRepository.save(new PasswordHistory(user, encodedPassword));
		List<PasswordHistory> all =
				passwordHistoryRepository.findByUserOrderByCreatedAtAsc(user);
		if (all.size() > 10) {
			passwordHistoryRepository.deleteAll(all.subList(0, all.size() - 10));
		}
	}

	// Helper method to generate a 6-digit OTP
	private String generateOtp() {
		return String.format("%06d", new Random().nextInt(1000000));
	}
}