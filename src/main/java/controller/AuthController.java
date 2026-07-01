package controller;

import dto.LoginRequest;
import dto.SignupRequest;
import dto.UpdateAccountRequest;
import model.User;
import payload.MessageResponse;
import repository.UserRepository;
import security.UserDetailsImpl;
import security.jwt.JwtUtils;
import service.AuditLogService;
import service.EmailService;
import service.MfaService;
import service.OtpService;
import service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import validator.InputSanitizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

	private static final String MESSAGE = "message";
	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final JwtUtils jwtUtils;
	private final OtpService otpService;
	private final AuthService authService;
	private final EmailService emailService;
	private final AuditLogService auditLogService;
	private final MfaService mfaService;  // new dependency

	public AuthController(AuthenticationManager authenticationManager,
			UserRepository userRepository,
			PasswordEncoder encoder,
			JwtUtils jwtUtils,
			OtpService otpService,
			EmailService emailService,
			AuditLogService auditLogService,
			MfaService mfaService,
			AuthService authService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.encoder = encoder;
		this.jwtUtils = jwtUtils;
		this.otpService = otpService;
		this.emailService = emailService;
		this.auditLogService = auditLogService;
		this.mfaService = mfaService;
		this.authService = authService;
	}

	// ==============================
	// Registration & Email Verification
	// ==============================

	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest,
			HttpServletRequest request) {
		String email = signupRequest.getEmail();
		String name = signupRequest.getName();

		if (userRepository.existsByEmail(email)) {
			auditLogService.logFailure(email, "REGISTER", request, "Email already in use");
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Email is already in use!"));
		}

		User user = new User();
		user.setName(InputSanitizer.sanitize(name));
		user.setEmail(email);
		String encodedPwd = encoder.encode(signupRequest.getPassword());
		user.setPassword(encodedPwd);
		String loc = signupRequest.getLocation();
		user.setLocation(loc != null ? InputSanitizer.sanitize(loc) : null);
		user.setRole(signupRequest.getRole());
		user.setEnabled(false);
		user.setCredits(50);
		user.setReputation(0);
		user.setMfaEnabled(false);
		user.setMfaSecret(null);

		userRepository.save(user);
		authService.recordPasswordHistory(user, encodedPwd);
		auditLogService.logSuccess(email, "REGISTER", request, "User registered successfully");

		String otp = generateOtp();
		otpService.saveOtp(email, otp);
		emailService.sendOtpEmail(email, otp);

		return ResponseEntity.ok(Map.of(MESSAGE, "OTP sent to email. Please verify."));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		String otp = request.get("otp");

		if (otpService.verifyOtp(email, otp)) {
			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new IllegalArgumentException("User not found"));
			user.setEnabled(true);
			userRepository.save(user);
			auditLogService.logSuccess(email, "OTP VERIFIED", null, "OTP verified successfully");
			return ResponseEntity.ok(Map.of(MESSAGE, "OTP verified. Please log in."));
		} else {
			auditLogService.logFailure(email, "OTP VERIFICATION FAILED", null, "Invalid OTP entered");
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Invalid OTP"));
		}
	}

	@PostMapping("/resend-otp")
	public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> payload, HttpServletRequest request) {
		String email = payload.get("email");
		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(new MessageResponse("Email address is required"));
		}
		try {
			authService.sendOtp(email, request);
			return ResponseEntity.ok(new MessageResponse("OTP sent to " + email));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
		} catch (Exception e) {
			logger.error("Error resending OTP to {}", email, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new MessageResponse("Failed to send OTP. Please try again later."));
		}
	}

	// ==============================
	// Login with MFA support
	// ==============================

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, password)
					);

			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new RuntimeException("User not found after authentication"));

			if (user.isMfaEnabled()) {
				String tempToken = generateTempToken(email);
				return ResponseEntity.ok(Map.of(
						"requiresMfa", true,
						"tempToken", tempToken,
						"message", "Please provide your TOTP code to complete login."
						));
			}

			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);
			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			Set<String> roles = extractRoles(userDetails);

			Map<String, Object> response = buildLoginResponse(jwt, userDetails, roles);
			auditLogService.logSuccess(email, "LOGIN", request, "User logged in successfully");
			return ResponseEntity.ok(response);

		} catch (BadCredentialsException e) {
			auditLogService.logFailure(email, "LOGIN", request, "Bad credentials");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of(MESSAGE, "Invalid email or password."));
		} catch (DisabledException e) {
			auditLogService.logFailure(email, "LOGIN", request, "Account not verified");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of(MESSAGE, "Account not verified. Please check your email."));
		} catch (Exception e) {
			logger.error("Login error for {}: {}", email, e.getMessage());
			auditLogService.logFailure(email, "LOGIN", request, "Unexpected error: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of(MESSAGE, "Authentication failed."));
		}
	}

	@PostMapping("/verify-mfa")
	public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> payload,
			HttpServletRequest request) {
		String tempToken = payload.get("tempToken");
		String totpCode = payload.get("totpCode");

		if (tempToken == null || totpCode == null) {
			return ResponseEntity.badRequest().body(Map.of(MESSAGE, "Missing temporary token or TOTP code."));
		}

		try {
			String email = jwtUtils.getEmailFromTempToken(tempToken);
			if (email == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of(MESSAGE, "Invalid or expired temporary token."));
			}

			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new RuntimeException("User not found"));
			if (!mfaService.verifyCode(user.getMfaSecret(), totpCode)) {
				auditLogService.logFailure(email, "MFA_VERIFY", request, "Invalid TOTP code");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of(MESSAGE, "Invalid TOTP code."));
			}

			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, user.getPassword())
					);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			Set<String> roles = extractRoles(userDetails);

			Map<String, Object> response = buildLoginResponse(jwt, userDetails, roles);
			auditLogService.logSuccess(email, "MFA_VERIFY", request, "MFA verified, login successful");
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			logger.error("MFA verification error: {}", e.getMessage());
			auditLogService.logFailure("unknown", "MFA_VERIFY", request, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of(MESSAGE, "MFA verification failed."));
		}
	}

	// ==============================
	// Password Management
	// ==============================

	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		Optional<User> userOpt = userRepository.findByEmail(email);

		if (userOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of(MESSAGE, "Email not found."));
		}

		User user = userOpt.get();
		String resetCode = generateOtp();
		user.setResetCode(resetCode);
		user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(15));
		userRepository.save(user);

		emailService.sendOtpEmail(email, resetCode);
		return ResponseEntity.ok(Map.of(MESSAGE, "Reset code sent to your email."));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
		String email = payload.get("email");
		String code = payload.get("resetCode");
		String newPassword = payload.get("newPassword");

		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null || !code.equals(user.getResetCode())
				|| user.getResetCodeExpiry() == null
				|| user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
			return ResponseEntity.badRequest().body(
					Map.of("error", "Reset code has expired. Please request a new one."));
		}

		// Throws PasswordReuseException (handled by GlobalExceptionHandler → 400)
		authService.validateAndStorePasswordHistory(user, newPassword);

		user.setPassword(encoder.encode(newPassword));
		user.setResetCode(null);
		user.setResetCodeExpiry(null);
		userRepository.save(user);

		return ResponseEntity.ok(Map.of(MESSAGE, "Password reset successful. You can now log in."));
	}

	@PostMapping("/verify-password")
	public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> payload,
			Authentication authentication) {
		String currentUserEmail = authentication.getName(); // email
		String email = payload.get("email");
		if (!currentUserEmail.equals(email)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("message", "You can only verify your own password."));
		}

		String currentPassword = payload.get("currentPassword");
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null || !encoder.matches(currentPassword, user.getPassword())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "Invalid current password"));
		}
		return ResponseEntity.ok(Map.of("message", "Password verified"));
	}

	// ==============================
	// Account Management
	// ==============================

	@PostMapping("/send-email-otp")
	public ResponseEntity<?> sendEmailOtp(@RequestBody Map<String, String> payload) {
		String email = payload.get("email");
		String otp = generateOtp();
		otpService.saveOtp(email, otp);
		emailService.sendOtpEmail(email, otp);
		return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
	}

	//    @PutMapping("/update-account")
	//    public ResponseEntity<?> updateAccount(@Valid @RequestBody UpdateAccountRequest updateRequest,
	//                                           Authentication authentication,
	//                                           HttpServletRequest request) {
	//        String currentUserEmail = authentication.getName();
	//        String emailToUpdate = updateRequest.getEmail();
	//
	//        if (!currentUserEmail.equals(emailToUpdate)) {
	//            return ResponseEntity.status(HttpStatus.FORBIDDEN)
	//                    .body(Map.of("message", "You can only update your own account."));
	//        }
	//
	//        User user = userRepository.findByEmail(emailToUpdate)
	//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
	//
	//        // Update full name if provided
	//        if (updateRequest.getName() != null && !updateRequest.getName().isBlank()) {
	//            user.setName(updateRequest.getName());
	//        }
	//
	//        // Update email if provided and changed
	//        if (updateRequest.getNewEmail() != null && !updateRequest.getNewEmail().equals(user.getEmail())) {
	//            if (userRepository.existsByEmail(updateRequest.getNewEmail())) {
	//                return ResponseEntity.badRequest().body(Map.of("message", "New email already in use."));
	//            }
	//            user.setEmail(updateRequest.getNewEmail());
	//        }
	//
	//        // Update location if provided
	//        if (updateRequest.getLocation() != null) {
	//            user.setLocation(updateRequest.getLocation());
	//        }
	//
	//        // Update password if provided
	//        if (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().isBlank()) {
	//            user.setPassword(encoder.encode(updateRequest.getNewPassword()));
	//        }
	//
	//        userRepository.save(user);
	//        auditLogService.logSuccess(currentUserEmail, "ACCOUNT_UPDATED", request, "Account updated successfully");
	//        return ResponseEntity.ok(Map.of("message", "Account updated"));
	//    }

	// Inside AuthController.java, modify updateAccount method:

	@PutMapping("/update-account")
	public ResponseEntity<?> updateAccount(@Valid @RequestBody UpdateAccountRequest updateRequest,
			Authentication authentication,
			HttpServletRequest request) {
		String currentUserEmail = authentication.getName();
		String emailToUpdate = updateRequest.getEmail();

		if (!currentUserEmail.equals(emailToUpdate)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("message", "You can only update your own account."));
		}

		User user = userRepository.findByEmail(emailToUpdate)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// 🔥 FIX: if updating email or password, require current password
		boolean requiresReauth = (updateRequest.getNewEmail() != null && !updateRequest.getNewEmail().equals(user.getEmail()))
				|| (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().isBlank());

		if (requiresReauth) {
			if (updateRequest.getCurrentPassword() == null ||
					!encoder.matches(updateRequest.getCurrentPassword(), user.getPassword())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("message", "Current password is required or incorrect."));
			}
		}

		// Update full name if provided
		if (updateRequest.getName() != null && !updateRequest.getName().isBlank()) {
			user.setName(InputSanitizer.sanitize(updateRequest.getName()));
		}

		// Update email if provided and changed
		if (updateRequest.getNewEmail() != null && !updateRequest.getNewEmail().equals(user.getEmail())) {
			if (userRepository.existsByEmail(updateRequest.getNewEmail())) {
				return ResponseEntity.badRequest().body(Map.of("message", "New email already in use."));
			}
			user.setEmail(updateRequest.getNewEmail());
		}

		// Update location if provided
		if (updateRequest.getLocation() != null) {
			user.setLocation(InputSanitizer.sanitize(updateRequest.getLocation()));
		}

		// Update password if provided
		if (updateRequest.getNewPassword() != null && !updateRequest.getNewPassword().isBlank()) {
			// Throws PasswordReuseException (handled by GlobalExceptionHandler → 400)
			authService.validateAndStorePasswordHistory(user, updateRequest.getNewPassword());
			user.setPassword(encoder.encode(updateRequest.getNewPassword()));
		}

		userRepository.save(user);
		auditLogService.logSuccess(currentUserEmail, "ACCOUNT_UPDATED", request, "Account updated successfully");
		return ResponseEntity.ok(Map.of("message", "Account updated"));
	}
	// ==============================
	// Helper Methods
	// ==============================

	private Map<String, Object> buildLoginResponse(String jwt, UserDetailsImpl userDetails, Set<String> roles) {
		Map<String, Object> response = new HashMap<>();
		response.put("token", jwt);
		response.put("username", userDetails.getName());
		response.put("email", userDetails.getEmail());
		response.put("roles", roles);

		if (roles.contains("admin")) {
			response.put("redirectUrl", "/admin-dashboard.html");
		} else if (roles.contains("sponsor")) {
			response.put("redirectUrl", "/sponsor-dashboard.html");
		} else {
			response.put("redirectUrl", "/dashboard.html");
		}
		return response;
	}

	private Set<String> extractRoles(UserDetailsImpl userDetails) {
		return userDetails.getAuthorities().stream()
				.map(auth -> auth.getAuthority().replace("ROLE_", "").toLowerCase())
				.collect(Collectors.toSet());
	}

	private String generateTempToken(String email) {
		return jwtUtils.generateTempToken(email); // assume this method exists
	}

	private String generateOtp() {
		return String.format("%06d", new Random().nextInt(1000000));
	}
}