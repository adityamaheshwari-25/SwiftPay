package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.example.demo.entity.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	private final String SECRET = "SwiftPaySecretKeyForAdityaMaheshwariOnlyAndOnly";
	private final long EXPIRATION = 1000 * 60 * 60 * 10; // 10hrs
	
	private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
	
	public String generateToken(AppUser user) {
		
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getId());
		claims.put("role", user.getRole().name());
		claims.put("name", user.getName());
		
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(user.getEmail())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}
	
	public String extractEmail(String token) {
		return extractAllClaims(token).getSubject();
	}
	
	public Long extractUserId(String token) {
		return extractAllClaims(token).get("userId", Long.class);
	}
	
	public String extractRole(String token) {
		return extractAllClaims(token).get("role", String.class);
	}
	
	public String extractName(String token) {
		return extractAllClaims(token).get("name", String.class);
	}
	
	public boolean isExpired(String token) {
		Date expiration = extractAllClaims(token).getExpiration();
		return expiration.before(new Date());
	}
	
	public boolean validateToken(String token, String email) {
		return extractEmail(token).equals(email) && !isExpired(token);
	}
	
	public Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}
