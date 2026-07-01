package model;
/*A DTO used in the OTP verification flow 
 * (e.g., during password reset or MFA). 
 * The frontend sends an email and OTP to 
 * the backend for validation.
*/
public class OtpRequest {
    private String email;
    private String otp;

    // Constructors
    public OtpRequest() {
    }

    public OtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    //toString() for debugging/logging.
    @Override
    public String toString() {
        return "OtpRequest{" +
                "email='" + email + '\'' +
                ", otp='" + otp + '\'' +
                '}';
    }
}
