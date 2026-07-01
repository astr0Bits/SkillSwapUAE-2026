package service;

import model.*;
import repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SponsorService {

    private final SponsorProfileRepository profileRepo;
    private final SponsorProgramRepository programRepo;
    private final SponsorCouponRepository couponRepo;
    private final SponsorNotificationSettingsRepository settingsRepo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SponsorService(SponsorProfileRepository profileRepo,
                          SponsorProgramRepository programRepo,
                          SponsorCouponRepository couponRepo,
                          SponsorNotificationSettingsRepository settingsRepo,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.profileRepo = profileRepo;
        this.programRepo = programRepo;
        this.couponRepo = couponRepo;
        this.settingsRepo = settingsRepo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SponsorProfile getOrCreateProfile(User user) {
        return profileRepo.findByUser(user)
                .orElseGet(() -> profileRepo.save(new SponsorProfile(user)));
    }

    public SponsorNotificationSettings getOrCreateSettings(User user) {
        return settingsRepo.findByUser(user)
                .orElseGet(() -> settingsRepo.save(new SponsorNotificationSettings(user)));
    }

    public List<SponsorProgram> getPrograms(User sponsor) {
        return programRepo.findBySponsor(sponsor);
    }

    public List<SponsorCoupon> getActiveCoupons(User sponsor) {
        return couponRepo.findBySponsorAndActiveTrue(sponsor);
    }

    public Map<String, Object> getMetrics(User sponsor) {
        List<SponsorProgram> programs = programRepo.findBySponsor(sponsor);
        List<SponsorCoupon> coupons = couponRepo.findBySponsor(sponsor);

        long activePrograms = programs.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
        int totalUsed = coupons.stream().mapToInt(c -> c.getUsedCount() == null ? 0 : c.getUsedCount()).sum();
        int totalMax = coupons.stream().mapToInt(c -> c.getMaxUses() == null ? 0 : c.getMaxUses()).sum();
        int participation = totalMax > 0 ? (int) ((totalUsed * 100.0) / totalMax) : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("creditsUsed", sponsor.getCredits() != null ? sponsor.getCredits() : 0);
        metrics.put("creditChange", "0%");
        metrics.put("popularSkills", programs.size() * 3);
        metrics.put("skillsChange", "0%");
        metrics.put("impactReports", (int) activePrograms);
        metrics.put("participation", participation);
        metrics.put("participationChange", "0%");
        return metrics;
    }

    public void saveProfile(User user, String name, String industry, String bio, String website,
                            String location, String contactName, String contactTitle,
                            String contactEmail, String contactPhone, String linkedin,
                            Integer foundedYear, String companySize) {
        SponsorProfile profile = getOrCreateProfile(user);
        profile.setName(name);
        profile.setIndustry(industry);
        profile.setBio(bio);
        profile.setWebsite(website);
        profile.setLocation(location);
        profile.setContactName(contactName);
        profile.setContactTitle(contactTitle);
        profile.setContactEmail(contactEmail);
        profile.setContactPhone(contactPhone);
        profile.setLinkedin(linkedin);
        profile.setFoundedYear(foundedYear);
        profile.setCompanySize(companySize);
        profileRepo.save(profile);

        // sync name/location back to User so sidebar shows up-to-date info
        if (name != null && !name.isBlank()) user.setName(name);
        if (location != null && !location.isBlank()) user.setLocation(location);
        userRepository.save(user);
    }

    public void saveSettings(User user, boolean notifApplications, boolean notifCoupons,
                             boolean notifReports, boolean notifAnnouncements) {
        SponsorNotificationSettings settings = getOrCreateSettings(user);
        settings.setNotifApplications(notifApplications);
        settings.setNotifCoupons(notifCoupons);
        settings.setNotifReports(notifReports);
        settings.setNotifAnnouncements(notifAnnouncements);
        settingsRepo.save(settings);
    }

    public boolean changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    public SponsorProgram createProgram(User sponsor, String title, String description, String status) {
        SponsorProgram program = new SponsorProgram();
        program.setSponsor(sponsor);
        program.setTitle(title);
        program.setDescription(description);
        program.setStatus(status != null ? status : "ACTIVE");
        program.setPaymentStatus("PENDING");
        return programRepo.save(program);
    }

    public Optional<SponsorProgram> findProgramById(Long id) {
        return programRepo.findById(id);
    }

    public SponsorProgram saveProgram(SponsorProgram program) {
        return programRepo.save(program);
    }

    public SponsorCoupon createCoupon(User sponsor, String code, Integer discount,
                                      String expiryDate, Integer maxUses) {
        SponsorCoupon coupon = new SponsorCoupon();
        coupon.setSponsor(sponsor);
        coupon.setCode(code.toUpperCase());
        coupon.setDiscount(discount);
        coupon.setExpiryDate(expiryDate);
        coupon.setMaxUses(maxUses);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        return couponRepo.save(coupon);
    }

    public List<User> getTalentPool() {
        return userRepository.findAll().stream()
                .filter(u -> enums.Role.LEARNER.equals(u.getRole()))
                .limit(20)
                .toList();
    }
}
