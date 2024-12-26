package com.origene.userservice.repository;

import com.origene.userservice.model.UserForgotPasswordToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserForgotPasswordTokenRepository extends ReactiveMongoRepository<UserForgotPasswordToken, String> {
    Mono<UserForgotPasswordToken> findByUserIdAndActiveStatus(String userId, String activeStatus);
    Mono<UserForgotPasswordToken> findByUserIdAndTokenAndActiveStatus(String userId, String token, String activeStatus);
}