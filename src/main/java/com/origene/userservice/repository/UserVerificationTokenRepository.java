package com.origene.userservice.repository;

import com.origene.userservice.model.UserVerificationToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserVerificationTokenRepository extends ReactiveMongoRepository<UserVerificationToken, String> {
    Mono<UserVerificationToken> findByToken(String token);
}