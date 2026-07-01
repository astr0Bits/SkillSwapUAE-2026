package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/index").setViewName("index");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/register").setViewName("register");
        registry.addViewController("/forgot-password").setViewName("forgot-password");
        registry.addViewController("/verify-otp").setViewName("verify-otp");
        // /dashboard, /sessions, /userProfile, /browse-skills, /ai-matching,
        // /session-details, /chat are handled by PageViewController (supply model data)
    }
}
