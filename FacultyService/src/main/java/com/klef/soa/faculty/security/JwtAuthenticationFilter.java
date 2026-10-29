package com.klef.soa.faculty.security;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final SecretKey key;
 public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret){if(secret==null||secret.length()<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 characters and match the issuer configuration");key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String header=request.getHeader("Authorization");
  if(header!=null&&header.startsWith("Bearer "))try{var claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();String email=claims.getSubject(),role=claims.get("role",String.class);if(email!=null&&role!=null)SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_"+role.toUpperCase()))));}catch(Exception ignored){SecurityContextHolder.clearContext();}
  chain.doFilter(request,response);
 }
}
