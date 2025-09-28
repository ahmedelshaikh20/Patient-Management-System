package com.example.authservice.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
  private final Key secertKey;

  public JwtUtil(@Value("${jwt.secret}") String secert) {
    byte[] keyBytes = Base64.getDecoder().decode(secert.getBytes(StandardCharsets.UTF_8));
    this.secertKey = Keys.hmacShaKeyFor(keyBytes);
  }


  public String generateToken(String email, String role) {

    return Jwts.builder()
      .subject(email)
      .claim("role", role)
      .issuedAt(new Date())
      .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
      .signWith(secertKey)
      .compact();
  }
  public void validateToken(String token) {
    try {
      Jwts.parser().verifyWith((SecretKey) secertKey)
        .build()
        .parseSignedClaims(token);
    } catch (JwtException e ){
      throw new JwtException("Invalid JWT token");
    }
  }

}
