package com.origene.userservice.repository;

import com.origene.userservice.model.UserRefreshToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRefreshTokenRepository extends ReactiveMongoRepository<UserRefreshToken, String> {
    Mono<Void> deleteByUserId(String userId);
    Mono<UserRefreshToken> findByUserIdAndToken(String userId, String token);
}
