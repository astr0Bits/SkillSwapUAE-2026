//package service;
///*Provides a simple OTP storage and verification mechanism, typically used 
// * for password reset or email verification flows.
//*/
//import org.springframework.stereotype.Service;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class OtpService {
//
//	//An in‑memory store (HashMap) that maps email to OTP. Warning: 
//	//This is not persistent; OTPs are lost on server restart. 
//	//For production, a cache with TTL or database storage should be used.
//    private final Map<String, String> otpStore = new HashMap<>();
//
//    //Stores an OTP for a given email. Overwrites any previous OTP.
//    public void saveOtp(String email, String otp) {
//        otpStore.put(email, otp);
//    }
//    
//    //Verifies that the provided OTP matches the stored one for that email.
//    public boolean verifyOtp(String email, String otp) {
//        return otp.equals(otpStore.get(email));
//    }
//}

package service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final long OTP_VALIDITY_MINUTES = 10;
    
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public void saveOtp(String email, String otp) {
        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES)));
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(email);
            return false;
        }
        boolean valid = entry.otp.equals(otp);
        if (valid) {
            otpStore.remove(email); // one-time use
        }
        return valid;
    }
    
    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;
        OtpEntry(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}