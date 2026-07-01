package validator;
 
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
 
public class InputSanitizer {
    private static final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
 
    public static String sanitize(String input) {
        return policy.sanitize(input);
    }
}