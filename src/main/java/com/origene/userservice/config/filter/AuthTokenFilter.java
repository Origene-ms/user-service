package com.origene.userservice.config.filter;

import com.origene.userservice.config.security.CustomSecurityContext;
import com.origene.userservice.config.security.CustomUserDetailsService;
import com.origene.userservice.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AuthTokenFilter implements ServerSecurityContextRepository, WebFilter {

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
            return userDetailsService.findByUsername(username)
                    .map(userDetails -> {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        return new SecurityContextImpl(auth);
                    });
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = jwtUtil.extractJwtFromRequest(exchange.getRequest());
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);

            return userDetailsService.findByUsername(username)
                    .flatMap(userDetails -> {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        SecurityContext context = new CustomSecurityContext(authToken, token);
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));

                    });
        }
        return chain.filter(exchange);
    }


}
