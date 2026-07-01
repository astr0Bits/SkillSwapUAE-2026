//package config;
///*
// * Centralizes JWT configuration (e.g., the secret) 
// * so it can be loaded from external configuration 
// * and reused in other components like JwtUtils.
// * */
//import org.springframework.boot.context.properties.ConfigurationProperties; //to bind external properties
//import org.springframework.stereotype.Component; //o make the class a Spring bean
//
//@Component //registers this class as a Spring component, so it can be autowired elsewhere.
//@ConfigurationProperties(prefix = "app.jwt") //tells Spring to map properties with the prefix app.jwt from application.properties to fields of this class.
//public class JwtProperties {
//    private String secret; // hold the JWT secret key
//
//    // Getters & Setters
//    public String getSecret() {
//        return secret;
//    }
//
//    public void setSecret(String secret) {
//        this.secret = secret;
//    }
//}


package config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private int expirationMs = 86400000; // default 24h

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(int expirationMs) {
        this.expirationMs = expirationMs;
    }
}