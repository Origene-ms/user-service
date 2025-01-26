package com.origene.userservice.service.impl;

import com.origene.userservice.model.UserRefreshToken;
import com.origene.userservice.repository.UserRefreshTokenService;
import reactor.core.publisher.Mono;

public class UserRefreshTokenServiceImpl implements com.origene.userservice.service.UserRefreshTokenService {
  private final UserRefreshTokenService userRefreshTokenService;

  public UserRefreshTokenServiceImpl(UserRefreshTokenService userRefreshTokenService) {
    this.userRefreshTokenService = userRefreshTokenService;
  }

  @Override
  public Mono<Void> deleteByUserId(String userId) {
    return userRefreshTokenService.deleteByUserId(userId);
  }

  @Override
  public Mono<UserRefreshToken> findByUserIdAndToken(String userId, String token) {
    return userRefreshTokenService.findByUserIdAndToken(userId, token);
  }
}
