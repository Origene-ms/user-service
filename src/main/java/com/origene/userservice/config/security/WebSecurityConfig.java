package com.origene.userservice.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final ReactiveUserDetailsService userDetailsService;
    private final com.origene.userservice.config.security.AuthEntryPointJwt unauthorizedHandler;
    private final com.origene.userservice.config.security.AuthTokenFilter authTokenFilter;

    public WebSecurityConfig(ReactiveUserDetailsService userDetailsService, com.origene.userservice.config.security.AuthEntryPointJwt unauthorizedHandler, com.origene.userservice.config.security.AuthTokenFilter authTokenFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.authTokenFilter = authTokenFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/users/login").permitAll()
                        .pathMatchers("/api/users/signup").permitAll()
                        .pathMatchers("/actuator/health").permitAll() // Allow public access to health endpoint
                        .anyExchange().authenticated()
                )
                .addFilterAt(authTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION) // Corrected here
                .build();
    }


    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager manager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder());
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
