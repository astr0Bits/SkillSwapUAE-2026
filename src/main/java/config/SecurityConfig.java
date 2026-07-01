package config;

import security.AuthEntryPointJwt;
import security.AuthTokenFilter;
import security.RateLimitFilter;
import security.jwt.JwtUtils;
import service.MyUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final MyUserDetailsService userDetailsService;
	private final AuthEntryPointJwt unauthorizedHandler;
	private final JwtUtils jwtUtils;
	private final RateLimitConfig rateLimitConfig;

	public SecurityConfig(MyUserDetailsService userDetailsService,
			AuthEntryPointJwt unauthorizedHandler,
			JwtUtils jwtUtils,
			RateLimitConfig rateLimitConfig) {
		this.userDetailsService = userDetailsService;
		this.unauthorizedHandler = unauthorizedHandler;
		this.jwtUtils = jwtUtils;
		this.rateLimitConfig = rateLimitConfig;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public AuthTokenFilter authenticationJwtTokenFilter() {
		return new AuthTokenFilter(jwtUtils, userDetailsService);
	}

	private RateLimitFilter rateLimitFilter() {
		return new RateLimitFilter(rateLimitConfig);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedHandler))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
							    // All static HTML pages — auth enforced client-side via JWT in localStorage
							    "/*.html", "/", "/error", "/favicon.ico",
							    // Static assets — /*.css covers root-level CSS files like /sponsor.css
							    // (browsers don't send Authorization headers on <link>/<script> requests,
							    // so any CSS/JS served from a non-permitAll path renders unauthenticated pages broken)
							    "/js/**", "/css/**", "/*.css", "/images/**", "/static/**", "/webjars/**",
							    "/uploads/**", "/default-logo.png","/sponsor-coupons",
							    "/sponsor-dashboard",
							    "/sponsor-profile",
							    "/sponsor-reports",
							    "/sponsor-sponsorships",
							    "/sponsor-talent",
							    // Public API endpoints
							    "/api/auth/**",
							    "/api/reviews/**",
							    "/api/categories/**",
							    "/api/auth/login",
							    "/api/auth/forgot-password",
							    "/api/verify-otp",
							    "/api/request-otp",
							    "/api/seo/**",
							    "/api/skills/**",
							    "/api/stripe/webhook"
							).permitAll().requestMatchers("/api/users/delete/**").authenticated()
						.requestMatchers("/api/browse/**").authenticated()
						.requestMatchers("/api/admin/**").hasAuthority("ADMIN")
						.anyRequest().authenticated()
						)
				// Rate limiting runs first — abusive requests rejected before JWT/DB work
				.addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("https://localhost:*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}