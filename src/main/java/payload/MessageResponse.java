package payload;

/*Standard response format for success or error messages 
 * (e.g., after registration, password reset). Used to return a 
 * plain text message in a JSON wrapper.
*/
public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}