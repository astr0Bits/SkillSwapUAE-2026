package controller;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import model.User;
import repository.UserRepository;
import service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/mfa")
public class MfaController {
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private MfaService mfaService;
    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa() {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        // We will enable only after verification (so keep false for now)
        user.setMfaEnabled(false);
        userRepo.save(user);

        // Generate QR code URI
        String qrCodeUri = mfaService.getQRCodeImage(secret, email);

        return ResponseEntity.ok(Map.of(
                "qrCodeUri", qrCodeUri,
                "secret", secret,
                "message", "Scan the QR code with your authenticator app, then verify."
        ));
    }

    @PostMapping("/verify-enable")
    public ResponseEntity<?> verifyAndEnable(@RequestBody Map<String, String> payload) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        String email = auth.getName();
        String code = payload.get("code");

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMfaSecret() == null) {
            return ResponseEntity.badRequest().body("MFA not enabled yet");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            return ResponseEntity.badRequest().body("Invalid code");
        }

        user.setMfaEnabled(true);
        userRepo.save(user);
        return ResponseEntity.ok("MFA enabled successfully");
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        String email = auth.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepo.save(user);
        return ResponseEntity.ok("MFA disabled");
    }
    
 // In MfaController.java
    @GetMapping("/status")
    public ResponseEntity<?> getMfaStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        String email = auth.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of("enabled", user.isMfaEnabled()));
    }
}