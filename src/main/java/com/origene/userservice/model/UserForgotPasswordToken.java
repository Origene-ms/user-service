package com.origene.userservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "userForgotPasswordTokens")
public class UserForgotPasswordToken {
  @Id
  private String id;

  @DBRef
  private User userId;

  private String token;
  private String activeStatus;
  private Date createdAt;

  public UserForgotPasswordToken(User user, String token, String activeStatus) {
    this.userId = user;
    this.token = token;
    this.activeStatus = activeStatus;
    this.createdAt = new Date();
  }
}