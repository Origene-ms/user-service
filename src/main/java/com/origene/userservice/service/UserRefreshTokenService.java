package com.origene.userservice.service;

import com.origene.userservice.model.UserRefreshToken;
import reactor.core.publisher.Mono;

public interface UserRefreshTokenService {
  Mono<Void> deleteByUserId(String userId);

  Mono<UserRefreshToken> findByUserIdAndToken(String userId, String token);
}
