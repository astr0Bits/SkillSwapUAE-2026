package config;

import org.springframework.context.annotation.Configuration;

import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
 
import org.springframework.context.annotation.Bean;
//nables validation of method parameters and return values using JSR‑303 annotations 
//(like @Valid, @NotNull, etc.) on Spring beans. 
//It intercepts method calls and applies validation constraints.
@Configuration
public class InputValidationConfig {

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();

    }

}

 