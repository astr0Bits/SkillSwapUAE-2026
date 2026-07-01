package controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import model.ProcessedStripeEvent;
import model.SponsorProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.ProcessedStripeEventRepository;
import repository.SponsorProgramRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final SponsorProgramRepository programRepo;
    private final ProcessedStripeEventRepository processedEventRepo;

    public StripeWebhookController(SponsorProgramRepository programRepo,
                                   ProcessedStripeEventRepository processedEventRepo) {
        this.programRepo = programRepo;
        this.processedEventRepo = processedEventRepo;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if (processedEventRepo.existsById(event.getId())) {
            log.info("Duplicate Stripe event {}, skipping", event.getId());
            return ResponseEntity.ok("Already processed");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Optional<StripeObject> stripeObject = deserializer.getObject();
            if (stripeObject.isPresent() && stripeObject.get() instanceof Session session) {
                String programIdStr = session.getMetadata().get("programId");
                if (programIdStr != null) {
                    try {
                        Long programId = Long.parseLong(programIdStr);
                        programRepo.findById(programId).ifPresent(program -> {
                            program.setPaymentStatus("PAID");
                            programRepo.save(program);
                            log.info("Marked SponsorProgram {} as PAID via Stripe event {}", programId, event.getId());
                        });
                    } catch (NumberFormatException e) {
                        log.error("Invalid programId in Stripe metadata: {}", programIdStr);
                    }
                }
            } else {
                log.warn("Could not deserialize checkout.session.completed payload for event {}", event.getId());
            }
        } else {
            log.info("Unhandled Stripe event type: {}", event.getType());
        }

        processedEventRepo.save(new ProcessedStripeEvent(event.getId(), LocalDateTime.now()));
        return ResponseEntity.ok("OK");
    }
}
