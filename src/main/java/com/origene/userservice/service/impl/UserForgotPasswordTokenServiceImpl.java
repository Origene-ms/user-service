package com.origene.userservice.service.impl;

import com.origene.userservice.model.UserForgotPasswordToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserForgotPasswordTokenServiceImpl implements com.origene.userservice.service.UserForgotPasswordTokenService {

  private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

  public UserForgotPasswordTokenServiceImpl(UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
    this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
  }

  @Override
  public Mono<UserForgotPasswordToken> createToken(UserForgotPasswordToken token) {
    return userForgotPasswordTokenRepository.save(token);
  }

  public Mono<UserForgotPasswordToken> getTokenByUserIdAndActiveStatus(String userId, String activeStatus) {
    return userForgotPasswordTokenRepository.findByUserIdAndActiveStatus(userId, activeStatus);
  }
}