package controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

import model.SponsorProgram;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.UserRepository;
import service.SponsorService;
import exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors")
@CrossOrigin(origins = "*")
public class SponsorController {

    private static final Logger log = LoggerFactory.getLogger(SponsorController.class);

    private final UserRepository userRepository;
    private final SponsorService sponsorService;

    public SponsorController(UserRepository userRepository, SponsorService sponsorService) {
        this.userRepository = userRepository;
        this.sponsorService = sponsorService;
    }

    @PostMapping("/coupons")
    public ResponseEntity<?> createCoupon(@RequestBody Map<String, String> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Coupon code is required"));
        }
        String discountStr = body.get("discount");
        if (discountStr == null || discountStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Discount percentage is required"));
        }
        int discount;
        try {
            discount = Integer.parseInt(discountStr);
            if (discount < 1 || discount > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Discount must be between 1 and 100"));
        }
        int maxUses = 100;
        String maxUsesStr = body.get("maxUses");
        if (maxUsesStr != null && !maxUsesStr.isBlank()) {
            try { maxUses = Math.max(1, Integer.parseInt(maxUsesStr)); } catch (NumberFormatException ignored) {}
        }
        String expiryDate = body.get("expiryDate");
        model.SponsorCoupon coupon = sponsorService.createCoupon(sponsor, code, discount, expiryDate, maxUses);
        return ResponseEntity.ok(Map.of(
            "id",         coupon.getId(),
            "code",       coupon.getCode(),
            "discount",   coupon.getDiscount(),
            "maxUses",    coupon.getMaxUses(),
            "usedCount",  coupon.getUsedCount(),
            "expiryDate", coupon.getExpiryDate() != null ? coupon.getExpiryDate() : "",
            "active",     coupon.isActive()
        ));
    }

