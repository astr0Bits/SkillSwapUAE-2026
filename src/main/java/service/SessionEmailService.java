package service;

import model.Session;
import model.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import enums.SessionMode;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class SessionEmailService {

    private static final String FROM = "dalal1087226@gmail.com";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' HH:mm");

    private final JavaMailSender mailSender;

    public SessionEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Learner confirmation ──────────────────────────────────────────────────

    public void sendBookingConfirmation(User learner, User mentor,
                                        Session session, String skillName) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(FROM);
        helper.setTo(learner.getEmail());
        helper.setSubject("Session Booking Confirmed — " + skillName);
        helper.setText(buildLearnerBody(learner, mentor, session, skillName), true);
        mailSender.send(msg);
    }

    // ── Mentor notification ───────────────────────────────────────────────────

    public void sendMentorNotification(User mentor, User learner,
                                       Session session, String skillName) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(FROM);
        helper.setTo(mentor.getEmail());
        helper.setSubject("New Learner Joined Your Session — " + skillName);
        helper.setText(buildMentorBody(mentor, learner, session, skillName), true);
        mailSender.send(msg);
    }

    // ── Email bodies ──────────────────────────────────────────────────────────

    private String buildLearnerBody(User learner, User mentor,
                                    Session session, String skillName) {
        String scheduledTime = session.getScheduledTime() != null
                ? session.getScheduledTime().format(DATE_FMT) : "To be confirmed";
        int duration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 60;
        boolean isOnline = SessionMode.ONLINE.equals(session.getMode());

        String modeBlock;
        if (isOnline) {
            String link = session.getMeetingLink();
            String lc = link != null ? link.toLowerCase() : "";
            String platform = lc.contains("zoom.us") ? "Zoom"
                    : lc.contains("teams.microsoft.com") ? "Microsoft Teams"
                    : lc.contains("meet.google.com") ? "Google Meet"
                    : "Online Meeting";
            String btnColor = lc.contains("zoom.us") ? "#0b5cff"
                    : lc.contains("teams.microsoft.com") ? "#464eb8"
                    : "#4f46e5";
            String linkHtml = link != null && !link.isBlank()
                ? """
                  <p style="color:#475569;font-size:14px;margin:0 0 12px;">
                    Join your session using the link below:
                  </p>
                  <p style="margin:0 0 8px;">
                    <a href="%s"
                       style="display:inline-block;background:%s;color:white;padding:11px 22px;
                              border-radius:8px;text-decoration:none;font-weight:600;font-size:14px;">
                      Join on %s
                    </a>
                  </p>
                  <p style="margin:0;font-size:11px;color:#94a3b8;word-break:break-all;">%s</p>
                  """.formatted(link, btnColor, platform, link)
                : "<p style=\"color:#94a3b8;font-size:13px;\">Your mentor will share the meeting link shortly.</p>";

            modeBlock = """
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Mode</td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <span style="background:#eef2ff;color:#4f46e5;padding:2px 10px;
                                 border-radius:20px;font-size:12px;font-weight:600;">Online</span>
                  </td>
                </tr>
                <tr>
                  <td colspan="2" style="padding:16px 0 8px;">%s</td>
                </tr>
                """.formatted(linkHtml);
        } else {
            String loc = session.getPhysicalLocation() != null && !session.getPhysicalLocation().isBlank()
                    ? session.getPhysicalLocation()
                    : (session.getLocation() != null && !session.getLocation().isBlank()
                    ? session.getLocation() : "Location to be confirmed by your mentor");

            modeBlock = """
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Mode</td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <span style="background:#fef3c7;color:#92400e;padding:2px 10px;
                                 border-radius:20px;font-size:12px;font-weight:600;">In-Person</span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Location</td>
                  <td style="padding:10px 0;border-bottom:1px solid #eee;">
                    <p style="margin:0;">Your session will be held at:</p>
                    <p style="margin:6px 0 0;font-weight:700;color:#0f172a;">%s</p>
                  </td>
                </tr>
                """.formatted(loc);
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:'DM Sans',Arial,sans-serif;background:#f5f7fc;">
              <div style="max-width:600px;margin:32px auto;background:white;border-radius:16px;
                          overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <div style="background:linear-gradient(135deg,#4f46e5,#818cf8);padding:32px 36px;">
                  <h1 style="margin:0;color:white;font-size:22px;">📚 SkillSwap</h1>
                  <p style="margin:8px 0 0;color:#c7d2fe;font-size:14px;">Session Booking Confirmed</p>
                </div>
                <div style="padding:28px 36px 0;">
                  <p style="color:#0f172a;font-size:16px;margin:0 0 4px;">Hi <strong>%s</strong>,</p>
                  <p style="color:#64748b;font-size:14px;margin:0 0 22px;">Your session has been successfully booked!</p>
                </div>
                <div style="padding:0 36px 20px;">
                  <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Skill</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Mentor</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Date &amp; Time</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Duration</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%d minutes</td>
                    </tr>
                    %s
                  </table>
                </div>
                <div style="margin:0 36px 28px;padding:14px 16px;background:#fefce8;
                            border-radius:8px;border-left:4px solid #f59e0b;">
                  <p style="margin:0;font-size:13px;color:#92400e;">
                    If you need to cancel, please do so at least 24 hours before the session.
                  </p>
                </div>
                <div style="padding:16px 36px;background:#f8faff;border-top:1px solid #e2e8f0;text-align:center;">
                  <p style="margin:0;color:#94a3b8;font-size:12px;">
                    SkillSwap &middot; Session #%s
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                learner.getName(), skillName, mentor.getName(),
                scheduledTime, duration, modeBlock, session.getId()
        );
    }

    private String buildMentorBody(User mentor, User learner,
                                   Session session, String skillName) {
        String scheduledTime = session.getScheduledTime() != null
                ? session.getScheduledTime().format(DATE_FMT) : "To be confirmed";
        boolean isOnline = SessionMode.ONLINE.equals(session.getMode());

        String connectionBlock;
        if (isOnline) {
            String link = session.getMeetingLink();
            connectionBlock = link != null && !link.isBlank()
                ? """
                  <tr>
                    <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Your Link</td>
                    <td style="padding:10px 0;border-bottom:1px solid #eee;">
                      <a href="%s" style="color:#4f46e5;word-break:break-all;">%s</a>
                    </td>
                  </tr>
                  """.formatted(link, link)
                : "";
        } else {
            String loc = session.getPhysicalLocation() != null && !session.getPhysicalLocation().isBlank()
                    ? session.getPhysicalLocation()
                    : (session.getLocation() != null ? session.getLocation() : "");
            connectionBlock = !loc.isBlank()
                ? """
                  <tr>
                    <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Location</td>
                    <td style="padding:10px 0;border-bottom:1px solid #eee;font-weight:600;">%s</td>
                  </tr>
                  """.formatted(loc)
                : "";
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:'DM Sans',Arial,sans-serif;background:#f5f7fc;">
              <div style="max-width:600px;margin:32px auto;background:white;border-radius:16px;
                          overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <div style="background:linear-gradient(135deg,#10b981,#34d399);padding:32px 36px;">
                  <h1 style="margin:0;color:white;font-size:22px;">📚 SkillSwap</h1>
                  <p style="margin:8px 0 0;color:#d1fae5;font-size:14px;">A learner has joined your session</p>
                </div>
                <div style="padding:28px 36px 0;">
                  <p style="color:#0f172a;font-size:16px;margin:0 0 4px;">Hi <strong>%s</strong>,</p>
                  <p style="color:#64748b;font-size:14px;margin:0 0 22px;">
                    <strong>%s</strong> has just booked a spot in your session.
                  </p>
                </div>
                <div style="padding:0 36px 24px;">
                  <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;width:38%%">Learner</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Learner Email</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Skill</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Date &amp; Time</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;color:#64748b;">Mode</td>
                      <td style="padding:10px 0;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    %s
                  </table>
                </div>
                <div style="padding:16px 36px;background:#f8faff;border-top:1px solid #e2e8f0;text-align:center;">
                  <p style="margin:0;color:#94a3b8;font-size:12px;">SkillSwap &middot; Session #%s</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                mentor.getName(),
                learner.getName(),
                learner.getName(), learner.getEmail(),
                skillName, scheduledTime,
                isOnline ? "Online" : "In-Person",
                connectionBlock, session.getId()
        );
    }
}
