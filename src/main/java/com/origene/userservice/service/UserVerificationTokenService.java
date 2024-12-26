package com.origene.userservice.service;

import com.origene.userservice.model.UserVerificationToken;
import com.origene.userservice.repository.UserVerificationTokenRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserVerificationTokenService {

    private final UserVerificationTokenRepository userVerificationTokenRepository;

    public UserVerificationTokenService(UserVerificationTokenRepository userVerificationTokenRepository) {
        this.userVerificationTokenRepository = userVerificationTokenRepository;
    }

    public Mono<UserVerificationToken> createToken(UserVerificationToken token) {
        return userVerificationTokenRepository.save(token);
    }

    public Mono<UserVerificationToken> getToken(String token) {
        return userVerificationTokenRepository.findByToken(token);
    }
}