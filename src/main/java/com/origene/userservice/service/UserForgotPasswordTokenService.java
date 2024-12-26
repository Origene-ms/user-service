package com.origene.userservice.service;

import com.origene.userservice.model.UserForgotPasswordToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserForgotPasswordTokenService {

    private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

    public UserForgotPasswordTokenService(UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
        this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
    }

    public Mono<UserForgotPasswordToken> createToken(UserForgotPasswordToken token) {
        return userForgotPasswordTokenRepository.save(token);
    }

    public Mono<UserForgotPasswordToken> getTokenByUserIdAndActiveStatus(String userId, String activeStatus) {
        return userForgotPasswordTokenRepository.findByUserIdAndActiveStatus(userId, activeStatus);
    }
}