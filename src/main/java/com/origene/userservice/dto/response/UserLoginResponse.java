package com.origene.userservice.dto.response;

import com.origene.userservice.dto.request.UserDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UserLoginResponse extends BaseResponse {
  private String accessToken;
  private String refreshToken;
  private String adminToken;
  private UserDTO user;

  public UserLoginResponse(int status, String message) {
    super(status, message);
  }

}