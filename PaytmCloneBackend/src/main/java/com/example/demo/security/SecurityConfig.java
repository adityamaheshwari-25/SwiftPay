package com.example.demo.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.config.CorsProperties;

import lombok.RequiredArgsConstructor;

/**
 *Role based authorization is defined in this.
 *
 *The application is fully stateless. Authentication state is not stored in the server session. 
 *Instead, each request carries a JWT token which is validated independently.
 *
 *Spring Security 6 configuration uses SecurityFilterChain instead of WebSecurityConfigurerAdapter.
 *
 *Options:
 *Browsers send an OPTIONS request automatically before some API calls (especially cross-origin calls). This is called a CORS preflight request.
 *like when frontend calls the backend, it first checks whether i can send GET/POST request with the 
 *Authorization header or not, its checks can I send those requests, so that question or check is what's the work
 *of the options.
 *We allow it because its just a permission check not any business use case.  
 *
 *Security Filter Chain: A filter chain is a sequence of filters that process every HTTP request before it reaches your controller.
 *In Spring Security, there is a special filter chain called SecurityFilterChain.
 *The SecurityFilterChain is an ordered chain of filters that process every incoming HTTP request. Each filter performs a specific security responsibility such as authentication, CSRF validation, or authorization before the request reaches the controller.
 * 
 * csrf highly matters for the session-based systems because session auth relies on cookies automatically sent by browser.
 * 
 * JWT is usually stored in localStorage or sent manually in Authorization header.
	Browser does NOT automatically attach JWT unlike in the case of cookies.
	So CSRF risk is reduced. 
	
	CSRF is an attack where a malicious site tricks a browser into sending authenticated requests using stored cookies. In stateless JWT-based systems, we disable CSRF because authentication is not cookie-based but token-based.
 * CSRF protects cookie-based sessions, not needed for stateless JWT.
 * 
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtAuthenticationFilter jwtFilter;
	private final CustomAuthEntryPoint authEntryPoint;
	private final AppUserDetailsService userDetailsService;
	private final CustomAccessDeniedHandler accessDeniedHandler;
	private final CorsProperties corsProperties;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint) // Handles 401
                .accessDeniedHandler(accessDeniedHandler)  // Handles 403
            )
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Every request must include JWT, no http session, no JSESSIONID
        .authorizeHttpRequests(auth -> auth
//        	.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight thing.
//            .requestMatchers(
//            		"/api/v1/users/register",
//                    "/api/v1/merchants/register",
//                    "/api/v1/auth/login"
//            		).permitAll()
//            .anyRequest().authenticated()
        		
                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public liveness/readiness check; all other Actuator endpoints remain disabled.
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                // Public
                .requestMatchers(
                    "/api/v1/users/register",
                    "/api/v1/merchants/register",
                    "/api/v1/auth/login",
                    "/api/v1/events/stream"
                ).permitAll()

                // 🔐 ADMIN ONLY
                .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")

                // 👤 USER / MERCHANT
                .requestMatchers(
                    "/api/v1/kyc/**",
                    "/api/v1/wallet/**",
                    "/api/v1/bank-accounts/**",
                    "/api/v1/transactions/**"
                ).hasAnyRole("USER", "MERCHANT")
                
                .requestMatchers("/api/v1/splits/**").hasRole("USER") // spring internally checks for ROLE_ADMIN, that's why we prefix it in AppUserDetails.

                
                
                .requestMatchers("/api/v1/merchant/**").hasRole("MERCHANT")

                // everything else
                .anyRequest().authenticated()
        );
		// later add the rbac part.

		/*
		 * In classic spring security, the authentication happens via username and password, typically a 
		 * login request form.
		 * But in the jwt systems, you don't send username and password on every request, you send the jwt
		 * token for authentication, but the default one still exists in the chain.
		 * So we authenticate the request via jwt filter first and then the default one as can be seen in this 
		 * code.
		 * 
		 * UsernamePasswordAuthenticationFilter is responsible for form-based login authentication. In a JWT-based system, we place our JWT filter before it so requests get authenticated via token first.
		 * 
		 * We insert the JWT filter before UsernamePasswordAuthenticationFilter so that token-based authentication happens before Spring’s default form-login mechanism. This ensures the SecurityContext is populated early for authorization checks.
		 * If we don't do that before then it is possible that UsernamePasswordAuthenticationFilter sets no authenticated user and hence rejects it.
		 **/
	    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
	
	    return http.build();
	}
	
	
	// as we are using spring security 6.x we use this.
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) {
		AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
		
		builder
			.userDetailsService(userDetailsService)
			.passwordEncoder(passwordEncoder());
		
		return builder.build();
	}
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		
		config.setAllowedOrigins(corsProperties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                "Idempotency-Key",
                "Last-Event-ID"
        ));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", config);
        
        return src;
	}
	
	
	
	
}
