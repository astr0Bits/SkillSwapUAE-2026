package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GeminiStartupChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeminiStartupChecker.class);

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.warn("GROQ_API_KEY is not set — AI matching, summaries, and assessments will use fallback logic only.");
        } else {
            log.info("Groq API key loaded — AI features are active.");
        }
    }
}
