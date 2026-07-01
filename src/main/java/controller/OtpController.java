//package controller;
//
//import model.OtpRequest;
//import model.User;
//import repository.UserRepository;
//import service.EmailService;
//import service.OtpService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Random;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*")
//public class OtpController {
//
//    private final EmailService emailService;
//    private final OtpService otpService;
//    private final UserRepository userRepository;
//    private final Map<String, String> otpStorage = new HashMap<>();
//
//    public OtpController(EmailService emailService, OtpService otpService, UserRepository userRepository) {
//        this.emailService = emailService;
//        this.otpService = otpService;
//        this.userRepository = userRepository;
//    }
//
//    // ✅ Send OTP to user email (with exception handling)
//    @PostMapping("/send-otp")
//    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
//        try {
//            String otp = String.format("%04d", new Random().nextInt(10000));
//            otpStorage.put(request.getEmail(), otp);
//            otpService.saveOtp(request.getEmail(), otp);
//            emailService.sendOtpEmail(request.getEmail(), otp);
//            return ResponseEntity.ok("OTP sent to " + request.getEmail());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(Map.of("message", "An error occurred while sending OTP"));
//        }
//    }
//
//    // ✅ Verify OTP and enable user
//    @PostMapping("/verify-otp")
//    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
//        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
//        if (valid) {
//            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
//            if (userOptional.isPresent()) {
//                User user = userOptional.get();
//                user.setEnabled(true);
//                userRepository.save(user);
//                return ResponseEntity.ok(Map.of("message", "OTP verified and user account activated"));
//            } else {
//                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
//            }
//        } else {
//            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
//        }
//    }
//
//    // ✅ Authenticated OTP verification
//    @PostMapping("/verify-otp-authenticated")
//    public ResponseEntity<?> verifyOtpAuthenticated(@RequestBody OtpRequest request) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
//        }
//
//        if (otpStorage.containsKey(request.getEmail()) &&
//                otpStorage.get(request.getEmail()).equals(request.getOtp())) {
//            return ResponseEntity.ok(Map.of("message", "OTP Verified"));
//        }
//
//        return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
//    }
//
//    // ✅ Send OTP for email change
//    @PostMapping("/auth/request-email-change-otp")
//    public ResponseEntity<?> sendOtpForEmailChange(@RequestBody Map<String, String> request) {
//        try {
//            String email = request.get("newEmail");
//            String otp = String.format("%04d", new Random().nextInt(10000));
//            otpService.saveOtp(email, otp);
//            emailService.sendOtpEmail(email, otp);
//            return ResponseEntity.ok(Map.of("message", "OTP sent to new email."));
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(Map.of("message", "Failed to send OTP to new email"));
//        }
//    }
//
//    // ✅ Verify OTP for email change
//    @PostMapping("/auth/verify-email-change-otp")
//    public ResponseEntity<?> verifyOtpForEmailChange(@RequestBody Map<String, String> request) {
//        String email = request.get("newEmail");
//        String otp = request.get("otp");
//
//        boolean valid = otpService.verifyOtp(email, otp);
//        if (valid) {
//            return ResponseEntity.ok(Map.of("message", "OTP verified"));
//        } else {
//            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
//        }
//    }
//}

package controller;

import model.OtpRequest;
import model.User;
import repository.UserRepository;
import service.EmailService;
import service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OtpController {

    private final EmailService emailService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    public OtpController(EmailService emailService, OtpService otpService, UserRepository userRepository) {
        this.emailService = emailService;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        try {
            String otp = String.format("%04d", new Random().nextInt(10000));
            otpService.saveOtp(request.getEmail(), otp);
            emailService.sendOtpEmail(request.getEmail(), otp);
            return ResponseEntity.ok("OTP sent to " + request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "An error occurred while sending OTP"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (valid) {
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setEnabled(true);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "OTP verified and user account activated"));
            } else {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
        }
    }

    @PostMapping("/verify-otp-authenticated")
    public ResponseEntity<?> verifyOtpAuthenticated(@RequestBody OtpRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        // 🔥 Use OtpService instead of local map
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (valid) {
            return ResponseEntity.ok(Map.of("message", "OTP Verified"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
    }

    @PostMapping("/auth/request-email-change-otp")
    public ResponseEntity<?> sendOtpForEmailChange(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("newEmail");
            String otp = String.format("%04d", new Random().nextInt(10000));
            otpService.saveOtp(email, otp);
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(Map.of("message", "OTP sent to new email."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to send OTP to new email"));
        }
    }

    @PostMapping("/auth/verify-email-change-otp")
    public ResponseEntity<?> verifyOtpForEmailChange(@RequestBody Map<String, String> request) {
        String email = request.get("newEmail");
        String otp = request.get("otp");

        boolean valid = otpService.verifyOtp(email, otp);
        if (valid) {
            return ResponseEntity.ok(Map.of("message", "OTP verified"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
        }
    }
}
