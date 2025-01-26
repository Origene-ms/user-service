package com.origene.userservice.dto.request;

import lombok.Data;

@Data
public class UpdatePasswordDTO {
  private String email;
  private String currentPassword;
  private String updatedPassword;
}
