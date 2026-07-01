package validator;
 
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
 
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.equals(InputSanitizer.sanitize(value));
    }
}