package com.origene.userservice.config.filter;

import com.origene.userservice.config.security.CustomSecurityContext;
import com.origene.userservice.config.security.CustomUserDetailsService;
import com.origene.userservice.exception.UnauthorizedRequestException;
import com.origene.userservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AuthTokenFilter implements ServerSecurityContextRepository, WebFilter {

  private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;

  public AuthTokenFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
    this.jwtUtil = jwtUtil;
    this.userDetailsService = userDetailsService;
  }

  @Override
  public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Mono<SecurityContext> load(ServerWebExchange exchange) {
    String token = jwtUtil.extractJwtFromRequest(exchange.getRequest());
    if (token != null && jwtUtil.validateToken(token)) {
      String username = jwtUtil.getUsernameFromToken(token);
      logger.info("Loading security context for user: {}", username);
      return userDetailsService.findByUsername(username)
              .map(userDetails -> {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                return (SecurityContext) new SecurityContextImpl(auth);
              })
              .doOnError(e -> logger.error("Error loading security context for user: {}", username, e));
    }
    logger.warn("Invalid or missing JWT token");
    return Mono.empty();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String token = jwtUtil.extractJwtFromRequest(exchange.getRequest());

    if (token == null || !jwtUtil.validateToken(token)) {
      logger.warn("Invalid or missing JWT token;");
      throw new UnauthorizedRequestException("Unauthorized request missing token"); // Proceed without authentication
    }

    String userId = jwtUtil.getUsernameFromToken(token);
    logger.info("Authenticating user");

    return userDetailsService.findByUsername(userId)
            .flatMap(userDetails -> {
              if (userDetails == null) {
                logger.warn("User not found for ID: {}", userId);
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
              }

              // Set up authentication token and security context
              UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                      userDetails, null, userDetails.getAuthorities());
              SecurityContext context = new CustomSecurityContext(authToken, token);

              return chain.filter(exchange)
                      .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))
                      .doOnSuccess(aVoid -> logger.info("Successfully authenticated user"))
                      .doOnError(e -> logger.error("Error during authentication", e));
            })
            .onErrorResume(e -> {
              logger.error("Authentication error", e);
              return chain.filter(exchange);
            });
  }

}