    @GetMapping("/coupons")
    public ResponseEntity<?> listCoupons(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        List<Map<String, Object>> result = sponsorService.getActiveCoupons(sponsor).stream()
            .map(c -> Map.<String, Object>of(
                "id",         c.getId(),
                "code",       c.getCode() != null ? c.getCode() : "",
                "discount",   c.getDiscount() != null ? c.getDiscount() : 0,
                "maxUses",    c.getMaxUses() != null ? c.getMaxUses() : 0,
                "usedCount",  c.getUsedCount() != null ? c.getUsedCount() : 0,
                "expiryDate", c.getExpiryDate() != null ? c.getExpiryDate() : "",
                "active",     c.isActive()
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/programs")
    public ResponseEntity<?> createProgram(@RequestBody Map<String, String> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Program title is required"));
        }
        String description = body.get("description");
        String status = body.get("status");
        SponsorProgram program = sponsorService.createProgram(sponsor, title, description, status);
        return ResponseEntity.ok(Map.of(
            "id",            program.getId(),
            "title",         program.getTitle() != null ? program.getTitle() : "",
            "description",   program.getDescription() != null ? program.getDescription() : "",
            "status",        program.getStatus() != null ? program.getStatus() : "ACTIVE",
            "paymentStatus", program.getPaymentStatus() != null ? program.getPaymentStatus() : "PENDING"
        ));
    }

    @GetMapping("/programs")
    public ResponseEntity<?> listPrograms(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        List<Map<String, Object>> result = sponsorService.getPrograms(sponsor).stream()
            .map(p -> Map.<String, Object>of(
                "id",          p.getId(),
                "title",       p.getTitle() != null ? p.getTitle() : "",
                "description", p.getDescription() != null ? p.getDescription() : "",
                "status",      p.getStatus() != null ? p.getStatus() : "PENDING"
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getSponsorStats(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        Map<String, Object> stats = sponsorService.getMetrics(sponsor);
        stats.put("programs", sponsorService.getPrograms(sponsor));
        stats.put("coupons", sponsorService.getActiveCoupons(sponsor));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/programs/{id}/checkout")
    public ResponseEntity<Map<String, Object>> createCheckout(
            @PathVariable Long id, Authentication auth) {

        User sponsor = getUserFromAuth(auth);

        SponsorProgram program = sponsorService.findProgramById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        if (!program.getSponsor().getId().equals(sponsor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        if (!"PENDING".equals(program.getPaymentStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Program is not in PENDING payment state"));
        }

        if (program.getFundingAmount() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Program has no funding amount set"));
        }

        try {
            long amountInCents = program.getFundingAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(program.getTitle())
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setSuccessUrl("https://localhost:8443/sponsor-dashboard.html?payment=success")
                    .setCancelUrl("https://localhost:8443/sponsor-dashboard.html?payment=cancelled")
                    .putMetadata("programId", program.getId().toString())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("checkout-program-" + program.getId())
                    .build();

            Session session = Session.create(params, options);
            program.setStripeSessionId(session.getId());
            sponsorService.saveProgram(program);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (StripeException e) {
            log.error("Stripe error creating checkout for program {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment service unavailable, please try again later"));
        }
    }

    @GetMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        model.SponsorProfile p = sponsorService.getOrCreateProfile(sponsor);
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("name",         p.getName() != null ? p.getName() : sponsor.getName());
        resp.put("industry",     p.getIndustry() != null ? p.getIndustry() : "");
        resp.put("bio",          p.getBio() != null ? p.getBio() : "");
        resp.put("website",      p.getWebsite() != null ? p.getWebsite() : "");
        resp.put("location",     p.getLocation() != null ? p.getLocation() : "");
        resp.put("contactName",  p.getContactName() != null ? p.getContactName() : "");
        resp.put("contactTitle", p.getContactTitle() != null ? p.getContactTitle() : "");
        resp.put("contactEmail", p.getContactEmail() != null ? p.getContactEmail() : sponsor.getEmail());
        resp.put("contactPhone", p.getContactPhone() != null ? p.getContactPhone() : "");
        resp.put("linkedin",     p.getLinkedin() != null ? p.getLinkedin() : "");
        resp.put("foundedYear",  p.getFoundedYear());
        resp.put("companySize",  p.getCompanySize() != null ? p.getCompanySize() : "1-10");
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> saveProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        Integer foundedYear = null;
        if (body.get("foundedYear") != null && !body.get("foundedYear").toString().isBlank()) {
            try { foundedYear = Integer.parseInt(body.get("foundedYear").toString()); } catch (NumberFormatException ignored) {}
        }
        sponsorService.saveProfile(sponsor,
                str(body, "name"), str(body, "industry"), str(body, "bio"),
                str(body, "website"), str(body, "location"), str(body, "contactName"),
                str(body, "contactTitle"), str(body, "contactEmail"), str(body, "contactPhone"),
                str(body, "linkedin"), foundedYear, str(body, "companySize"));
        return ResponseEntity.ok(Map.of("message", "Profile saved"));
    }

    @GetMapping("/me/settings")
    public ResponseEntity<Map<String, Object>> getSettings(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        model.SponsorNotificationSettings s = sponsorService.getOrCreateSettings(sponsor);
        return ResponseEntity.ok(Map.of(
            "notifApplications",  s.isNotifApplications(),
            "notifCoupons",       s.isNotifCoupons(),
            "notifReports",       s.isNotifReports(),
            "notifAnnouncements", s.isNotifAnnouncements()
        ));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, Object> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        sponsorService.saveSettings(sponsor,
                bool(body, "notifApplications"),
                bool(body, "notifCoupons"),
                bool(body, "notifReports"),
                bool(body, "notifAnnouncements"));
        return ResponseEntity.ok(Map.of("message", "Settings saved"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        String current = body.get("currentPassword");
        String newPwd  = body.get("newPassword");
        if (current == null || newPwd == null || newPwd.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid password data"));
        }
        boolean ok = sponsorService.changePassword(sponsor, current, newPwd);
        if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Current password is incorrect"));
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    @GetMapping("/talent")
    public ResponseEntity<List<Map<String, Object>>> getTalent(Authentication auth) {
        List<Map<String, Object>> result = sponsorService.getTalentPool().stream()
            .map(u -> Map.<String, Object>of(
                "name",     u.getName() != null ? u.getName() : "",
                "email",    u.getEmail() != null ? u.getEmail() : "",
                "location", u.getLocation() != null ? u.getLocation() : "UAE"
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private static boolean bool(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
    }

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
