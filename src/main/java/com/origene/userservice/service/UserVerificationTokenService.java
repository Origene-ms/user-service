package com.origene.userservice.service;

import com.origene.userservice.model.UserVerificationToken;
import reactor.core.publisher.Mono;

public interface UserVerificationTokenService {
  Mono<UserVerificationToken> createToken(UserVerificationToken token);

  Mono<UserVerificationToken> getToken(String token);

}
