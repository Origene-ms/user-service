package com.origene.userservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "userVerificationTokens")
public class UserVerificationToken {
  @Id
  private String id;

  @DBRef
  private final User user;

  private String token;
  private LocalDateTime createdAt;
  private LocalDateTime expireAt;

  public UserVerificationToken(User user, String token) {
    this.user = user;
    this.token = token;
    this.createdAt = LocalDateTime.now();
    this.expireAt = LocalDateTime.now().plusMinutes(15);
  }
}