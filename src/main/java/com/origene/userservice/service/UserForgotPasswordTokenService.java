package com.origene.userservice.service;

import com.origene.userservice.model.UserForgotPasswordToken;
import reactor.core.publisher.Mono;

public interface UserForgotPasswordTokenService {
  Mono<UserForgotPasswordToken> createToken(UserForgotPasswordToken token);

  Mono<UserForgotPasswordToken> getTokenByUserIdAndActiveStatus(String userId, String activeStatus);
}
