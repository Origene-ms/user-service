package com.origene.userservice.service.impl;

import com.origene.userservice.model.UserVerificationToken;
import com.origene.userservice.repository.UserVerificationTokenRepository;
import com.origene.userservice.service.UserVerificationTokenService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserVerificationTokenServiceImpl implements UserVerificationTokenService {

  private final UserVerificationTokenRepository userVerificationTokenRepository;

  public UserVerificationTokenServiceImpl(UserVerificationTokenRepository userVerificationTokenRepository) {
    this.userVerificationTokenRepository = userVerificationTokenRepository;
  }

  @Override
  public Mono<UserVerificationToken> createToken(UserVerificationToken token) {
    return userVerificationTokenRepository.save(token);
  }

  @Override
  public Mono<UserVerificationToken> getToken(String token) {
    return userVerificationTokenRepository.findByToken(token);
  }
}