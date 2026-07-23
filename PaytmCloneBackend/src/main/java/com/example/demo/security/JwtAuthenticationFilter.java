package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JwtAuthenticationFilter is OncePerRequestFilter, so basically every request goes through it once. 
 * 
 * SecurityContextHolder:
 * SecurityContextHolder is where Spring Security stores the current request’s authentication information.
 * Spring Security’s memory of who the current user is for this request.
 * It holds a SecurityContext, which holds an Authentication.
 * 
 * Authorities are Spring’s permissions model (ROLE_ADMIN etc). OPTIONS requests are CORS preflight checks, 
 * so we allow them without auth. UsernamePasswordAuthenticationFilter handles username/password login, but 
 * in JWT we authenticate using a custom filter before it. The JWT filter runs once per request, extracts and
 * validates the token, loads user details from DB, then sets an Authentication object in 
 * SecurityContextHolder—this is what makes Spring treat the request as authenticated and enables role-based 
 * authorization.
 * 
 * filterChain.doFilter() forwards the request to the next filter in the chain. If it is not called, the request processing stops and never reaches the controller.
 * 
 * @AuthenticationPrincipal allows direct injection of the current authenticated principal into controller methods. It retrieves the principal from SecurityContextHolder automatically.
 * doFilter() = pass request to next filter
 * JWT filter runs before default one to populate SecurityContext early.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{
	
	private final JwtUtil jwtUtil;
	private final AppUserDetailsService userDetailsService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		
		String path = request.getServletPath();
		
		// skip public endpoints.
		/**
		 * 
		 * That means:
			Skip JWT validation
			Continue normally
		 */
		if (path.equals("/api/v1/users/register")
				|| path.equals("/api/v1/merchants/register")
				|| path.equals("/api/v1/auth/login")) {
			filterChain.doFilter(request, response);
			return;
		}
			
		
		String header = request.getHeader("Authorization");
		
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String token = header.substring(7);
		String email = null;

		try {
		    email = jwtUtil.extractEmail(token);
		} catch (Exception e) {
		    filterChain.doFilter(request, response);
		    return;
		}
		
		// Check if token is valid and no one
		
		/**
		 *new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
		 *This is the Spring Security object representing:
		 *principal = userDetails (who the user is)
		 *credentials = null (because we’re not using password in JWT flow)
		 *authorities = roles/permissions for authorization
		 *Important: Despite the name “UsernamePassword…”, it’s commonly used even in JWT systems as a general Authentication token object.
		 *
		 * setDetails(...)
			Adds request-specific metadata:
			remote address
			session id (if any)
			etc.
			Not mandatory for JWT auth, but useful for auditing/logging.
		 */
		if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			
			/**
			 *Even though JWT contains claims, we still load the user from the database to ensure the 
			 *account is still active and roles are up to date. This prevents privilege escalation or access 
			 *after deactivation. 
			 */
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);
			
			if (jwtUtil.validateToken(token, email)) {
				
				/**
				 *Authentication is the complete security object stored in SecurityContext, containing 
				 *principal(useDetails here), credentials(null in case of using JWT), and authorities(Roles). 
				 *Principal is just the identity of the user, usually an implementation of UserDetails(here, AppUserDetails). 
				 */
				
				UsernamePasswordAuthenticationToken authToken = 
						new UsernamePasswordAuthenticationToken(
									userDetails,
									null,
									userDetails.getAuthorities()
								);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				
				// set the auth token.
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}
		
		filterChain.doFilter(request, response);
		
	}
	
	
}
