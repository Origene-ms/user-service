package com.origene.userservice.config.security;

import com.origene.userservice.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationManager implements AuthenticationManager {

  private final JwtUtil jwtUtil;

  public JwtAuthenticationManager(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String token = authentication.getCredentials().toString();

    if (jwtUtil.validateToken(token)) {
      String username = jwtUtil.getUsernameFromToken(token);
      return new UsernamePasswordAuthenticationToken(username, null, authentication.getAuthorities());
    } else {
      throw new BadCredentialsException("Invalid JWT token");
    }
  }
}