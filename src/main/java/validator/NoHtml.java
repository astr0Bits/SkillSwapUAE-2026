package validator;
 
import jakarta.validation.Constraint;

import jakarta.validation.Payload;
 
import java.lang.annotation.*;
 
@Documented

@Constraint(validatedBy = NoHtmlValidator.class)

@Target({ElementType.METHOD, ElementType.FIELD})

@Retention(RetentionPolicy.RUNTIME)

public @interface NoHtml {

    String message() default "Invalid input: HTML not allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

 