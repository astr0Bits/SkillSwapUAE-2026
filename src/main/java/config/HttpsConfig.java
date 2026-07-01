package config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
 
@Configuration
@Profile("prod")
public class HttpsConfig {
 
    @Bean
    //Defines a bean that customizes the Tomcat server. It adds an additional connector (HTTP) to the server, 
    //so it listens on both HTTP (port 8085) and HTTPS (port 8443). The HTTP connector will redirect to HTTPS.
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return server -> server.addAdditionalTomcatConnectors(httpConnector());
    }
    //Creates a connector for HTTP on port 8085. When a request comes to this port, 
    //it is redirected to port 8443 (HTTPS) because setRedirectPort(8443) is set. 
    //This ensures all traffic is served over HTTPS for security.
    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8085); // Regular HTTP
        connector.setSecure(false);
        connector.setRedirectPort(8443); // Redirect to HTTPS
        return connector;
    }
}