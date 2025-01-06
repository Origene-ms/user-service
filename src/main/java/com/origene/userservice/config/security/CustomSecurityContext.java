package com.origene.userservice.config.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;

import java.io.Serializable;

@Getter
public class CustomSecurityContext extends SecurityContextImpl implements Serializable {
    private transient String token; // Exclude from serialization

    public CustomSecurityContext(Authentication authentication, String token) {
        super(authentication);
        this.token = token;
    }

}
