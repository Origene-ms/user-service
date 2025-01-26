package com.origene.userservice.dto.request;

import lombok.Data;

@Data
public class ForgotTokenDTO {
  private String email;
  private String token;
  private String password;
}
